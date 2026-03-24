package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.ReportJobBatchDTO;
import com.bada.cali.dto.WorkerDataDTO;
import com.bada.cali.service.ReportJobBatchServiceImpl;
import com.bada.cali.service.WorkerDataServiceImpl;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 성적서 작업서버 ↔ CALI 내부 통신 API
 *
 * 경로: /api/worker/**
 * 인증: Spring Security 세션 인증 없음.
 *      X-Worker-Api-Key 헤더로 요청 출처(작업서버)를 검증한다.
 *      app.worker.api-key가 비어 있으면 개발 모드로 간주하여 검증을 생략한다.
 *
 * ⚠️ SecurityConfig에서 /api/worker/** 를 permitAll로 설정해야 동작함.
 *    (세션 없이 접근 가능해야 하므로)
 */
@Tag(name = "성적서 작업서버 연동", description = "작업서버 ↔ CALI 내부 통신 API (X-Worker-Api-Key 인증, 세션 불필요)")
@RestController("WorkerCallbackController")
@RequestMapping("/api/worker")
@Log4j2
@RequiredArgsConstructor
public class WorkerCallbackController {

    private final ReportJobBatchServiceImpl batchService;
    private final WorkerDataServiceImpl workerDataService;

    /** CALI ↔ 작업서버 공용 API 인증 키 */
    @Value("${app.worker.api-key:}")
    private String workerApiKey;

    /**
     * 작업서버가 배치 + item 목록을 조회하는 API.
     *
     * 트리거 수신 후, 처리할 item 목록과 각 reportId를 확인하기 위해 호출한다.
     * 응답 구조는 브라우저 Polling API(GET /api/report/jobs/batches/{id})와 동일하다.
     */
    @Operation(
            summary = "배치 정보 조회 (작업서버용)",
            description = "트리거 수신 후 작업서버가 배치의 item 목록을 확인하기 위해 호출. " +
                    "응답 구조는 브라우저 Polling API와 동일"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "API 키 불일치",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 배치 id",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ResMessage<ReportJobBatchDTO.BatchStatusRes>> getBatchForWorker(
            @Parameter(description = "배치 id", example = "123") @PathVariable Long batchId,
            @RequestHeader(value = "X-Worker-Api-Key", defaultValue = "") String apiKey
    ) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResMessage<>(-1, "유효하지 않은 API 키입니다.", null));
        }
        return ResponseEntity.ok(new ResMessage<>(1, "배치 조회 성공", batchService.getBatchStatus(batchId)));
    }

    /**
     * 작업서버가 개별 item 처리 결과를 CALI에 보고하는 콜백 API.
     *
     * 호출 시점:
     *  - item 처리를 시작할 때 (status=PROGRESS, step=현재단계)
     *  - item 처리가 성공/실패로 끝날 때 (status=SUCCESS 또는 FAIL)
     *
     * item 1건 처리마다 독립 트랜잭션으로 동작하므로
     * 한 건 실패가 다른 item에 영향을 주지 않는다.
     */
    @Operation(
            summary = "item 처리 결과 콜백",
            description = "작업서버가 개별 item의 처리 결과를 CALI에 보고. " +
                    "PROGRESS·SUCCESS·FAIL 상태 변경마다 호출 가능. " +
                    "SUCCESS 시 report.write_status=SUCCESS, write_member_id, write_datetime 갱신"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "status 파라미터 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "403", description = "API 키 불일치",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 item id",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PostMapping("/items/{itemId}/callback")
    public ResponseEntity<ResMessage<Void>> itemCallback(
            @Parameter(description = "item id", example = "456") @PathVariable Long itemId,
            @Valid @RequestBody ReportJobBatchDTO.ItemCallbackReq req,
            @RequestHeader(value = "X-Worker-Api-Key", defaultValue = "") String apiKey
    ) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResMessage<>(-1, "유효하지 않은 API 키입니다.", null));
        }
        batchService.handleItemCallback(itemId, req);
        return ResponseEntity.ok(new ResMessage<>(1, "item 상태 업데이트 완료", null));
    }

    // ── 워커용 데이터 조회 API ─────────────────────────────────────────────────

    /**
     * 성적서시트 셀위치/형식 전체 설정 조회.
     *
     * 워커가 성적서 엑셀 작성 시 fieldCode 별 셀 주소/형식을 알아야 할 때 호출.
     * 데이터 출처: Env(id=1).sheetInfoSetting JSON
     */
    @Operation(
            summary = "성적서시트 설정 조회 (작업서버용)",
            description = "Env.sheetInfoSetting JSON 을 파싱하여 fieldCode → 셀설정 맵으로 반환. " +
                    "워커가 성적서 엑셀 작성 전 셀 위치를 확인할 때 호출"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "API 키 불일치",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "Env 설정 없음",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping("/env/sheet-setting")
    public ResponseEntity<ResMessage<WorkerDataDTO.SheetSettingRes>> getSheetSetting(
            @RequestHeader(value = "X-Worker-Api-Key", defaultValue = "") String apiKey
    ) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResMessage<>(-1, "유효하지 않은 API 키입니다.", null));
        }
        return ResponseEntity.ok(
                new ResMessage<>(1, "성적서시트 설정 조회 성공", workerDataService.getSheetSetting()));
    }

    /**
     * 성적서 엑셀 삽입 데이터 조회.
     *
     * 워커가 엑셀 셀에 값을 채우기 위한 모든 데이터를 하나의 응답으로 반환.
     * Report, CaliOrder, Member, ItemCode, Env, FileInfo 를 조합한 데이터.
     *
     * @param reportId  성적서 id
     * @param sampleId  데이터시트(샘플) 파일의 file_info.id
     */
    @Operation(
            summary = "성적서 엑셀 삽입 데이터 조회 (작업서버용)",
            description = "Report·CaliOrder·Member·ItemCode·Env·FileInfo 를 조합하여 " +
                    "엑셀 셀에 삽입할 모든 데이터를 반환. sampleId = 데이터시트 file_info.id"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "API 키 불일치",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 reportId",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @GetMapping("/reports/{reportId}/fill-data")
    public ResponseEntity<ResMessage<WorkerDataDTO.ReportFillDataRes>> getReportFillData(
            @Parameter(description = "성적서 id", example = "100") @PathVariable Long reportId,
            @Parameter(description = "샘플 파일의 file_info.id (WRITE 전용, WORK_APPROVAL 시 생략 가능)", example = "55")
            @RequestParam(required = false) Long sampleId,
            @RequestHeader(value = "X-Worker-Api-Key", defaultValue = "") String apiKey
    ) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResMessage<>(-1, "유효하지 않은 API 키입니다.", null));
        }
        return ResponseEntity.ok(new ResMessage<>(1, "성적서 삽입 데이터 조회 성공",
                workerDataService.getReportFillData(reportId, sampleId)));
    }

    /**
     * API 키 검증.
     * app.worker.api-key가 비어 있으면 개발 모드로 간주하여 검증을 생략한다.
     */
    private boolean isValidApiKey(String apiKey) {
        if (workerApiKey == null || workerApiKey.isBlank()) {
            log.warn("app.worker.api-key 미설정 — API 키 검증 생략 (개발 모드)");
            return true;
        }
        return workerApiKey.equals(apiKey);
    }
}