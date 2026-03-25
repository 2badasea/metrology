package com.bada.cali.repository.projection;

/**
 * 출장일정 중복 장비 조회용 Projection
 * - standard_equipment_ref + standard_equipment + business_trip 조인 결과
 * - conflictInfo: "출장제목 (M/d HH:mm~HH:mm)" 포맷으로 미리 조합
 */
public interface ConflictEquipmentRow {

    Long getEquipmentId();      // 중복 장비 id

    String getName();           // 기기명

    String getManageNo();       // 관리번호

    String getConflictInfo();   // "출장제목 (3/25 16:00~17:00)" 형식

}