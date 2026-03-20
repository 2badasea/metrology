package com.bada.cali.common.enums;

/**
 * 성적서 작업 개별 항목(report_job_item) 처리 상태
 * - READY    : 처리 대기 중 (배치 생성 시 기본값)
 * - PROGRESS : 작업서버에서 처리 중
 * - SUCCESS  : 처리 성공
 * - FAIL     : 처리 실패 (message 컬럼에 실패 사유 저장)
 * - CANCELED : 취소됨
 */
public enum JobItemStatus {
    READY,
    PROGRESS,
    SUCCESS,
    FAIL,
    CANCELED
}