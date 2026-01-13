package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.StandardEquipmentRef;
import com.bada.cali.repository.projection.UsedEquipmentListPr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
	

}
