package com.bada.cali.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 성적서 작업서버 연동 데이터 DTO
 *
 * 워커 서버가 CALI에 요청하는 두 가지 데이터를 담는다.
 *  1. SheetSettingRes   : env.sheet_info_setting — 성적서시트 셀위치/형식 설정
 *  2. ReportFillDataRes : 성적서 엑셀에 삽입할 데이터 (여러 테이블에서 조합)
 *
 * 필드명은 성적서시트 설정의 항목코드(fieldCode)와 1:1 대응한다.
 * 워커는 fieldCode로 셀 위치를 조회하고, 동일한 fieldCode로 삽입 값을 찾아 엑셀에 기록한다.
 */
public class WorkerDataDTO {

    // ── 성적서시트 설정 ────────────────────────────────────────────────────────

    /**
     * 성적서시트 설정의 개별 항목 (항목코드 1개에 대한 설정)
     * env.sheet_info_setting JSON 내 각 항목의 구조와 동일
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetFieldSetting {
        /**
         * 엑셀 셀 주소 (예: "B5", "AA12").
         * null 이면 해당 항목코드는 미사용 — 엑셀 삽입 생략.
         */
        private String cell;

        /**
         * 값 형식 (항목코드에 따라 의미가 다름).
         * 날짜 : "yyyy-mm-dd" / "yyyy. mm. dd" / "yyyy. m. d" / "yyyy-m-d"
         * 이름 : "dense"(붙여쓰기) / "indent"(글자 사이 공백) / "first_indent"(첫 글자 뒤 공백)
         * null 이면 기본 형식 사용.
         */
        private String format;
    }

    /**
     * 성적서시트 전체 설정 응답
     * GET /api/worker/env/sheet-setting
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "성적서시트 셀위치/형식 전체 설정")
    public static class SheetSettingRes {
        /**
         * 항목코드 → 셀 설정 맵
         * 예: { "custAgent": { "cell": "B5", "format": null }, "caliDate": { "cell": "D10", "format": "yyyy. m. d" } }
         */
        @Schema(description = "항목코드 → 셀설정 맵")
        private Map<String, SheetFieldSetting> settings;
    }

    // ── 성적서 엑셀 삽입 데이터 ───────────────────────────────────────────────

    /**
     * 성적서 엑셀 삽입 데이터 응답
     * GET /api/worker/reports/{reportId}/fill-data?sampleId={sampleId}
     *
     * 필드명은 성적서시트 설정(sheet_info_setting)의 항목코드와 1:1 대응.
     * null 값 = 해당 성적서에 데이터 없음 → 워커가 빈 문자열("")로 삽입.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "성적서 엑셀 삽입 데이터")
    public static class ReportFillDataRes {

        // ── 신청업체 (CaliOrder) ─────────────────────────────────────────────
        private String custAgent;
        private String custAgentEn;
        private String custAgentAddr;
        private String custAgentAddrEn;

        // ── 접수/성적서 번호 ────────────────────────────────────────────────
        /** CaliOrder.orderNum */
        private String orderNum;
        /** Report.reportNum */
        private String reportNum;

        // ── 분류코드 (ItemCode) ──────────────────────────────────────────────
        /** 중분류 ItemCode.codeNum */
        private String middleItemCodeNum;
        /** 소분류 ItemCode.codeNum */
        private String smallItemCodeNum;

        // ── 기기 정보 (Report) ───────────────────────────────────────────────
        private String itemName;
        private String itemNameEn;
        /** Report.itemMakeAgent */
        private String makeAgent;
        /** Report.itemMakeAgentEn */
        private String makeAgentEn;
        /** Report.itemFormat. 필드명 충돌 방지를 위해 DTO에서도 'format' 유지 */
        private String format;
        private String itemNum;
        /** 자산번호 — 현재 DB 미존재 → null 반환 */
        private String assetNum;

        // ── 교정 정보 ────────────────────────────────────────────────────────
        /** 교정장소 — Env.addr (교정실험실 주소) */
        private String caliAddress;
        /** CaliOrder.siteAddr */
        private String siteAddr;
        /** CaliOrder.siteAddrEn */
        private String siteAddrEn;
        /** Report.caliDate */
        private LocalDate caliDate;
        /** Report.itemCaliCycle */
        private Integer itemCaliCycle;
        /** 승인일자 — Report.approvalDatetime.toLocalDate() (미결재 시 null) */
        private LocalDate approvalDate;

        // ── 환경 데이터 (Report.environmentInfo JSON 파싱) ──────────────────
        private String tempMin;
        private String tempMax;
        private String humMin;
        private String humMax;
        private String preMin;
        private String preMax;

        // ── 담당자 이름 (Member) ─────────────────────────────────────────────
        /** 실무자 이름 국문 — Member.name */
        private String worker;
        /** 실무자 이름 영문 — Member.nameEng */
        private String workerEn;
        /** 기술책임자 이름 국문 — Member.name */
        private String approval;
        /** 기술책임자 이름 영문 — Member.nameEng */
        private String approvalEn;

        // ── 소급성 문구 (Report.tracestatementInfo JSON 파싱) ────────────────
        private String traceStatement;
        private String traceStatement2;
        private String traceStatement3;
        private String traceStatementEn;
        private String traceStatementEn2;
        private String traceStatementEn3;

        // ── 성적서 언어 설정 (PDF 변환 시 대상 시트 결정에 사용) ─────────────
        // KR: 국문 시트만, EN: 영문 시트만, BOTH: 국문+영문 모두
        @Schema(description = "성적서 언어 설정 (KR / EN / BOTH)")
        private String reportLang;

        // ── 샘플 파일 정보 (file_info — 워커가 스토리지에서 다운로드 시 사용) ─
        // 워커가 S3 objectKey 구성 방법:
        //   if (sampleFileDir.endsWith("/")) -> {storageRootDir}/{sampleFileDir}{sampleFileId}.{sampleFileExtension}
        //   else                             -> {storageRootDir}/{sampleFileDir}/{sampleFileId}.{sampleFileExtension}
        @Schema(description = "샘플 file_info.id (S3 objectKey 구성에 사용)")
        private Long sampleFileId;
        @Schema(description = "샘플 file_info.dir (S3 디렉토리 경로)")
        private String sampleFileDir;
        @Schema(description = "샘플 파일 확장자 (예: xlsx)")
        private String sampleFileExtension;
    }
}
