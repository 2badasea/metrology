package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.StandardEquipmentRef;
import com.bada.cali.repository.projection.ConflictEquipmentRow;
import com.bada.cali.repository.projection.UsedEquipmentListPr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

// 표준장비관리 참조 관련 리파지토리
public interface EquipmentRefRepository extends JpaRepository<StandardEquipmentRef, Long> {
	
	
	@Query("""
        select
            er.equipmentId as equipmentId,
            e.manageNo as manageNo,
            e.name as name,
            e.makeAgent as makeAgent,
            e.serialNo as serialNo
        from StandardEquipmentRef er
        left join StandardEquipment e
            on e.id = er.equipmentId
           and e.isVisible = :isVisible
        where er.refTable = :refTable
          and er.refTableId = :refTableId
        order by er.seq asc
    """)
	Page<UsedEquipmentListPr> getUsedEquipment(
			@Param("refTable") String refTable,
			@Param("refTableId") Long refTableId,
			@Param("isVisible") YnType isVisible,
			Pageable pageable
	);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			DELETE
				FROM StandardEquipmentRef AS ef
			WHERE ef.refTable = :refTable
			AND ef.refTableId = :refTableId
	""")
	void deleteUsedEquipData(
			@Param("refTable") String refTable,
			@Param("refTableId") Long refTableId
	);

	/**
	 * 겹치는 출장일정들에 등록된 장비 중, 검사 대상 장비 ID와 교집합 조회 (중복 체크용)
	 * - tripIds    : 겹치는 출장일정 id 목록 (BusinessTripRepository.findOverlappingTripIds 결과)
	 * - equipmentIds: 현재 모달 그리드에 있는 장비 id 목록
	 * - conflictInfo: "출장제목 (M/d HH:mm~HH:mm)" 형식으로 미리 조합하여 반환
	 */
	@Query(value = """
                SELECT
                    er.equipment_id                                                           AS equipmentId,
                    e.name                                                                    AS name,
                    e.manage_no                                                               AS manageNo,
                    CONCAT(b.title, ' (',
                           DATE_FORMAT(b.start_datetime, '%m/%d %H:%i'), '~',
                           DATE_FORMAT(b.end_datetime,   '%H:%i'), ')')                      AS conflictInfo
                FROM standard_equipment_ref er
                JOIN standard_equipment e  ON e.id  = er.equipment_id AND e.is_visible = 'y'
                JOIN business_trip      b  ON b.id  = er.ref_table_id
                WHERE er.ref_table       = 'business_trip'
                  AND er.ref_table_id   IN :tripIds
                  AND er.equipment_id   IN :equipmentIds
            """, nativeQuery = true)
	List<ConflictEquipmentRow> findConflictEquipments(
			@Param("tripIds") Set<Long> tripIds,
			@Param("equipmentIds") List<Long> equipmentIds
	);


}
