package com.bada.cali.repository.projection;

/**
 * caliOrder 리스트 조회 시, 접수별 성적서 개수를 담는 프로젝션 인터페이스.
 * ReportRepository.countByCaliOrderIds() 쿼리의 반환 타입으로 사용.
 */
public interface ReportCountRow {

    /** 교정접수 ID */
    Long getCaliOrderId();

    /** 해당 접수에 속한 성적서 개수 (삭제 제외, 최상위 성적서만) */
    Long getReportCnt();
}