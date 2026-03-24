package com.bada.cali.service;

import com.bada.cali.common.enums.AppStatus;
import com.bada.cali.common.enums.BatchStatus;
import com.bada.cali.common.enums.JobItemStatus;
import com.bada.cali.common.enums.JobType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.config.NcpStorageProperties;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.entity.Report;
import com.bada.cali.entity.ReportJobBatch;
import com.bada.cali.entity.ReportJobItem;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.repository.ReportJobBatchRepository;
import com.bada.cali.repository.ReportJobItemRepository;
import com.bada.cali.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 데모 모드 배치 처리 서비스
 *
 * app.worker.url 이 비어 있을 때(이력서 오픈 / 로컬 데모 환경) 실제 cali-worker 대신 동작한다.
 * C:/BadaDev/cali/etc/ 경로의 고정 샘플 파일(BD26-0004-0001.xlsx / .pdf)을
 * 스토리지의 올바른 위치에 업로드하여 성적서작성·실무자결재가 완료된 것처럼 처리한다.
 *
 * 각 단계 사이에 인위적 딜레이를 주어 폴링 UI에서 단계별 진행상황이 표시된다.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * 모드 전환 방법 (application.properties)
 *   데모 모드 활성:  app.worker.url=                 (비워두면 이 서비스가 처리)
 *   실제 모드 전환:  app.worker.url=http://localhost:8060  (cali-worker 사용)
 * ──────────────────────────────────────────────────────────────────────────────
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class DemoJobProcessorService {

    private final ReportJobBatchRepository  batchRepository;
    private final ReportJobItemRepository   itemRepository;
    private final ReportRepository          reportRepository;
    private final FileInfoRepository        fileInfoRepository;
    private final S3Client                  ncloudS3Client;
    private final NcpStorageProperties      storageProps;

    /** 데모 파일 디렉토리 (기본값: C:/BadaDev/cali/etc) */
    @Value("${app.demo.file-dir:C:/BadaDev/cali/etc}")
    private String demoFileDir;

    // 데모용 고정 파일명 (etc 디렉토리 내)
    private static final String DEMO_XLSX_NAME = "BD26-0004-0001.xlsx";
    private static final String DEMO_PDF_NAME  = "BD26-0004-0001.pdf";

    // XLSX MIME 타입
    private static final String CT_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 데모 배치 비동기 처리.
     *
     * ReportJobBatchServiceImpl.triggerWorkerServer() 에서
     * workerUrl이 비어 있을 때 이 메서드를 호출한다.
     * @Async로 즉시 반환되고, 내부에서 단계별 딜레이를 주며 item을 처리한다.
     *
     * @param batchId 처리할 배치 id (이미 DB에 READY 상태로 커밋된 상태)
     */
    @Async
    public void process(Long batchId) {
        log.info("[Demo] 배치 처리 시작 — batchId: {}", batchId);
        try {
            processInternal(batchId);
        } catch (Exception e) {
            // 예상치 못한 치명적 오류 (배치 조회 실패, JPA 오류 등) — 전체 배치 강제 FAIL 처리
            log.error("[Demo] 배치 처리 중 치명적 오류 — batchId: {}. 배치 전체를 FAIL로 강제 처리합니다.", batchId, e);
            forceFailEntireBatch(batchId, "[Demo 치명적 오류] " + e.getMessage());
        }
        log.info("[Demo] 배치 처리 완료 — batchId: {}", batchId);
    }

    /**
     * 실제 배치 처리 로직.
     * process() 에서 최상위 try-catch로 감싸서 호출한다.
     */
    private void processInternal(Long batchId) {
        ReportJobBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            log.error("[Demo] 배치를 찾을 수 없음 — batchId: {}", batchId);
            return;
        }

        List<ReportJobItem> items = itemRepository.findByBatchId(batchId);
        if (items.isEmpty()) {
            log.warn("[Demo] 배치에 item이 없음 — batchId: {}. 배치를 FAIL로 처리합니다.", batchId);
            batch.setStatus(BatchStatus.FAIL);
            batchRepository.save(batch);
            return;
        }

        JobType jobType = batch.getJobType();

        for (ReportJobItem item : items) {
            try {
                if (jobType == JobType.WRITE) {
                    processWriteItem(batch, item);
                } else if (jobType == JobType.WORK_APPROVAL) {
                    processWorkApprovalItem(batch, item);
                } else {
                    log.warn("[Demo] 지원하지 않는 jobType: {} — itemId: {}", jobType, item.getId());
                    failItem(batch, item, "지원하지 않는 jobType: " + jobType);
                }
            } catch (Exception e) {
                log.error("[Demo] item 처리 중 오류 — itemId: {}, batchId: {}", item.getId(), batchId, e);
                failItem(batch, item, e.getMessage());
            }
        }
    }

    // ── WRITE 처리 ─────────────────────────────────────────────────────────────
    // 단계: DOWNLOADING_TEMPLATE → FILLING_DATA → UPLOADING_ORIGIN → DONE

    private void processWriteItem(ReportJobBatch batch, ReportJobItem item) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();

        // [1] 배치 PROGRESS 전환 + item 시작 기록
        markBatchProgressIfNeeded(batch, startTime);
        updateItemProgress(item, "DOWNLOADING_TEMPLATE", startTime);
        sleep(1500); // 템플릿 다운로드 시뮬레이션

        // [2] 데이터 삽입 시뮬레이션
        updateItemStep(item, "FILLING_DATA");
        sleep(2000);

        // [3] 스토리지 업로드
        updateItemStep(item, "UPLOADING_ORIGIN");
        // 데모 xlsx → {rootDir}/report/{reportId}/origin.xlsx
        String xlsxKey = buildReportKey(item.getReportId(), "origin.xlsx");
        uploadDemoFile(DEMO_XLSX_NAME, xlsxKey, CT_XLSX);
        sleep(800);

        // [4] item 완료 + report 상태 업데이트
        LocalDateTime doneTime = LocalDateTime.now();
        updateItemStep(item, "DONE");
        finishItemSuccess(item, doneTime);

        Report report = reportRepository.findById(item.getReportId()).orElseThrow();
        report.setWriteStatus(AppStatus.SUCCESS);
        report.setWriteMemberId(batch.getRequestMemberId());
        report.setWriteDatetime(doneTime);
        reportRepository.save(report);

        // 기존 file_info 소프트삭제 후 origin file_info 등록
        String reportDir = "report/" + item.getReportId() + "/";
        fileInfoRepository.softDeleteVisibleByRefAndDir(
                "report", item.getReportId(), reportDir,
                YnType.n, doneTime, batch.getRequestMemberId());
        fileInfoRepository.save(FileInfo.builder()
                .refTableName("report")
                .refTableId(item.getReportId())
                .originName("origin.xlsx")
                .name("origin")
                .extension("xlsx")
                .fileSize(0L)
                .contentType(CT_XLSX)
                .dir(reportDir)
                .createMemberId(batch.getRequestMemberId())
                .createDatetime(doneTime)
                .build());

        incrementBatchSuccess(batch, doneTime);
        log.info("[Demo] WRITE item 완료 — itemId: {}, reportId: {}", item.getId(), item.getReportId());
    }

    // ── WORK_APPROVAL 처리 ─────────────────────────────────────────────────────
    // 단계: DOWNLOADING_ORIGIN → INSERTING_SIGN → CONVERTING_PDF → UPLOADING_SIGNED → DONE

    private void processWorkApprovalItem(ReportJobBatch batch, ReportJobItem item) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();

        // [1] 배치 PROGRESS 전환 + item 시작 기록
        markBatchProgressIfNeeded(batch, startTime);
        updateItemProgress(item, "DOWNLOADING_ORIGIN", startTime);
        sleep(1500); // origin 다운로드 시뮬레이션

        // [2] 서명 삽입 시뮬레이션
        updateItemStep(item, "INSERTING_SIGN");
        sleep(2000);

        // [3] PDF 변환 시뮬레이션
        updateItemStep(item, "CONVERTING_PDF");
        sleep(2000);

        // [4] 스토리지 업로드
        updateItemStep(item, "UPLOADING_SIGNED");
        // 데모 xlsx → {rootDir}/report/{reportId}/signed.xlsx
        // 데모 pdf  → {rootDir}/report/{reportId}/signed.pdf
        String xlsxKey = buildReportKey(item.getReportId(), "signed.xlsx");
        String pdfKey  = buildReportKey(item.getReportId(), "signed.pdf");
        uploadDemoFile(DEMO_XLSX_NAME, xlsxKey, CT_XLSX);
        uploadDemoFile(DEMO_PDF_NAME,  pdfKey,  "application/pdf");
        sleep(800);

        // [5] item 완료 + report 상태 업데이트
        LocalDateTime doneTime = LocalDateTime.now();
        updateItemStep(item, "DONE");
        finishItemSuccess(item, doneTime);

        Report report = reportRepository.findById(item.getReportId()).orElseThrow();
        report.setWorkStatus(AppStatus.SUCCESS);
        report.setWorkDatetime(doneTime);
        reportRepository.save(report);

        // 기존 signed_xlsx / signed_pdf file_info 소프트삭제 후 신규 등록
        String reportDir = "report/" + item.getReportId() + "/";
        fileInfoRepository.softDeleteByRefAndNames(
                "report", item.getReportId(),
                List.of("signed_xlsx", "signed_pdf"),
                YnType.n, doneTime, batch.getRequestMemberId());
        fileInfoRepository.save(FileInfo.builder()
                .refTableName("report")
                .refTableId(item.getReportId())
                .originName("signed.xlsx")
                .name("signed_xlsx")
                .extension("xlsx")
                .fileSize(0L)
                .contentType(CT_XLSX)
                .dir(reportDir)
                .createMemberId(batch.getRequestMemberId())
                .createDatetime(doneTime)
                .build());
        fileInfoRepository.save(FileInfo.builder()
                .refTableName("report")
                .refTableId(item.getReportId())
                .originName("signed.pdf")
                .name("signed_pdf")
                .extension("pdf")
                .fileSize(0L)
                .contentType("application/pdf")
                .dir(reportDir)
                .createMemberId(batch.getRequestMemberId())
                .createDatetime(doneTime)
                .build());

        incrementBatchSuccess(batch, doneTime);
        log.info("[Demo] WORK_APPROVAL item 완료 — itemId: {}, reportId: {}", item.getId(), item.getReportId());
    }

    // ── 헬퍼 메서드 ────────────────────────────────────────────────────────────

    /**
     * 보고서 파일 S3 objectKey 조합.
     * 규칙: {rootDir}/report/{reportId}/{fileName}
     * (FileServiceImpl.downloadReportFile() 과 동일한 규칙)
     */
    private String buildReportKey(Long reportId, String fileName) {
        return storageProps.getRootDir() + "/report/" + reportId + "/" + fileName;
    }

    /**
     * 데모 파일을 S3에 업로드.
     *
     * @param demoFileName etc 디렉토리 내 파일명 (BD26-0004-0001.xlsx 등)
     * @param objectKey    업로드할 S3 objectKey
     * @param contentType  MIME 타입
     */
    private void uploadDemoFile(String demoFileName, String objectKey, String contentType) throws Exception {
        File file = new File(demoFileDir, demoFileName);
        if (!file.exists()) {
            throw new IllegalStateException(
                    "데모 파일이 존재하지 않습니다: " + file.getAbsolutePath() +
                    " — app.demo.file-dir 설정을 확인하세요.");
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(storageProps.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();
        ncloudS3Client.putObject(req, RequestBody.fromBytes(bytes));
        log.debug("[Demo] S3 업로드 완료 — key: {}", objectKey);
    }

    /** item 상태를 PROGRESS로 전환하고 시작 시각을 기록 */
    private void updateItemProgress(ReportJobItem item, String step, LocalDateTime startTime) {
        item.setStatus(JobItemStatus.PROGRESS);
        item.setStep(step);
        item.setStartDatetime(startTime);
        itemRepository.save(item);
    }

    /** item의 현재 단계(step)만 갱신 (status는 PROGRESS 유지) */
    private void updateItemStep(ReportJobItem item, String step) {
        item.setStep(step);
        itemRepository.save(item);
    }

    /** item을 SUCCESS로 최종 완료 처리 */
    private void finishItemSuccess(ReportJobItem item, LocalDateTime endTime) {
        item.setStatus(JobItemStatus.SUCCESS);
        item.setEndDatetime(endTime);
        itemRepository.save(item);
    }

    /**
     * 배치를 READY → PROGRESS로 전환 (최초 item 처리 시작 시 1회만 실행).
     * 이미 PROGRESS 이상이면 아무 작업도 하지 않는다.
     */
    private void markBatchProgressIfNeeded(ReportJobBatch batch, LocalDateTime startTime) {
        if (batch.getStatus() == BatchStatus.READY) {
            batch.setStatus(BatchStatus.PROGRESS);
            batch.setStartDatetime(startTime);
            batchRepository.save(batch);
        }
    }

    /**
     * 배치 successCount 증가. 모든 item 처리 완료 시 배치를 SUCCESS로 마감.
     * 동시성 이슈 방지를 위해 DB에서 최신 배치 상태를 재조회 후 증가시킨다.
     */
    private void incrementBatchSuccess(ReportJobBatch batch, LocalDateTime doneTime) {
        // 다른 item이 이미 카운트를 올렸을 수 있으므로 DB에서 최신 값 재조회
        ReportJobBatch fresh = batchRepository.findById(batch.getId()).orElse(batch);
        fresh.setSuccessCount(fresh.getSuccessCount() + 1);
        int done = fresh.getSuccessCount() + fresh.getFailCount();
        if (done >= fresh.getTotalCount()) {
            fresh.setStatus(BatchStatus.SUCCESS);
            fresh.setEndDatetime(doneTime);
        }
        batchRepository.save(fresh);
        // 로컬 참조도 최신화 (markBatchProgressIfNeeded가 중복 PROGRESS 전환하지 않도록)
        batch.setStatus(fresh.getStatus());
        batch.setSuccessCount(fresh.getSuccessCount());
    }

    /**
     * item 실패 처리.
     * item status=FAIL, report 작업 status=FAIL, 배치 failCount 증가.
     */
    private void failItem(ReportJobBatch batch, ReportJobItem item, String message) {
        LocalDateTime now = LocalDateTime.now();

        item.setStatus(JobItemStatus.FAIL);
        item.setMessage(message != null ? message : "알 수 없는 오류");
        item.setEndDatetime(now);
        itemRepository.save(item);

        // report 작업 상태 FAIL로 복원
        reportRepository.findById(item.getReportId()).ifPresent(report -> {
            if (batch.getJobType() == JobType.WORK_APPROVAL) {
                report.setWorkStatus(AppStatus.FAIL);
            } else {
                report.setWriteStatus(AppStatus.FAIL);
            }
            reportRepository.save(report);
        });

        // 배치 failCount 증가
        ReportJobBatch fresh = batchRepository.findById(batch.getId()).orElse(batch);
        fresh.setFailCount(fresh.getFailCount() + 1);
        int done = fresh.getSuccessCount() + fresh.getFailCount();
        if (done >= fresh.getTotalCount()) {
            fresh.setStatus(BatchStatus.FAIL);
            fresh.setEndDatetime(now);
        }
        batchRepository.save(fresh);
        batch.setStatus(fresh.getStatus());
        batch.setFailCount(fresh.getFailCount());
    }

    /**
     * 치명적 오류 시 배치 전체 강제 FAIL 처리.
     * 아직 처리되지 않은(READY/PROGRESS) item 및 연관 report를 모두 FAIL로 설정한다.
     * 이후 사용자가 재작업을 요청할 수 있다.
     */
    private void forceFailEntireBatch(Long batchId, String reason) {
        try {
            LocalDateTime now = LocalDateTime.now();
            ReportJobBatch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) return;

            List<ReportJobItem> items = itemRepository.findByBatchId(batchId);
            for (ReportJobItem item : items) {
                // 이미 최종 상태(SUCCESS/FAIL/CANCELED)인 item은 건드리지 않음
                if (item.getStatus() == JobItemStatus.SUCCESS
                        || item.getStatus() == JobItemStatus.FAIL
                        || item.getStatus() == JobItemStatus.CANCELED) {
                    continue;
                }
                item.setStatus(JobItemStatus.FAIL);
                item.setMessage(reason);
                item.setEndDatetime(now);
                itemRepository.save(item);

                // 연관 report 상태 복원
                reportRepository.findById(item.getReportId()).ifPresent(report -> {
                    if (batch.getJobType() == JobType.WORK_APPROVAL) {
                        report.setWorkStatus(AppStatus.FAIL);
                    } else {
                        report.setWriteStatus(AppStatus.FAIL);
                    }
                    reportRepository.save(report);
                });

                batch.setFailCount(batch.getFailCount() + 1);
            }

            batch.setStatus(BatchStatus.FAIL);
            batch.setEndDatetime(now);
            batchRepository.save(batch);

            log.info("[Demo] 배치 강제 FAIL 완료 — batchId: {}, failCount: {}", batchId, batch.getFailCount());
        } catch (Exception e) {
            // forceFailEntireBatch 자체가 실패해도 더 이상 할 수 없음 — 로그만 남김
            log.error("[Demo] 배치 강제 FAIL 처리 중 추가 오류 발생 — batchId: {}", batchId, e);
        }
    }

    /** 처리 지연 (인터럽트 발생 시 스레드 상태 복원) */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
