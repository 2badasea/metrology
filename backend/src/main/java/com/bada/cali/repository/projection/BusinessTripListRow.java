package com.bada.cali.repository.projection;

import java.time.LocalDateTime;

/**
 * 출장일정 리스트 조회용 Projection
 * - 그리드 표시 필드 구성
 * - travelerNames: traveler_ids 기반 GROUP_CONCAT(member.name) 결과
 * - fileCnt: file_info 테이블 COUNT 서브쿼리 결과
 */
public interface BusinessTripListRow {

    Long getId();                       // 출장일정 고유 id

    String getType();                   // 출장유형

    LocalDateTime getStartDatetime();   // 출장시작일시

    LocalDateTime getEndDatetime();     // 출장종료일시

    String getTitle();                  // 일정제목

    String getCustAgent();              // 신청업체

    String getCustAgentAddr();          // 신청업체 주소

    String getReportAgent();            // 성적서발행처

    String getReportAgentAddr();        // 성적서발행처 주소

    String getCustManager();            // 담당자(신청업체)

    String getCustManagerTel();         // 담당자 연락처

    Long getFileCnt();                  // 첨부파일 건수 (서브쿼리 COUNT)

    String getTravelerNames();          // 출장자 이름 목록 (GROUP_CONCAT, ', ' 구분)
}
