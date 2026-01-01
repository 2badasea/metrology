package com.bada.cali.repository;

import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.entity.ItemCode;
import com.bada.cali.repository.projection.ItemCodeList;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
			@Param("codeLevel") CodeLevel codeLevel,
			Pageable pageable
	);
}
