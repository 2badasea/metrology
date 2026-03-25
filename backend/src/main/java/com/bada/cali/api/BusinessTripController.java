package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.BusinessTripDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.BusinessTripListRow;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.BusinessTripServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController("ApiBusinessTripController")
@RequestMapping("/api/admin/businessTrip")
@Tag(name = "출장일정", description = "출장일정 관리 API")
@RequiredArgsConstructor
@Log4j2
public class BusinessTripController {

    private final BusinessTripServiceImpl businessTripService;

    /**
     * 캘린더 기간 내 출장일정 목록 조회
     * FullCalendar가 뷰 변경 시 rangeStart/rangeEnd를 파라미터로 전달
     */
    @GetMapping("/calendar")
    @Operation(summary = "캘린더 출장일정 조회", description = "지정 기간과 겹치는 출장일정 목록을 반환함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<List<BusinessTripDTO.CalendarEventRes>>> getCalendarEvents(
            @RequestParam LocalDateTime rangeStart,
            @RequestParam LocalDateTime rangeEnd
    ) {
        List<BusinessTripDTO.CalendarEventRes> data = businessTripService.getCalendarEvents(rangeStart, rangeEnd);
        return ResponseEntity.ok(new ResMessage<>(200, "조회 성공", data));
    }

    /**
     * 출장일정 상세 조회 (수정 모달 초기 데이터)
     */
    @GetMapping("/{id}")
    @Operation(summary = "출장일정 상세 조회", description = "수정 모달 초기 데이터 로드 시 호출함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "출장일정 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<BusinessTripDTO.DetailRes>> getDetail(@PathVariable Long id) {
        BusinessTripDTO.DetailRes data = businessTripService.getDetail(id);
        return ResponseEntity.ok(new ResMessage<>(200, "조회 성공", data));
    }

    /**
     * 출장일정 등록
     * - /api/admin/businessTrip 경로 → POST·PATCH는 Security 필터에서 ROLE_ADMIN 자동 적용
     */
    @PostMapping
    @Operation(summary = "출장일정 등록", description = "출장일정 및 첨부파일을 등록함")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수 항목 누락"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<Long>> create(
            @RequestPart("createReq") @Valid BusinessTripDTO.CreateReq req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long savedId = businessTripService.create(req, files, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResMessage<>(201, "등록 성공", savedId));
    }

    /**
     * 출장일정 수정
     */
    @PatchMapping("/{id}")
    @Operation(summary = "출장일정 수정", description = "출장일정 정보를 수정함. 첨부파일은 추가됨 (기존 파일 유지)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수 항목 누락"),
            @ApiResponse(responseCode = "404", description = "출장일정 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<Void>> update(
            @PathVariable Long id,
            @RequestPart("updateReq") @Valid BusinessTripDTO.UpdateReq req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        businessTripService.update(id, req, files, user);
        return ResponseEntity.ok(new ResMessage<>(200, "수정 성공", null));
    }

    /**
     * 표준장비 중복 체크
     * - 요청한 기간과 겹치는 다른 출장일정에 이미 등록된 장비 목록 반환
     * - 중복 있으면 hasConflict=true + conflictEquipments 목록 반환 → 프론트에서 그리드 제거
     */
    @PostMapping("/checkConflict")
    @Operation(summary = "표준장비 중복 체크", description = "지정 기간과 겹치는 다른 출장일정에 이미 등록된 장비 목록을 반환함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크 완료 (중복 없음 포함)"),
            @ApiResponse(responseCode = "400", description = "필수 항목 누락"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<BusinessTripDTO.ConflictCheckRes>> checkConflict(
            @RequestBody @Valid BusinessTripDTO.ConflictCheckReq req
    ) {
        BusinessTripDTO.ConflictCheckRes result = businessTripService.checkConflict(req);
        return ResponseEntity.ok(new ResMessage<>(200, "체크 완료", result));
    }

    /**
     * 출장자 선택용 직원 목록 조회
     * - 수정 모달 진입 시 selectpicker 옵션 구성에 사용
     */
    @GetMapping("/memberOptions")
    @Operation(summary = "출장자 선택 직원 목록", description = "selectpicker 옵션 구성용 사내 직원 목록을 반환함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<List<BusinessTripDTO.MemberOption>>> getMemberOptions() {
        List<BusinessTripDTO.MemberOption> data = businessTripService.getMemberOptions();
        return ResponseEntity.ok(new ResMessage<>(200, "조회 성공", data));
    }

    /**
     * 출장일정 리스트 조회 (TUI Grid 서버사이드 페이지네이션 표준 패턴)
     * - searchType: title | custAgent | custAgentAddr | reportAgent | reportAgentAddr
     * - dateStart/dateEnd: 출장시작일시(start_datetime) 기준 범위 필터 (yyyy-MM-dd 문자열)
     * - GET 요청이므로 인증된 사용자 전체 허용 (Security 필터 기준)
     */
    @GetMapping("/list")
    @Operation(summary = "출장일정 리스트 조회", description = "검색 필터 + 날짜 범위 + 페이지네이션 기반 출장일정 목록을 반환함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<BusinessTripListRow>>> getList(
            @ModelAttribute BusinessTripDTO.GetListReq req
    ) {
        TuiGridDTO.ResData<BusinessTripListRow> data = businessTripService.getList(req);
        return ResponseEntity.ok(new TuiGridDTO.Res<>(true, data));
    }

    /**
     * 출장일정 일괄 삭제
     * - business_trip: soft delete (delete_datetime / delete_member_id 세팅)
     * - standard_equipment_ref: hard delete (참조 데이터 완전 삭제)
     * - DELETE /api/** → Security 필터에서 ROLE_ADMIN 자동 적용
     */
    @DeleteMapping
    @Operation(summary = "출장일정 일괄 삭제", description = "선택한 출장일정을 soft delete 처리함. 연결된 표준장비 ref는 함께 hard delete됨")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류"),
            @ApiResponse(responseCode = "404", description = "출장일정 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ResMessage<Void>> delete(
            @RequestBody @Valid BusinessTripDTO.DeleteReq req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        businessTripService.delete(req.ids(), user);
        return ResponseEntity.ok(new ResMessage<>(200, "삭제 성공", null));
    }
}
