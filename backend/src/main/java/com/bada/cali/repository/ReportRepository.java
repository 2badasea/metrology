package com.bada.cali.repository;

import com.bada.cali.common.enums.OrderType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.entity.Report;
import com.bada.cali.repository.projection.LastManageNoByType;
import com.bada.cali.repository.projection.LastReportNumByOrderType;
import com.bada.cali.repository.projection.OrderDetailsList;
import org.springframework.data.domain.Pageable;
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
	
	
	// 접수상세내역 리스트 (인터페이스 프로젝션으로 가져옴)
	@Query("""
			    select
						r.id as id,
						r.reportType as reportType,
						r.orderType as orderType,
			
						r.middleItemCodeId as middleItemCodeId,
						r.smallItemCodeId as smallItemCodeId,
			
						r.reportNum as reportNum,
						r.itemName as itemName,
						r.itemFormat as itemFormat,
						r.itemNum as itemNum,
			
						r.itemCaliCycle as itemCaliCycle,
						r.itemMakeAgent as itemMakeAgent,
						r.manageNo as manageNo,
			
						r.reportStatus as reportStatus,
						r.remark as remark,
						r.caliFee as caliFee,
						r.statusRemark as statusRemark,
			
						r.approvalDatetime as approvalDatetime,
						r.workDatetime as workDatetime,
						
						r.workStatus as workStatus,
						r.approvalStatus as approvalStatus
			    	from Report r
			    	where r.isVisible = 'y'
			    	and r.parentId IS NULL
			    	and r.parentScaleId IS NULL
					and r.caliOrderId = :caliOrderId
			    	and (:orderType IS NULL OR r.orderType = :orderType)
			    	and (
			    		:keyword = '' OR
			    		(
			    			(:searchType = 'reportNum' AND r.reportNum LIKE %:keyword%)
			    			OR (:searchType = 'manageNo' AND r.manageNo LIKE %:keyword%)
			    			OR (:searchType = 'itemName' AND r.itemName LIKE %:keyword%)
			    			OR (:searchType = 'itemMakeAgent' AND r.itemMakeAgent LIKE %:keyword%)
			    			OR (:searchType = 'itemFormat' AND r.itemFormat LIKE %:keyword%)
			    			OR (:searchType = 'itemNum' AND r.itemNum LIKE %:keyword%)
			    			OR (:searchType = 'all' AND (
														    			r.reportNum LIKE %:keyword%
														    			OR r.manageNo LIKE %:keyword%
														    			OR r.itemName LIKE %:keyword%
														    			OR r.itemMakeAgent LIKE %:keyword%
														    			OR r.itemFormat LIKE %:keyword%
														    			OR r.itemNum LIKE %:keyword%
			    									)
			    			)
			    		)
			    	)
			    	AND (:statusType IS NULL OR (
			    			(:statusType = 'cancel' AND r.reportStatus = 'CANCEL')
			    			OR  (:statusType = 'impossible' AND r.reportStatus = 'IMPOSSIBLE')
			    			OR  (:statusType = 'return' AND (r.reportStatus = 'WORK_RETURN' OR r.reportStatus = 'APPROV_RETURN')
			    			)
			    			OR  (:statusType = 'wait' AND r.workDatetime IS NULL AND r.approvalDatetime IS NULL)
			    			OR (:statusType = 'progress' AND r.workDatetime IS NOT NULL AND r.approvalDatetime IS NULL)
			    			OR (:statusType = 'success' AND r.workDatetime IS NOT NULL AND r.approvalDatetime IS NOT NULL)
			    		)
			    	)
					ORDER BY
						case
							 when r.orderType = com.bada.cali.common.enums.OrderType.ACCREDDIT then 0
							 when r.orderType = com.bada.cali.common.enums.OrderType.UNACCREDDIT then 1
							 when r.orderType = com.bada.cali.common.enums.OrderType.TESTING then 2
			            	else 9
			         	end asc,
			         	r.id asc
			""")
	List<OrderDetailsList> searchOrderDetails(
			@Param("orderType") OrderType orderType,
			@Param("statusType") String statusType,	// 진행전체, 대기, 취소, 불가, 반려, 진행중, 완료
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			@Param("caliOrderId") Long caliOrderId,
			Pageable pageable
			);
	
	
	/**
	 * @param orderType
	 * @param isAgcy
	 * @param reportIds
	 * @param pageable
	 * @return
	 */
	@Query("""
			SELECT r
			FROM Report r
			WHERE r.isVisible = 'y'
			AND r.parentId IS NULL
			AND r.parentScaleId IS NULL
			AND (
				(:isAgcy = true AND r.id IN :reportIds) OR (:isAgcy = false AND r.orderType = :orderType)
			)
			ORDER BY r.id DESC
	""")
	List<Report> getDeleteCheckReport(
			@Param("orderType") OrderType orderType,
			@Param("isAgcy") boolean isAgcy,
			@Param("reportIds") List<Long> reportIds,
			Pageable pageable
	);
	
	// 삭제대상 id를 가진 성적서와 이 성적서를 바라보는 재발행, 자식성적서를 모두 가져온다.
	List<Report> findByIdInOrParentIdInOrParentScaleIdIn(List<Long> deleteIds, List<Long> deleteIds1, List<Long> deleteIds2);
	
	/**
	 * 성적서 수정 모달 내 데이터 조회 (접수의 데이터도 조인하여 가져온다)
	 * @param id
	 * @return
	 */
	@Query("""
				select new com.bada.cali.dto.ReportDTO$ReportInfo(
					 r.id,
					 r.orderType,
					 r.caliDate,
					 r.reportLang,
					 r.priorityType,
					 r.caliType,
					 r.caliTakeType,
					 r.manageNo,
					
					 r.middleItemCodeId,
					 r.smallItemCodeId,
					 r.workMemberId,
					 r.approvalMemberId,
					
					 r.itemId,
					 r.itemName,
					 r.itemNameEn,
					 r.itemFormat,
					 r.itemNum,
					 r.itemCaliCycle,
					 r.itemMakeAgent,
					 r.itemMakeAgentEn,
					 r.remark,
					 r.caliFee,
					 r.additionalFee,
					 r.additionalFeeCause,
					 r.request,
					 r.environmentInfo,
					 r.tracestatementInfo,
					
					 o.custAgent,
					 o.custAgentAddr,
					 o.custManager,
					 o.custManagerTel,
					 o.reportAgent,
					 o.reportAgentEn,
					 o.reportAgentAddr,
					 o.reportManager,
					 o.reportManagerTel
				)
				from Report r
				JOIN CaliOrder o ON o.id = r.caliOrderId
				where r.id = :id
							and r.isVisible = 'y'
			""")
	ReportDTO.ReportInfo getReportInfo(@Param("id") Long id);
	
	@Query("""
			select new com.bada.cali.dto.ReportDTO$ChildReportInfo(
				    r.id,
					r.middleItemCodeId,
					r.smallItemCodeId,
					r.itemId,
					r.itemName,
					r.itemNameEn,
					r.itemFormat,
					r.itemNum,
					r.itemCaliCycle,
					r.itemMakeAgent,
					r.itemMakeAgentEn,
					r.remark,
					r.caliFee,
					r.additionalFee,
					r.additionalFeeCause
			)
			from Report  r
			where r.parentScaleId = :parentScaleId
				and r.isVisible = 'y'
			ORDER BY r.id ASC
	""")
	List<ReportDTO.ChildReportInfo> getChildReport(@Param("parentScaleId") Long parentScaleId);
	
	
	List<Report> findByMiddleItemCodeIdInAndIsVisible(List<Long> attr0, YnType isVisible);
}
