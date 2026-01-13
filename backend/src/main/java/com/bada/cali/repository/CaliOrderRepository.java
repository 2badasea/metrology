package com.bada.cali.repository;

import com.bada.cali.common.enums.CaliType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.CaliOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaliOrderRepository extends JpaRepository<CaliOrder, Long> {
	
	// 교정접수 데이터 가져오기
	@Query("""
				select o
				from CaliOrder o
				where o.isVisible = :isVisible
					and (:startDate IS NULL OR o.orderDate >= :startDate)
					and (:endDate IS NULL OR o.orderDate <= :endDate)
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
				ORDER BY o.orderDate DESC, o.id DESC
			""")
	Page<CaliOrder> searchOrders(
			@Param("isVisible") YnType isVisible,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("isTax") YnType isTax,
			@Param("caliType") CaliType caliType,
			@Param("statusType") String statusType,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			Pageable pageable
	);
	
	// 파라미터로 넘어온 연도를 기준으로 그해 마지막 접수번호를 가져온다.
	@Query("""
				select
					o
				from CaliOrder as o
				where o.isVisible = 'y'
					and o.orderNum like concat(:orderPrefix,																								 '%')
				order by o.id DESC
			""")
	List<CaliOrder> getLastOrderByYear(@Param("orderPrefix") String orderPrefix, Pageable pageable);
	
	
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				UPDATE CaliOrder AS o
					SET o.isTax = :isTax,
						o.updateDatetime = :now,
						o.updateMemberId = :userId
					WHERE o.id = :caliOrderId
			""")
	int updateIsTax(
			@Param("caliOrderId") Long caliOrderId,
			@Param("isTax") YnType isTax,
			@Param("now")LocalDateTime now,
			@Param("userId") Long userId
	);
	
	
}
