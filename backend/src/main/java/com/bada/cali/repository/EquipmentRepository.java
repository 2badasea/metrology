package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.StandardEquipment;
import com.bada.cali.repository.projection.EquipmentListPr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<StandardEquipment, Long> {
	
	// NOTE (member join 2회(관리담당 정부), field join 분야정보)
	// 표준장비관리 리스트 조회 (토스트 그리드)
	@Query("""
					select
							e.id AS id,
							e.manageNo AS manageNo,
							e.name AS name,
							e.makeAgent AS makeAgent,
							e.serialNo AS serialNo,
							e.isUse AS isUse,
							e.isDispose AS isDispose,
							e.modelName AS modelName,
							e.installLocation AS installLocation,
							ef.name AS fieldName,
							m1.name AS primaryManager,
							m2.name AS secondaryManager
					FROM StandardEquipment AS e
					LEFT JOIN EquipmentField AS ef ON ef.id = e.equipmentFieldId
					LEFT JOIN Member AS m1 ON m1.id = e.primaryManagerId
					LEFT JOIN Member AS m2  ON m2.id = e.secondaryManagerId
					WHERE e.isVisible = :isVisible
						AND (:isUse IS NULL OR e.isUse = :isUse)
						AND (:isDispose IS NULL OR e.isDispose = :isDispose)
						AND (:equipmentFieldId IS NULL OR e.equipmentFieldId = :equipmentFieldId)
						AND (
							:keyword = '' OR
							(
								(:searchType = 'manageNo' AND e.manageNo LIKE concat('%', :keyword, '%'))
								OR (:searchType = 'name' AND e.name LIKE concat('%', :keyword, '%'))
								OR (:searchType = 'makeAgent' AND e.makeAgent LIKE concat('%', :keyword, '%'))
								OR (:searchType = 'modelName' AND e.modelName LIKE concat('%', :keyword, '%'))
								OR (:searchType = 'all' AND (
															e.name LIKE concat('%', :keyword, '%')
															OR 	e.manageNo LIKE concat('%', :keyword, '%')
															OR 	e.makeAgent LIKE concat('%', :keyword, '%')
															OR 	e.modelName LIKE concat('%', :keyword, '%')
														)
								)
							)
						)
			""")
	Page<EquipmentListPr> getEquipmentList(
			@Param("isVisible") YnType isVisible,
			@Param("isUse") YnType isUse,
			@Param("isDispose") YnType isDispose,
			@Param("equipmentFieldId") Long equipmentFieldId,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			Pageable pageable
	);
	
}
