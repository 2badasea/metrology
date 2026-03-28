package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.AlarmType;
import com.bada.cali.dto.AlarmDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.AlarmServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "사용자 알림 조회/읽음 처리 API")
@RestController("ApiAlarmController")
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
@Log4j2
public class AlarmController {

    private final AlarmServiceImpl alarmService;

    // ─── 미읽음 카운트 ─────────────────────────────────────────────────────

    @Operation(summary = "미읽음 알림 수 조회", description = "로그인 사용자의 미읽음 알림 수를 반환. 상단 벨 아이콘 뱃지 업데이트에 사용")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping("/count")
    public ResponseEntity<ResMessage<AlarmDTO.CountRes>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = alarmService.countUnread(userDetails.getId());
        return ResponseEntity.ok(new ResMessage<>(1, null, new AlarmDTO.CountRes(count)));
    }

    // ─── 알림 목록 ─────────────────────────────────────────────────────────

    @Operation(summary = "알림 목록 조회",
            description = "로그인 사용자의 알림 목록을 최신순으로 반환 (TuiGrid 서버 페이징). " +
                          "alarmType 파라미터가 있으면 해당 유형만 필터링")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping
    public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<AlarmDTO.ListItem>>> getAlarmList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute TuiGridDTO.Request req,
            @Parameter(description = "알림 유형 필터 (없으면 전체 조회)")
            @RequestParam(required = false) AlarmType alarmType) {
        TuiGridDTO.ResData<AlarmDTO.ListItem> data = alarmService.getAlarmList(req, userDetails.getId(), alarmType);
        return ResponseEntity.ok(new TuiGridDTO.Res<>(true, data));
    }

    // ─── 읽음 처리 ─────────────────────────────────────────────────────────

    @Operation(summary = "개별 읽음 처리", description = "알림 ID를 지정하여 단건 읽음 처리")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<ResMessage<Void>> markReadById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 ID") @PathVariable Long id) {
        alarmService.markReadById(id, userDetails.getId());
        return ResponseEntity.ok(new ResMessage<>(1, null, null));
    }

    @Operation(summary = "전체 읽음 처리", description = "로그인 사용자의 미읽음 알림 전체를 읽음 처리")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PatchMapping("/readAll")
    public ResponseEntity<ResMessage<Void>> markReadAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alarmService.markReadAll(userDetails.getId());
        return ResponseEntity.ok(new ResMessage<>(1, null, null));
    }

    @Operation(summary = "ref 기준 일괄 읽음 처리",
            description = "refType + refId 조합에 해당하는 알림을 일괄 읽음 처리. " +
                          "개발팀 문의 상세 진입 시 해당 work 관련 알림 자동 읽음에 사용")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PatchMapping("/readByRef")
    public ResponseEntity<ResMessage<Void>> markReadByRef(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlarmDTO.ReadByRefReq req) {
        alarmService.markReadByRef(userDetails.getId(), req.refType(), req.refId());
        return ResponseEntity.ok(new ResMessage<>(1, null, null));
    }
}
