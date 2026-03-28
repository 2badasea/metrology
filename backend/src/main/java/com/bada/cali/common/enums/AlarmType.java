package com.bada.cali.common.enums;

/**
 * 알림 유형
 * - WORK_COMMENT   : 개발팀 문의에 댓글 등록 (대시보드 → cali webhook)
 * - WORK_NOTICE    : 개발팀 공지사항 등록
 * - REPORT_REJECT  : 기술책임자가 성적서를 반려
 * - REPORT_SUBMIT  : 실무자가 성적서를 결재 상신
 * - REPORT_REUPLOAD: 실무자가 성적서를 재업로드
 * - BOARD_NOTICE   : 사내 공지 (추후 확장용)
 */
public enum AlarmType {
    WORK_COMMENT,
    WORK_NOTICE,
    REPORT_REJECT,
    REPORT_SUBMIT,
    REPORT_REUPLOAD,
    BOARD_NOTICE
}
