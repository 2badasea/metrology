package com.bada.cali.repository.projection;

import java.time.LocalDateTime;

/**
 * 출장일정 캘린더 조회용 Projection
 * - FullCalendar 이벤트 렌더링에 필요한 최소 필드
 * - traveler_ids는 문자열로 반환 → JS에서 파싱하여 이름 조회
 */
public interface BusinessTripCalendarRow {

    Long getId();                       // 출장일정 고유 id

    String getType();                   // 출장유형

    String getTitle();                  // 출장제목

    LocalDateTime getStartDatetime();   // 출장시작일시

    LocalDateTime getEndDatetime();     // 출장종료일시

    String getCustAgent();              // 신청업체명

    String getTravelerIds();            // 출장자 id 목록 (콤마 구분 문자열)

    String getUpdateMemberName();       // 최종수정자 이름 (member JOIN)
}
