package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Department;
import com.bada.cali.repository.projection.DepartmentListPr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
	List<DepartmentListPr> getDepartmentList(@Param("isVisible")YnType isVisible);

}
