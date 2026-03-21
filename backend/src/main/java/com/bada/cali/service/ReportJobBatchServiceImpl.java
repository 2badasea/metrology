package com.bada.cali.service;

import com.bada.cali.common.enums.AppStatus;
import com.bada.cali.common.enums.BatchStatus;
import com.bada.cali.common.enums.JobItemStatus;
import com.bada.cali.common.enums.JobType;
import com.bada.cali.common.enums.ReportType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.ReportJobBatchDTO;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Report;
import com.bada.cali.entity.ReportJobBatch;
import com.bada.cali.entity.ReportJobItem;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.ReportJobBatchRepository;
import com.bada.cali.repository.ReportJobItemRepository;
import com.bada.cali.repository.ReportRepository;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ReportJobBatchServiceImpl {

    private final ReportRepository            reportRepository;
    private final ReportJobBatchRepository    batchRepository;
    private final ReportJobItemRepository     itemRepository;
    private final LogRepository               logRepository;
    private final FileInfoRepository          fileInfoRepository;

    // ── 작업서버 연동 설정 (@Value 주입) ──────────────────────────────────────

    /** 작업서버 베이스 URL. 비어 있으면 트리거 생략(개발 모드). */
    @Value("${app.worker.url:}")
    private String workerUrl;

    /** CALI ↔ 작업서버 공용 API 인증 키 */
    @Value("${app.worker.api-key:}")
    private String workerApiKey;

    /** CALI 서버의 콜백 베이스 URL (작업서버가 이 주소로 콜백 요청) */
    @Value("${app.cali.callback-base-url:http://localhost:8050}")
    private String callbackBaseUrl;

    // NCP 오브젝트 스토리지 접속 정보 — 트리거 페이로드에 포함
    @Value("${ncp.storage.endpoint}")       private String storageEndpoint;
    @Value("${ncp.storage.bucket-name}")    private String storageBucketName;
    @Value("${ncp.storage.root-dir}")       private String storageRootDir;
    @Value("${ncp.storage.access-key}")     private String storageAccessKey;
    @Value("${ncp.storage.secret-key}")     private String storageSecretKey;

    /** 작업서버 트리거 요청에 사용하는 HTTP 클라이언트 */
    private final RestClient restClient = RestClient.create();

    /**
     * 성적서작성 배치 생성 (WRITE 타입)
     *
     * 처리 순서:
     *  1. 대상 성적서 유효성 검증
     *     - 존재 여부, soft delete 여부, SELF 타입 여부, 소분류 설정 여부
     *     - write_status 가 PROGRESS 인 경우(이미 처리 중) 제외하고 경고
     *  2. ReportJobBatch 생성 (status=READY)
     *  3. 각 성적서에 대해 ReportJobItem 생성 (status=READY)
     *  4. 대상 성적서의 write_status → PROGRESS 업데이트
     *  5. 작업 로그 기록
     *  6. 배치 id + 총 건수 반환
     *
     * NOTE: 작업서버 트리거(POST /api/jobs/execute)는 현재 미구현.
     *       배치/아이템을 DB에 READY 상태로만 생성해두고,
     *       추후 작업서버 연동 시 이 메서드 하단에 트리거 호출을 추가한다.
     *
     * @param req  성적서 id 목록 + 샘플 id
     * @param user 요청자 (로그인 사용자)
     * @return 생성된 배치 id + 총 건수
     */
    @Transactional
    public ReportJobBatchDTO.CreateBatchRes createWriteBatch(
            ReportJobBatchDTO.CreateWriteBatchReq req,
            CustomUserDetails user
    ) {
        List<Long> reportIds = req.getReportIds();

        // ── 1. 대상 성적서 조회 및 유효성 검증 ────────────────────────────────
        List<Report> reports = reportRepository.findAllById(reportIds);

        // 요청한 id 수와 실제 조회된 수가 다르면 일부 id가 존재하지 않는 것
        if (reports.size() != reportIds.size()) {
            throw new EntityNotFoundException("존재하지 않는 성적서가 포함되어 있습니다.");
        }

        // 각 성적서에 대한 세부 검증
        List<Long> validatedIds = new ArrayList<>();
        for (Report report : reports) {
            // soft delete 된 성적서 제외
            if (report.getIsVisible() != YnType.y) {
                throw new IllegalArgumentException(
                        String.format("삭제된 성적서가 포함되어 있습니다. (id: %d)", report.getId()));
            }
            // 자체성적서(SELF)만 처리 가능
            if (report.getReportType() != ReportType.SELF) {
                throw new IllegalArgumentException(
                        String.format("자체성적서(SELF)만 성적서작성이 가능합니다. (id: %d)", report.getId()));
            }
            // 소분류 미설정 성적서는 처리 불가
            if (report.getSmallItemCodeId() == null) {
                throw new IllegalArgumentException(
                        String.format("소분류가 설정되지 않은 성적서가 포함되어 있습니다. (id: %d)", report.getId()));
            }
            // 이미 성적서작성 진행 중인 경우 중복 실행 방지
            if (report.getWriteStatus() == AppStatus.PROGRESS) {
                throw new IllegalArgumentException(
                        String.format("이미 성적서작성이 진행 중인 성적서가 포함되어 있습니다. (id: %d, 성적서번호: %s)",
                                report.getId(), report.getReportNum()));
            }
            // 실무자결재가 대기(READY) 또는 진행 중(PROGRESS)인 경우 성적서작성 차단
            // (결재 진행 중 원본 파일 교체 방지)
            if (report.getWorkStatus() == AppStatus.READY || report.getWorkStatus() == AppStatus.PROGRESS) {
                throw new IllegalArgumentException(
                        String.format("실무자결재가 진행 중인 성적서가 포함되어 있습니다. (id: %d, 성적서번호: %s)",
                                report.getId(), report.getReportNum()));
            }
            // 기술책임자결재가 대기(READY) 또는 진행 중(PROGRESS)인 경우 성적서작성 차단
            if (report.getApprovalStatus() == AppStatus.READY || report.getApprovalStatus() == AppStatus.PROGRESS) {
                throw new IllegalArgumentException(
                        String.format("기술책임자결재가 진행 중인 성적서가 포함되어 있습니다. (id: %d, 성적서번호: %s)",
                                report.getId(), report.getReportNum()));
            }
            validatedIds.add(report.getId());
        }

        // ── 2. ReportJobBatch 생성 ────────────────────────────────────────────
        ReportJobBatch batch = ReportJobBatch.builder()
                .jobType(JobType.WRITE)
                .requestMemberId(user.getId())
                .sampleId(req.getSampleId())
                .totalCount(validatedIds.size())
                .status(BatchStatus.READY)
                .createDatetime(LocalDateTime.now())
                .build();
        batch = batchRepository.save(batch);
        final Long batchId = batch.getId();

        // ── 3. ReportJobItem 생성 (성적서 1건 = item 1건, 초기 상태: READY) ──
        List<ReportJobItem> items = validatedIds.stream()
                .map(reportId -> ReportJobItem.builder()
                        .batchId(batchId)
                        .reportId(reportId)
                        .build()  // status 기본값: READY
                )
                .collect(Collectors.toList());
        itemRepository.saveAll(items);

        // ── 4. 대상 성적서 write_status → PROGRESS 업데이트 ──────────────────
        for (Report report : reports) {
            report.setWriteStatus(AppStatus.PROGRESS);
        }
        // dirty checking으로 flush → @Transactional 범위 내에서 자동 반영

        // ── 5. 작업 로그 기록 ──────────────────────────────────────────────────
        String logContent = String.format("성적서작성 배치 생성 (batchId: %d) — 대상 성적서 id - %s",
                batchId,
                validatedIds.toString());
        Log logEntry = Log.builder()
                .logIp(null)  // 컨트롤러에서 IP를 넘겨줘도 되지만 현재는 생략
                .workerName(user.getName())   // name: 직원 실명 (username은 로그인 id)
                .logContent(logContent)
                .logType("i")
                .refTable("report_job_batch")
                .refTableId(batchId)
                .createDatetime(LocalDateTime.now())
                .createMemberId(user.getId())
                .build();
        logRepository.save(logEntry);

        log.info("성적서작성 배치 생성 완료 — batchId: {}, 대상 건수: {}, 요청자: {}",
                batchId, validatedIds.size(), user.getName());

        return new ReportJobBatchDTO.CreateBatchRes(batchId, validatedIds.size());
    }

    // ── 배치 진행상황 조회 (Polling / 작업서버 배치 조회 공용) ─────────────────

    /**
     * 배치 진행상황 조회.
     * 브라우저 Polling(GET /api/report/jobs/batches/{id})과
     * 작업서버 배치+item 조회(GET /api/worker/batches/{id}) 모두에서 호출한다.
     *
     * @param batchId 조회할 배치 id
     * @return 배치 상태 + 소속 item 목록
     */
    @Transactional(readOnly = true)
    public ReportJobBatchDTO.BatchStatusRes getBatchStatus(Long batchId) {
        ReportJobBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 배치입니다. (id: " + batchId + ")"));

        List<ReportJobItem> items = itemRepository.findByBatchId(batchId);

        List<ReportJobBatchDTO.ItemStatusRes> itemResList = items.stream()
                .map(item -> new ReportJobBatchDTO.ItemStatusRes(
                        item.getId(),
                        item.getReportId(),
                        item.getStatus().name(),
                        item.getStep(),
                        item.getMessage(),
                        item.getStartDatetime(),
                        item.getEndDatetime()
                ))
                .toList();

        return new ReportJobBatchDTO.BatchStatusRes(
                batch.getId(),
                batch.getJobType().name(),
                batch.getStatus().name(),
                batch.getTotalCount(),
                batch.getSuccessCount(),
                batch.getFailCount(),
                batch.getSampleId(),
                batch.getCreateDatetime(),
                batch.getStartDatetime(),
                batch.getEndDatetime(),
                itemResList
        );
    }

    // ── 작업서버 콜백 처리 ────────────────────────────────────────────────────

    /**
     * 작업서버로부터 item 처리 결과를 수신하여 DB 상태를 업데이트한다.
     *
     * 처리 순서:
     *  1. item 조회
     *  2. 연관 batch 조회
     *  3. status 파싱 및 item 상태 업데이트
     *  4. PROGRESS: 배치 상태를 READY → PROGRESS로 전환 (최초 1회)
     *     SUCCESS:  report.write_status=SUCCESS, write_member_id, write_datetime 업데이트. batch.successCount++
     *     FAIL:     report.write_status=FAIL. batch.failCount++
     *  5. 모든 item 처리 완료 시 배치 최종 상태(SUCCESS/FAIL) 결정
     *
     * item 1건 = 독립 트랜잭션 → 1건 실패가 다른 item에 영향 없음.
     *
     * @param itemId 처리된 item id
     * @param req    작업서버가 전송한 콜백 요청 (status, step, message)
     */
    @Transactional
    public void handleItemCallback(Long itemId, ReportJobBatchDTO.ItemCallbackReq req) {
        // ── 1. item 조회 ─────────────────────────────────────────────────────
        ReportJobItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 item입니다. (id: " + itemId + ")"));

        // ── 2. 연관 배치 조회 ─────────────────────────────────────────────────
        ReportJobBatch batch = batchRepository.findById(item.getBatchId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "연관 배치를 찾을 수 없습니다. (batchId: " + item.getBatchId() + ")"));

        // ── 3. status 파싱 ────────────────────────────────────────────────────
        JobItemStatus newStatus;
        try {
            newStatus = JobItemStatus.valueOf(req.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 status 값입니다: " + req.getStatus()
                    + " (허용값: PROGRESS / SUCCESS / FAIL)");
        }

        // ── 4. item 상태 업데이트 ─────────────────────────────────────────────
        item.setStatus(newStatus);
        if (req.getStep()    != null) item.setStep(req.getStep());
        if (req.getMessage() != null) item.setMessage(req.getMessage());

        LocalDateTime now = LocalDateTime.now();

        switch (newStatus) {
            case PROGRESS -> {
                // 처음 PROGRESS 진입 시에만 시작일시 기록 (중간 단계 업데이트 시 덮어쓰지 않음)
                if (item.getStartDatetime() == null) {
                    item.setStartDatetime(now);
                }
                // 배치: READY → PROGRESS (첫 번째 item이 처리 시작될 때 1회만 전환)
                if (batch.getStatus() == BatchStatus.READY) {
                    batch.setStatus(BatchStatus.PROGRESS);
                    batch.setStartDatetime(now);
                }
            }
            case SUCCESS -> {
                item.setEndDatetime(now);
                // 대상 성적서 write_status 갱신 (성적서작성 성공)
                Report report = reportRepository.findById(item.getReportId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "성적서를 찾을 수 없습니다. (id: " + item.getReportId() + ")"));
                report.setWriteStatus(AppStatus.SUCCESS);
                report.setWriteMemberId(batch.getRequestMemberId());  // 작업 요청자가 작성자
                report.setWriteDatetime(now);
                batch.setSuccessCount(batch.getSuccessCount() + 1);

                // 성적서 원본 파일 file_info 등록
                // S3 고정 objectKey: {rootDir}/report/{reportId}/origin.xlsx
                // → 파일 다운로드는 file_info.id 가 아닌 report.id 기반 고정 경로로 처리되므로
                //   file_info 는 "파일 존재 여부" 표시 용도로만 사용됨 (아이콘 표시 조건)
                // 기존 원본 file_info가 있으면 먼저 소프트삭제 (재작성 시 중복 방지)
                String reportDir = "report/" + item.getReportId() + "/";
                fileInfoRepository.softDeleteVisibleByRefAndDir(
                        "report",
                        item.getReportId(),
                        reportDir,
                        YnType.n,
                        now,
                        batch.getRequestMemberId()
                );
                FileInfo reportFile = FileInfo.builder()
                        .refTableName("report")
                        .refTableId(item.getReportId())
                        .originName("origin.xlsx")
                        .name("origin")
                        .extension("xlsx")
                        .fileSize(0L)          // 워커가 파일 크기를 콜백으로 전달하지 않으므로 0으로 기록
                        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .dir(reportDir)
                        .createMemberId(batch.getRequestMemberId())
                        .createDatetime(now)
                        .build();
                fileInfoRepository.save(reportFile);
                log.debug("성적서 원본 file_info 생성 완료 — reportId: {}, dir: {}", item.getReportId(), reportDir);
            }
            case FAIL -> {
                item.setEndDatetime(now);
                // 대상 성적서 write_status → FAIL (재시도 가능 상태임을 UI에서 표시)
                Report report = reportRepository.findById(item.getReportId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "성적서를 찾을 수 없습니다. (id: " + item.getReportId() + ")"));
                report.setWriteStatus(AppStatus.FAIL);
                batch.setFailCount(batch.getFailCount() + 1);
            }
        }

        // ── 5. 배치 완료 여부 체크 ───────────────────────────────────────────
        // SUCCESS 또는 FAIL 전환 시에만 완료 건수를 재계산한다
        if (newStatus == JobItemStatus.SUCCESS || newStatus == JobItemStatus.FAIL) {
            int doneCount = batch.getSuccessCount() + batch.getFailCount();
            if (doneCount >= batch.getTotalCount()) {
                // 모든 item 처리 완료 → 배치 최종 상태 결정
                batch.setEndDatetime(now);
                batch.setStatus(batch.getFailCount() > 0 ? BatchStatus.FAIL : BatchStatus.SUCCESS);
                log.info("배치 처리 완료 — batchId: {}, success: {}, fail: {}",
                        batch.getId(), batch.getSuccessCount(), batch.getFailCount());
            }
            // SUCCESS 케이스에서 softDeleteVisibleByRefAndDir(clearAutomatically=true)가 실행되면
            // EntityManager 캐시가 클리어되어 batch 가 detached 상태가 된다.
            // dirty-checking 으로 변경사항이 반영되지 않을 수 있으므로 명시적으로 save(merge) 호출.
            batchRepository.save(batch);
        }

        log.info("item 콜백 처리 완료 — itemId: {}, status: {}, step: {}, batchId: {}",
                itemId, newStatus, req.getStep(), batch.getId());
    }

    // ── 작업서버 트리거 ────────────────────────────────────────────────────────

    /**
     * 작업서버 트리거 호출.
     *
     * createWriteBatch() 트랜잭션이 커밋된 후 컨트롤러에서 별도로 호출된다.
     * (같은 트랜잭션 내에서 호출하면 트리거 실패 시 배치 자체가 롤백되어 복구 불가)
     *
     * app.worker.url이 비어 있으면 개발 모드로 간주하고 트리거를 생략한다.
     * 트리거 실패 시: 배치 FAIL, item CANCELED, report.write_status IDLE 복원 후 예외 throw.
     *
     * @param batchId 트리거할 배치 id (이미 DB에 커밋된 상태)
     */
    @Transactional
    public void triggerWorkerServer(Long batchId) {
        // ── 개발 모드: 작업서버 URL 미설정 시 트리거 생략 ────────────────────
        if (workerUrl == null || workerUrl.isBlank()) {
            log.info("app.worker.url 미설정 — 작업서버 트리거 생략 (개발 모드, batchId: {})", batchId);
            return;
        }

        ReportJobBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("배치를 찾을 수 없습니다. (id: " + batchId + ")"));

        // ── 트리거 페이로드 구성 (스토리지 접속정보 + 콜백 URL 포함) ─────────
        // NOTE: 스토리지 시크릿 등 민감값은 DB에 저장하지 않고 요청 시에만 전달
        ReportJobBatchDTO.WorkerTriggerReq triggerReq = ReportJobBatchDTO.WorkerTriggerReq.builder()
                .batchId(batchId)
                .callbackBaseUrl(callbackBaseUrl)
                .workerApiKey(workerApiKey)
                .storageEndpoint(storageEndpoint)
                .storageBucketName(storageBucketName)
                .storageRootDir(storageRootDir)
                .storageAccessKey(storageAccessKey)
                .storageSecretKey(storageSecretKey)
                .build();

        try {
            restClient.post()
                    .uri(workerUrl + "/api/jobs/execute")
                    .header("X-Worker-Api-Key", workerApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(triggerReq)
                    .retrieve()
                    .toBodilessEntity();

            log.info("작업서버 트리거 성공 — batchId: {}, workerUrl: {}", batchId, workerUrl);

        } catch (Exception e) {
            log.error("작업서버 트리거 실패 — batchId: {}, workerUrl: {}", batchId, workerUrl, e);

            // ── 트리거 실패 복구 ──────────────────────────────────────────────
            // 배치 FAIL 처리
            batch.setStatus(BatchStatus.FAIL);

            // item CANCELED, report write_status IDLE 복원 (재시도 가능 상태로)
            List<ReportJobItem> items = itemRepository.findByBatchId(batchId);
            List<Long> reportIds = items.stream().map(ReportJobItem::getReportId).toList();

            for (ReportJobItem item : items) {
                item.setStatus(JobItemStatus.CANCELED);
                item.setMessage("작업서버 트리거 실패로 자동 취소됨");
            }
            List<Report> reports = reportRepository.findAllById(reportIds);
            for (Report report : reports) {
                report.setWriteStatus(AppStatus.IDLE);
            }

            throw new RuntimeException("작업서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요. (batchId: " + batchId + ")");
        }
    }
}