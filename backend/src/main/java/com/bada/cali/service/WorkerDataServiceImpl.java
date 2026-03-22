package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.WorkerDataDTO;
import com.bada.cali.entity.*;
import com.bada.cali.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 워커 서버용 데이터 조회 서비스
 *
 * 워커가 성적서 엑셀 작성에 필요한 두 종류의 데이터를 제공한다:
 *   1. getSheetSetting()       : Env.sheetInfoSetting JSON → 셀위치/형식 맵
 *   2. getReportFillData(...)  : Report/CaliOrder/Member 등 → 엑셀 삽입 값 조합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerDataServiceImpl {

    private final EnvRepository envRepository;
    private final ReportRepository reportRepository;
    private final CaliOrderRepository caliOrderRepository;
    private final MemberRepository memberRepository;
    private final ItemCodeRepository itemCodeRepository;
    private final FileInfoRepository fileInfoRepository;

    // Spring Boot 자동 구성 ObjectMapper (JavaTimeModule 포함)
    private final ObjectMapper objectMapper;

    // ── 1. 성적서시트 설정 ────────────────────────────────────────────────────

    /**
     * 성적서시트 셀위치/형식 전체 설정 조회
     *
     * Env(id=1).sheetInfoSetting JSON 을 파싱하여 fieldCode → SheetFieldSetting 맵으로 반환.
     * JSON 포맷: { "fieldCode": { "cell": "B5", "format": "yyyy. m. d" }, ... }
     *
     * @return 항목코드별 셀 설정 맵 (sheetInfoSetting 미설정 시 빈 맵 반환)
     */
    @Transactional(readOnly = true)
    public WorkerDataDTO.SheetSettingRes getSheetSetting() {
        Env env = envRepository.findById((byte) 1)
                .orElseThrow(() -> new EntityNotFoundException("Env 설정을 찾을 수 없습니다."));

        // sheetInfoSetting 이 없으면 빈 맵으로 응답 (워커가 빈 설정으로 처리)
        if (env.getSheetInfoSetting() == null || env.getSheetInfoSetting().isBlank()) {
            log.warn("Env.sheetInfoSetting 이 비어있습니다. 빈 설정을 반환합니다.");
            return new WorkerDataDTO.SheetSettingRes(new HashMap<>());
        }

        try {
            Map<String, WorkerDataDTO.SheetFieldSetting> settings = objectMapper.readValue(
                    env.getSheetInfoSetting(),
                    new TypeReference<Map<String, WorkerDataDTO.SheetFieldSetting>>() {}
            );
            return new WorkerDataDTO.SheetSettingRes(settings);
        } catch (Exception e) {
            log.error("sheetInfoSetting JSON 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("성적서시트 설정 파싱에 실패했습니다.", e);
        }
    }

    // ── 2. 성적서 엑셀 삽입 데이터 ───────────────────────────────────────────

    /**
     * 성적서 엑셀 삽입 데이터 조합
     *
     * Report, CaliOrder, Member(실무자/기술책임자), ItemCode(중/소분류), Env, FileInfo 를
     * 조합하여 워커가 엑셀에 직접 삽입하는 데이터를 반환한다.
     *
     * @param reportId  성적서 id
     * @param sampleId  데이터시트(샘플) 파일의 file_info.id
     * @return 엑셀 삽입용 데이터 DTO
     */
    @Transactional(readOnly = true)
    public WorkerDataDTO.ReportFillDataRes getReportFillData(Long reportId, Long sampleId) {

        // ── 1. Report 로드 ────────────────────────────────────────────────────
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report를 찾을 수 없습니다. id=" + reportId));

        // ── 2. CaliOrder 로드 (신청업체·접수번호·소재지 출처) ─────────────────
        CaliOrder order = caliOrderRepository.findById(report.getCaliOrderId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "CaliOrder를 찾을 수 없습니다. id=" + report.getCaliOrderId()));

        // ── 3. 담당자 이름 로드 ───────────────────────────────────────────────
        // 실무자: report.workMemberId
        Member workerMember = report.getWorkMemberId() != null
                ? memberRepository.findById(report.getWorkMemberId()).orElse(null)
                : null;
        // 기술책임자: report.approvalMemberId
        Member approvalMember = report.getApprovalMemberId() != null
                ? memberRepository.findById(report.getApprovalMemberId()).orElse(null)
                : null;

        // ── 4. 분류코드 로드 ──────────────────────────────────────────────────
        // Report 에 middleItemCodeId / smallItemCodeId 가 별도로 저장되므로 직접 조회
        ItemCode middleItemCode = report.getMiddleItemCodeId() != null
                ? itemCodeRepository.findById(report.getMiddleItemCodeId()).orElse(null)
                : null;
        ItemCode smallItemCode = report.getSmallItemCodeId() != null
                ? itemCodeRepository.findById(report.getSmallItemCodeId()).orElse(null)
                : null;

        // ── 5. Env 로드 (교정장소 주소: Env.addr) ─────────────────────────────
        Env env = envRepository.findById((byte) 1).orElse(null);

        // ── 6. 샘플 파일 정보 로드 ───────────────────────────────────────────
        // sampleId = sample 테이블의 PK (file_info.id 가 아님)
        // file_info 에서 ref_table_name='sample', ref_table_id=sampleId, is_visible='y' 로 조회
        List<FileInfo> sampleFiles = fileInfoRepository.findByRefTableNameAndRefTableIdAndIsVisible(
                "sample", sampleId, YnType.y);
        FileInfo sampleFile = sampleFiles.isEmpty() ? null : sampleFiles.get(0);
        if (sampleFile == null) {
            log.warn("샘플 파일을 찾을 수 없습니다. sampleId(sample.id)={}", sampleId);
        }

        // ── 7. 환경정보 JSON 파싱 ─────────────────────────────────────────────
        // 키: tempMin, tempMax, humMin, humMax, preMin, preMax
        Map<String, String> envMap = parseJsonToStringMap(
                report.getEnvironmentInfo(), "environmentInfo");

        // ── 8. 소급성문구 JSON 파싱 ───────────────────────────────────────────
        // 키: traceStatement, traceStatement2, traceStatement3,
        //     traceStatementEn, traceStatementEn2, traceStatementEn3
        Map<String, String> traceMap = parseJsonToStringMap(
                report.getTracestatementInfo(), "tracestatementInfo");

        // ── 9. 최종 DTO 조합 ──────────────────────────────────────────────────
        return WorkerDataDTO.ReportFillDataRes.builder()

                // 신청업체 (CaliOrder)
                .custAgent(order.getCustAgent())
                .custAgentEn(order.getCustAgentEn())
                .custAgentAddr(order.getCustAgentAddr())
                .custAgentAddrEn(order.getCustAgentAddrEn())

                // 접수/성적서 번호
                .orderNum(order.getOrderNum())
                .reportNum(report.getReportNum())

                // 분류코드 (ItemCode.codeNum)
                .middleItemCodeNum(middleItemCode != null ? middleItemCode.getCodeNum() : null)
                .smallItemCodeNum(smallItemCode != null ? smallItemCode.getCodeNum() : null)

                // 기기 정보 (Report)
                .itemName(report.getItemName())
                .itemNameEn(report.getItemNameEn())
                .makeAgent(report.getItemMakeAgent())
                .makeAgentEn(report.getItemMakeAgentEn())
                .format(report.getItemFormat())         // DTO 필드명 'format' = DB itemFormat
                .itemNum(report.getItemNum())
                .assetNum(null)                         // 현재 DB에 없는 필드 — 항상 null

                // 교정 정보
                .caliAddress(env != null ? env.getAddr() : null)  // 교정장소: Env.addr
                .siteAddr(order.getSiteAddr())
                .siteAddrEn(order.getSiteAddrEn())
                .caliDate(report.getCaliDate())
                .itemCaliCycle(report.getItemCaliCycle())
                // 승인일자: 기술책임자 결재일시 → LocalDate 변환 (미결재 시 null)
                .approvalDate(report.getApprovalDatetime() != null
                        ? report.getApprovalDatetime().toLocalDate() : null)

                // 환경 데이터 (environmentInfo JSON)
                .tempMin(envMap.get("tempMin"))
                .tempMax(envMap.get("tempMax"))
                .humMin(envMap.get("humMin"))
                .humMax(envMap.get("humMax"))
                .preMin(envMap.get("preMin"))
                .preMax(envMap.get("preMax"))

                // 담당자 이름 (Member.name / nameEng)
                .worker(workerMember != null ? workerMember.getName() : null)
                .workerEn(workerMember != null ? workerMember.getNameEng() : null)
                .approval(approvalMember != null ? approvalMember.getName() : null)
                .approvalEn(approvalMember != null ? approvalMember.getNameEng() : null)

                // 소급성 문구 (tracestatementInfo JSON)
                .traceStatement(traceMap.get("traceStatement"))
                .traceStatement2(traceMap.get("traceStatement2"))
                .traceStatement3(traceMap.get("traceStatement3"))
                .traceStatementEn(traceMap.get("traceStatementEn"))
                .traceStatementEn2(traceMap.get("traceStatementEn2"))
                .traceStatementEn3(traceMap.get("traceStatementEn3"))

                // 성적서 언어 설정 (PDF 변환 시 대상 시트 결정)
                .reportLang(report.getReportLang() != null ? report.getReportLang().name() : null)

                // 샘플 파일 정보 (file_info — 워커 S3 다운로드용)
                .sampleFileId(sampleFile != null ? sampleFile.getId() : null)
                .sampleFileDir(sampleFile != null ? sampleFile.getDir() : null)
                .sampleFileExtension(sampleFile != null ? sampleFile.getExtension() : null)

                .build();
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * JSON 문자열을 Map&lt;String, String&gt; 으로 파싱.
     * 파싱 실패 또는 null/빈 값이면 빈 맵을 반환하여 서비스 흐름을 유지한다.
     *
     * @param json      파싱할 JSON 문자열
     * @param fieldName 로그 출력용 필드명
     */
    private Map<String, String> parseJsonToStringMap(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("{} JSON 파싱 실패 (빈 맵으로 대체): {}", fieldName, e.getMessage());
            return new HashMap<>();
        }
    }
}
