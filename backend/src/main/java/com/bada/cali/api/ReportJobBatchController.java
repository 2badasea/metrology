package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.ReportJobBatchDTO;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.ReportJobBatchServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
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
     * 작업서버 트리거는 현재 미구현(TODO). 배치 생성 후 별도 연동 예정.
     *
     * 경로가 /api/admin/** 이 아니므로 Security 필터 자동 제한 없음.
     * 인증된 사용자(실무자 포함) 전체가 호출 가능하다.
     */
    @Operation(
            summary = "성적서작성 배치 생성",
            description = "선택된 성적서 n건과 샘플 id를 받아 성적서작성 배치(WRITE)를 생성. " +
                    "대상 성적서의 write_status가 PROGRESS로 변경되며, 작업서버 트리거는 추후 구현 예정"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "배치 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류 또는 유효하지 않은 성적서 포함",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 성적서 포함",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PostMapping("/batches")
    public ResponseEntity<ResMessage<ReportJobBatchDTO.CreateBatchRes>> createWriteBatch(
            @Valid @RequestBody ReportJobBatchDTO.CreateWriteBatchReq req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ReportJobBatchDTO.CreateBatchRes res = batchService.createWriteBatch(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResMessage<>(1, String.format("성적서작성 작업이 준비되었습니다. (%d건)", res.getTotalCount()), res));
    }
}