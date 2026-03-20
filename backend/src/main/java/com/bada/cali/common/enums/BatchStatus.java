package com.bada.cali.common.enums;

/**
 * 성적서 작업 배치(report_job_batch) 전체 상태
 * - READY            : 배치가 생성되어 작업서버 트리거 대기 중
 * - PROGRESS         : 작업서버에서 하나 이상의 item을 처리 중
 * - SUCCESS          : 모든 item 처리 완료 (전체 성공)
 * - FAIL             : 처리 완료 후 하나 이상의 item이 실패
 * - CANCEL_REQUESTED : 사용자가 취소 요청 (PROGRESS 중 요청 가능)
 * - CANCELED         : 취소 완료
 */
public enum BatchStatus {
    READY,
    PROGRESS,
    SUCCESS,
    FAIL,
    CANCEL_REQUESTED,
    CANCELED
}