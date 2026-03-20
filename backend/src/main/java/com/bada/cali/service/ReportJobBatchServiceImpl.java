package com.bada.cali.service;

import com.bada.cali.common.enums.AppStatus;
import com.bada.cali.common.enums.BatchStatus;
import com.bada.cali.common.enums.JobType;
import com.bada.cali.common.enums.ReportType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.ReportJobBatchDTO;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Report;
import com.bada.cali.entity.ReportJobBatch;
import com.bada.cali.entity.ReportJobItem;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.ReportJobBatchRepository;
import com.bada.cali.repository.ReportJobItemRepository;
import com.bada.cali.repository.ReportRepository;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            // 이미 작성 진행 중인 성적서는 중복 실행 방지
            if (report.getWriteStatus() == AppStatus.PROGRESS) {
                throw new IllegalArgumentException(
                        String.format("이미 성적서작성이 진행 중인 성적서가 포함되어 있습니다. (id: %d)", report.getId()));
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

        // ── TODO: 작업서버 트리거 ──────────────────────────────────────────────
        // 추후 작업서버(WSL2 / 홈서버)가 구축되면 아래를 구현한다.
        // batchId + storageConfig + envInfo 를 JSON으로 묶어 POST 요청.
        // 트리거 실패 시 배치 status를 FAIL로 변경 후 예외 throw.
        // triggerWorkerServer(batchId, user);

        return new ReportJobBatchDTO.CreateBatchRes(batchId, validatedIds.size());
    }
}