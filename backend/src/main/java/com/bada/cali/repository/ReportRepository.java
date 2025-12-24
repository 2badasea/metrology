package com.bada.cali.repository;

import com.bada.cali.entity.Report;
import com.bada.cali.repository.projection.LastManageNoByType;
import com.bada.cali.repository.projection.LastReportNumByOrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
	
	// 접수구분별 가장 마지막 성적서번호 호출
	@Query(value = """
			SELECT x.order_type AS orderType, x.report_num AS reportNum
			FROM (
			  SELECT
			    r.order_type,
			    r.report_num,
			    ROW_NUMBER() OVER (PARTITION BY r.order_type ORDER BY r.id DESC) AS rn
			  FROM report r
			  WHERE r.cali_order_id = :caliOrderId
			    AND r.is_visible = 'y'
			    AND r.parent_id IS NULL
			    AND r.parent_scale_id IS NULL
			) x
			WHERE x.rn = 1
			""", nativeQuery = true)
	List<LastReportNumByOrderType> findLastReportNumsByOrderType(@Param("caliOrderId") Long caliOrderId);
	
	// 접수구분별 가장 마지막 관리번호를 가져온다.
	@Query(value = """
					SELECT x.order_type AS orderType, x.manage_no AS manageNo
						FROM (
							    SELECT r.order_type,
									r.manage_no,
										ROW_NUMBER() over (PARTITION BY r.order_type ORDER BY r.id DESC) as rn
								FROM report AS r
									LEFT JOIN cali_order o ON o.id = r.cali_order_id
								WHERE r.is_visible = 'y'
									AND YEAR(o.order_date) = :year
									AND r.parent_id IS NULL
										AND r.parent_scale_id IS NULL
			
							) x
							where x.rn = 1;
			""", nativeQuery = true)
	List<LastManageNoByType> findLastManageNoByOrderType(@Param("year") int year);
}
