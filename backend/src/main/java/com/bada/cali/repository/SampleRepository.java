package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Sample;
import com.bada.cali.repository.projection.SampleListRow;
import com.bada.cali.repository.projection.SampleReportWriteRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {

	// 샘플 목록 검색 (서버 페이징, native SQL)
	@Query(value = """
			SELECT s.id,
			       s.name,
			       s.middle_item_code_id AS middleItemCodeId,
			       mc.code_num           AS middleCodeNum,
			       mc.code_name          AS middleCodeName,
			       s.small_item_code_id  AS smallItemCodeId,
			       sc.code_num           AS smallCodeNum,
			       sc.code_name          AS smallCodeName
			  FROM sample s
			  LEFT JOIN item_code mc ON mc.id = s.middle_item_code_id
			  LEFT JOIN item_code sc ON sc.id = s.small_item_code_id
			 WHERE s.is_visible = 'y'
			   AND (:middleItemCodeId IS NULL OR s.middle_item_code_id = :middleItemCodeId)
			   AND (:smallItemCodeId  IS NULL OR s.small_item_code_id  = :smallItemCodeId)
			   AND (
			         :keyword = ''
			         OR (
			               (:searchType = 'all'            AND (mc.code_num  LIKE CONCAT('%',:keyword,'%')
			                                                 OR mc.code_name LIKE CONCAT('%',:keyword,'%')
			                                                 OR sc.code_num  LIKE CONCAT('%',:keyword,'%')
			                                                 OR sc.code_name LIKE CONCAT('%',:keyword,'%')
			                                                 OR s.name       LIKE CONCAT('%',:keyword,'%')))
			            OR (:searchType = 'middleCodeNum'  AND mc.code_num  LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'middleCodeName' AND mc.code_name LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'smallCodeNum'   AND sc.code_num  LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'smallCodeName'  AND sc.code_name LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'name'           AND s.name       LIKE CONCAT('%',:keyword,'%'))
			         )
			       )
			 ORDER BY mc.code_num ASC, sc.code_num ASC, s.name ASC
			""",
			countQuery = """
					SELECT COUNT(s.id)
					  FROM sample s
					  LEFT JOIN item_code mc ON mc.id = s.middle_item_code_id
					  LEFT JOIN item_code sc ON sc.id = s.small_item_code_id
					 WHERE s.is_visible = 'y'
					   AND (:middleItemCodeId IS NULL OR s.middle_item_code_id = :middleItemCodeId)
					   AND (:smallItemCodeId  IS NULL OR s.small_item_code_id  = :smallItemCodeId)
					   AND (
					         :keyword = ''
					         OR (
					               (:searchType = 'all'            AND (mc.code_num  LIKE CONCAT('%',:keyword,'%')
					                                                 OR mc.code_name LIKE CONCAT('%',:keyword,'%')
					                                                 OR sc.code_num  LIKE CONCAT('%',:keyword,'%')
					                                                 OR sc.code_name LIKE CONCAT('%',:keyword,'%')
					                                                 OR s.name       LIKE CONCAT('%',:keyword,'%')))
					            OR (:searchType = 'middleCodeNum'  AND mc.code_num  LIKE CONCAT('%',:keyword,'%'))
					            OR (:searchType = 'middleCodeName' AND mc.code_name LIKE CONCAT('%',:keyword,'%'))
					            OR (:searchType = 'smallCodeNum'   AND sc.code_num  LIKE CONCAT('%',:keyword,'%'))
					            OR (:searchType = 'smallCodeName'  AND sc.code_name LIKE CONCAT('%',:keyword,'%'))
					            OR (:searchType = 'name'           AND s.name       LIKE CONCAT('%',:keyword,'%'))
					         )
					       )
					""",
			nativeQuery = true)
	Page<SampleListRow> searchSamples(
			@Param("middleItemCodeId") Long middleItemCodeId,
			@Param("smallItemCodeId") Long smallItemCodeId,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			Pageable pageable
	);

	// 성적서작성 모달 — 소분류 기준 샘플 파일 목록 조회 (sample + file_info + member JOIN, 최신순)
	// 성적서작성 모달 — 전체 목록 조회 (페이지네이션 없음, 스크롤 방식)
	@Query(value = """
			SELECT fi.id,
			       s.id           AS sampleId,
			       s.name         AS itemName,
			       fi.origin_name AS fileName,
			       m.name         AS createMemberName,
			       fi.create_datetime AS createDatetime
			  FROM sample s
			  JOIN file_info fi ON fi.ref_table_name = 'sample'
			                   AND fi.ref_table_id   = s.id
			                   AND fi.is_visible     = 'y'
			  JOIN member m ON m.id = fi.create_member_id
			 WHERE s.is_visible        = 'y'
			   AND s.small_item_code_id = :smallItemCodeId
			   AND (
			         :keyword = ''
			         OR (
			               (:searchType = 'all'              AND (s.name        LIKE CONCAT('%',:keyword,'%')
			                                                   OR fi.origin_name LIKE CONCAT('%',:keyword,'%')
			                                                   OR m.name         LIKE CONCAT('%',:keyword,'%')))
			            OR (:searchType = 'itemName'         AND s.name         LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'fileName'         AND fi.origin_name LIKE CONCAT('%',:keyword,'%'))
			            OR (:searchType = 'createMemberName' AND m.name         LIKE CONCAT('%',:keyword,'%'))
			         )
			       )
			 ORDER BY fi.create_datetime DESC
			""",
			nativeQuery = true)
	List<SampleReportWriteRow> searchReportWriteSamples(
			@Param("smallItemCodeId") Long smallItemCodeId,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword
	);

	// 중복 체크 (중분류 + 소분류 + 기기명 + is_visible=y)
	Optional<Sample> findByMiddleItemCodeIdAndSmallItemCodeIdAndNameAndIsVisible(
			Long middleItemCodeId, Long smallItemCodeId, String name, YnType isVisible
	);

	// 삭제 대상 샘플 목록 조회
	List<Sample> findByIdInAndIsVisible(Collection<Long> ids, YnType isVisible);

	// 샘플 소프트 삭제
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Sample s
			   SET s.isVisible      = :isVisible,
			       s.deleteDatetime = :deleteDatetime,
			       s.deleteMemberId = :deleteMemberId
			 WHERE s.id IN :ids
			""")
	int softDeleteByIds(
			@Param("ids") Collection<Long> ids,
			@Param("isVisible") YnType isVisible,
			@Param("deleteDatetime") LocalDateTime deleteDatetime,
			@Param("deleteMemberId") Long deleteMemberId
	);
}
