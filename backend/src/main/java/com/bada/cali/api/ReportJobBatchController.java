package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.ReportJobBatchDTO;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.ReportJobBatchServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "성적서 작업 배치", description = "성적서작성/결재 작업 배치 생성·조회 API")
@RestController("ApiReportJobBatchController")
@RequestMapping("/api/report/jobs")
@Log4j2
@RequiredArgsConstructor
public class ReportJobBatchController {

    private final ReportJobBatchServiceImpl batchService;

    /**
     * 성적서작성 배치 생성 (WRITE 타입)
     *
     * reportWrite 모달에서 샘플 행 클릭 → 확인 시 이 API를 호출한다.
     * 성적서작성 배치와 개별 item이 DB에 READY 상태로 생성되며,
     * 대상 성적서의 write_status 가 PROGRESS 로 변경된다.
     *
     * 배치+item DB 커밋 완료 후 작업서버 트리거를 별도 트랜잭션으로 호출한다.
     * app.worker.url이 비어 있으면 트리거를 건너뜀(개발 모드).
     *
     * 경로가 /api/admin/** 이 아니므로 Security 필터 자동 제한 없음.
     * 인증된 사용자(실무자 포함) 전체가 호출 가능하다.
     */
    @Operation(
            summary = "성적서작성 배치 생성",
            description = "선택된 성적서 n건과 샘플 id를 받아 성적서작성 배치(WRITE)를 생성. " +
                    "배치+item 커밋 후 작업서버 트리거를 호출. app.worker.url 미설정 시 트리거 생략(개발 모드)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "배치 생성 및 작업서버 트리거 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류 또는 유효하지 않은 성적서 포함",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 성적서 포함",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 작업서버 트리거 실패",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PostMapping("/batches")
    public ResponseEntity<ResMessage<ReportJobBatchDTO.CreateBatchRes>> createWriteBatch(
            @Valid @RequestBody ReportJobBatchDTO.CreateWriteBatchReq req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // 배치+item 생성 (트랜잭션 커밋까지 완료)
        ReportJobBatchDTO.CreateBatchRes res = batchService.createWriteBatch(req, user);

        // 커밋 후 작업서버 트리거 (별도 트랜잭션 — 실패 시 배치/item/report 상태 복원)
        batchService.triggerWorkerServer(res.getBatchId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResMessage<>(1, String.format("성적서작성 작업이 준비되었습니다. (%d건)", res.getTotalCount()), res));
    }

    /**
     * 배치 진행상황 조회 (브라우저 Polling용)
     *
     * 성적서작성 모달 또는 작업 현황 UI에서 일정 주기로 호출한다.
     * 배치 전체 상태 + 소속 item 목록(status, step, message)을 반환한다.
     *
     * 인증된 사용자만 접근 가능 (Security 기본 설정).
     */
    @Operation(
            summary = "배치 진행상황 조회 (Polling)",
            description = "브라우저에서 일정 주기로 호출하여 배치와 개별 item의 진행상황을 조회. " +
                    "인증된 사용자만 접근 가능"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 배치 id",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ResMessage<ReportJobBatchDTO.BatchStatusRes>> getBatchStatus(
            @Parameter(description = "배치 id", example = "123") @PathVariable Long batchId
    ) {
        ReportJobBatchDTO.BatchStatusRes res = batchService.getBatchStatus(batchId);
        return ResponseEntity.ok(new ResMessage<>(1, "배치 상태 조회 성공", res));
    }
}