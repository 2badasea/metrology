package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.MemberLevel;
import com.bada.cali.repository.projection.MemberLevelListPr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				update MemberLevel as ml
					set ml.isVisible = :isVisible,
						ml.deleteMemberId = :userId,
						ml.deleteDatetime = :now
					where ml.id IN :deleteIds
			""")
	int deleteIds(
			@Param("isVisible") YnType isVisible,
			@Param("userId") Long userId,
			@Param("now") LocalDateTime now,
			@Param("deleteIds") List<Long> deleteIds);
}
