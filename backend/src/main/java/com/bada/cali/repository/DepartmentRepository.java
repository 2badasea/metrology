package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Department;
import com.bada.cali.repository.projection.DepartmentListPr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
	
	@Query("""
				SELECT
						d.id AS id,
						d.name AS name,
						d.seq AS seq
				FROM Department AS d
				WHERE d.isVisible = :isVisible
				ORDER BY d.seq ASC
			""")
	List<DepartmentListPr> getDepartmentList(@Param("isVisible") YnType isVisible);
	
	// 삭제처리
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				update Department as d
					set d.isVisible = :isVisible,
						d.deleteMemberId = :userId,
						d.deleteDatetime = :now
					where d.id IN :deleteIds
			""")
	int deleteIds(
			@Param("isVisible") YnType isVisible,
			@Param("userId") Long userId,
			@Param("now") LocalDateTime now,
			@Param("deleteIds") List<Long> deleteIds);
	
}
