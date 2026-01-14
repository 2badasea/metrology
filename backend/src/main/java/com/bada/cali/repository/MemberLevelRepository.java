package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.MemberLevel;
import com.bada.cali.repository.projection.MemberLevelListPr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberLevelRepository extends JpaRepository<MemberLevel, Long> {
	
	@Query("""
		SELECT
				ml.id AS id,
				ml.name AS name,
				ml.seq AS seq
		FROM MemberLevel AS ml
		WHERE ml.isVisible = :isVisible
		ORDER BY ml.seq ASC
	""")
	List<MemberLevelListPr> getMemberLevelList(@Param("isVisible") YnType isVisible);
}
