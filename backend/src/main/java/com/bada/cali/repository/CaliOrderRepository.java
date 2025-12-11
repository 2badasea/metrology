package com.bada.cali.repository;

import com.bada.cali.common.YnType;
import com.bada.cali.entity.CaliOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CaliOrderRepository extends JpaRepository<CaliOrder, Long> {
	
	// 교정접수 데이터 가져오기
	@Query("""
				select o
				from CaliOrder o
				where o.isVisible = :isVisible
					and (:startDateTime IS NULL OR o.orderDatetime >= :startDateTime)
					and (:endDateTime IS NULL OR o.orderDatetime <= :endDateTime)
					and (:isTax IS NULL OR o.isTax = :isTax)
					and (:caliType IS  NULL OR o.caliType = :caliType)
					and (:statusType IS NULL OR o.statusType = :statusType)
			      AND (
			            :keyword = '' OR
			            (
			                (:searchType = 'orderNum'        AND o.orderNum        LIKE %:keyword%)
			             OR (:searchType = 'custAgent'       AND o.custAgent   LIKE %:keyword%)
			             OR (:searchType = 'reportAgent'     AND o.reportAgent LIKE %:keyword%)
			             OR (:searchType = 'reportAgentAddr' AND o.reportAgentAddr LIKE %:keyword%)
			             OR (:searchType = 'remark'          AND o.remark          LIKE %:keyword%)
			             OR (:searchType = 'all' AND (
			                    o.orderNum        LIKE %:keyword%
			                 OR o.custAgent   LIKE %:keyword%
			                 OR o.reportAgent LIKE %:keyword%
			                 OR o.reportAgentAddr LIKE %:keyword%
			                 OR o.remark          LIKE %:keyword%
			             ))
			            )
			      )
			
			""")
	Page<CaliOrder> searchOrders(
			@Param("isVisible") YnType isVisible,
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime,
			@Param("isTax") YnType isTax,
			@Param("caliType") String caliType,
			@Param("statusType") String statusType,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			Pageable pageable
	);
	
	
	// isVisible, startData, endDate, isTax, caliType, statusType, searchType, keyword, pageable);
	
}
