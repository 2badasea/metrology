package com.bada.cali.repository;

import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.ItemCode;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.repository.projection.ItemCodeRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemCodeRepository extends JpaRepository<ItemCode, Long> {
	
	
	@Query("""
			select
				    ic.id as id,
			     ic.parentId as parentId,
			     ic.codeLevel as codeLevel,
			     ic.codeNum as codeNum,
			     ic.codeName as codeName,
			     ic.codeNameEn as codeNameEn,
			     ic.caliCycleUnit as caliCycleUnit,
			     ic.stdCali as stdCali,
			     ic.preCali as preCali,
			     ic.tracestatementInfo as tracestatementInfo,
			     ic.isKolasStandard as isKolasStandard,
			     ic.isVisible as isVisible
					from ItemCode as ic
			where ic.isVisible =  com.bada.cali.common.enums.YnType.y
						and (:parentId IS NULL OR ic.parentId = :parentId)
						and (:codeLevel IS NULL OR ic.codeLevel = :codeLevel)
			ORDER BY ic.id ASC
			""")
	List<ItemCodeList> searchItemCodeList(
			@Param("parentId") Long parentId,
			@Param("codeLevel") CodeLevel codeLevel
	);
	
	// 삭제할 대상 분류코들르 참조하는 하위 분류코드를 조회한다.
	List<ItemCode> findItemCodeByParentIdInAndCodeLevelAndIsVisibleOrderByParentIdAscIdAsc(List<Long> ids, CodeLevel childCodeLevel, YnType ynType);
	
	
	@Query(value = """
			    WITH RECURSIVE tree AS (
			        SELECT
			            ic.id                       AS id,
			            ic.parent_id                AS parentId,
			            ic.code_level               AS codeLevel,
			            ic.code_num                 AS codeNum,
			            ic.code_name                AS codeName,
			            ic.code_name_en             AS codeNameEn,
			            ic.cali_cycle_unit          AS caliCycleUnit,
			            ic.std_cali                 AS stdCali,
			            ic.pre_cali                 AS preCali,
			            ic.tracestatement_info      AS tracestatementInfo,
			            ic.is_visible               AS isVisible
			        FROM item_code ic
			        WHERE ic.id IN (:ids)
			          AND ic.is_visible = 'y'
			
			        UNION ALL
			
			        SELECT
			            c.id                        AS id,
			            c.parent_id                 AS parentId,
			            c.code_level                AS codeLevel,
			            c.code_num                  AS codeNum,
			            c.code_name                 AS codeName,
			            c.code_name_en              AS codeNameEn,
			            c.cali_cycle_unit           AS caliCycleUnit,
			            c.std_cali                  AS stdCali,
			            c.pre_cali                  AS preCali,
			            c.tracestatement_info       AS tracestatementInfo,
			            c.is_visible                AS isVisible
			        FROM item_code c
			        JOIN tree t ON t.id = c.parent_id
			        WHERE c.is_visible = 'y'
			    )
			    SELECT DISTINCT
			        id, parentId, codeLevel, codeNum, codeName, codeNameEn,
			        caliCycleUnit, stdCali, preCali, tracestatementInfo, isVisible
			    FROM tree
			""", nativeQuery = true)
	List<ItemCodeRow> getDeleteItemCode(@Param("ids") List<Long> ids);
	
	// 삭제
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				update ItemCode ic
					set ic.isVisible = :isVisible,
						ic.deleteDatetime = :deleteDatetime,
						ic.deleteMemberId = :deleteMemberId
				where ic.id in :ids
			""")
	int deleteItemCode(
			@Param("ids") List<Long> ids,
			@Param("isVisible") YnType isVisible,
			@Param("deleteDatetime") LocalDateTime deleteDatetime,
			@Param("deleteMemberId") Long deleteMemberId
	);
	
}
