package com.bada.cali.repository;

import com.bada.cali.entity.BusinessTrip;
import com.bada.cali.repository.projection.BusinessTripCalendarRow;
import com.bada.cali.repository.projection.BusinessTripListRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface BusinessTripRepository extends JpaRepository<BusinessTrip, Long> {

    /**
     * 캘린더 기간 조회 — 지정 기간과 겹치는 출장일정 전체 반환
     * (soft delete 미삭제 건만 조회)
     *
     * 기간 겹침 조건: start < rangeEnd AND end > rangeStart
     */
    @Query(value = """
                SELECT
                    b.id                    AS id,
                    b.type                  AS type,
                    b.title                 AS title,
                    b.start_datetime        AS startDatetime,
                    b.end_datetime          AS endDatetime,
                    b.cust_agent            AS custAgent,
                    b.traveler_ids          AS travelerIds,
                    m.name                  AS updateMemberName
                FROM business_trip b
                LEFT JOIN member m ON m.id = b.update_member_id
                WHERE b.delete_datetime IS NULL
                  AND b.start_datetime < :rangeEnd
                  AND b.end_datetime   > :rangeStart
                ORDER BY b.start_datetime ASC
            """, nativeQuery = true)
    List<BusinessTripCalendarRow> findCalendarEvents(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    /**
     * 지정 기간과 겹치는 다른 출장일정 id 목록 반환 (중복 체크용)
     * - btripId: 수정 시 자기 자신 제외 (신규 등록이면 NULL — IS NULL OR 조건으로 처리)
     * - 겹침 조건: start < rangeEnd AND end > rangeStart
     */
    @Query(value = """
                SELECT b.id
                FROM business_trip b
                WHERE b.delete_datetime IS NULL
                  AND (:btripId IS NULL OR b.id != :btripId)
                  AND b.start_datetime < :endDatetime
                  AND b.end_datetime   > :startDatetime
            """, nativeQuery = true)
    Set<Long> findOverlappingTripIds(
            @Param("btripId") Long btripId,
            @Param("startDatetime") LocalDateTime startDatetime,
            @Param("endDatetime") LocalDateTime endDatetime
    );

    /**
     * 리스트 조회 — 검색 필터 + 날짜 범위 + 페이지네이션
     *
     * 검색 조건:
     *   - searchType IS NULL → 전체 조회 (키워드 없음)
     *   - searchType 지정 + keywordLike → 해당 컬럼 LIKE 검색
     *
     * 출장자 이름(travelerNames):
     *   - traveler_ids('1,5,12') → FIND_IN_SET으로 member 테이블 조인 후 GROUP_CONCAT
     *
     * 첨부파일 건수(fileCnt):
     *   - file_info 테이블 서브쿼리 COUNT (is_visible='y' 조건)
     */
    @Query(value = """
                SELECT
                    b.id                AS id,
                    b.type              AS type,
                    b.start_datetime    AS startDatetime,
                    b.end_datetime      AS endDatetime,
                    b.title             AS title,
                    b.cust_agent        AS custAgent,
                    b.cust_agent_addr   AS custAgentAddr,
                    b.report_agent      AS reportAgent,
                    b.report_agent_addr AS reportAgentAddr,
                    b.cust_manager      AS custManager,
                    b.cust_manager_tel  AS custManagerTel,
                    (
                        SELECT COUNT(*)
                        FROM file_info f
                        WHERE f.ref_table_name = 'business_trip'
                          AND f.ref_table_id   = b.id
                          AND f.is_visible     = 'y'
                    )                   AS fileCnt,
                    (
                        SELECT GROUP_CONCAT(m.name ORDER BY m.id SEPARATOR ', ')
                        FROM member m
                        WHERE b.traveler_ids IS NOT NULL
                          AND b.traveler_ids  != ''
                          AND FIND_IN_SET(m.id, b.traveler_ids) > 0
                    )                   AS travelerNames
                FROM business_trip b
                WHERE b.delete_datetime IS NULL
                  AND (
                       :searchType IS NULL
                       OR (
                           (:searchType = 'title'             AND b.title             LIKE :keywordLike) OR
                           (:searchType = 'custAgent'         AND b.cust_agent        LIKE :keywordLike) OR
                           (:searchType = 'custAgentAddr'     AND b.cust_agent_addr   LIKE :keywordLike) OR
                           (:searchType = 'reportAgent'       AND b.report_agent      LIKE :keywordLike) OR
                           (:searchType = 'reportAgentAddr'   AND b.report_agent_addr LIKE :keywordLike)
                       )
                      )
                  AND (:dateStart IS NULL OR b.end_datetime   >= :dateStart)
                  AND (:dateEnd   IS NULL OR b.start_datetime <= :dateEnd)
                ORDER BY b.end_datetime DESC
                LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<BusinessTripListRow> findList(
            @Param("searchType") String searchType,
            @Param("keywordLike") String keywordLike,
            @Param("dateStart") LocalDateTime dateStart,
            @Param("dateEnd") LocalDateTime dateEnd,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 리스트 전체 건수 조회 (페이지네이션 totalCount 산출용)
     * findList 와 동일한 WHERE 조건 사용
     */
    @Query(value = """
                SELECT COUNT(*)
                FROM business_trip b
                WHERE b.delete_datetime IS NULL
                  AND (
                       :searchType IS NULL
                       OR (
                           (:searchType = 'title'             AND b.title             LIKE :keywordLike) OR
                           (:searchType = 'custAgent'         AND b.cust_agent        LIKE :keywordLike) OR
                           (:searchType = 'custAgentAddr'     AND b.cust_agent_addr   LIKE :keywordLike) OR
                           (:searchType = 'reportAgent'       AND b.report_agent      LIKE :keywordLike) OR
                           (:searchType = 'reportAgentAddr'   AND b.report_agent_addr LIKE :keywordLike)
                       )
                      )
                  AND (:dateStart IS NULL OR b.end_datetime   >= :dateStart)
                  AND (:dateEnd   IS NULL OR b.start_datetime <= :dateEnd)
            """, nativeQuery = true)
    long countList(
            @Param("searchType") String searchType,
            @Param("keywordLike") String keywordLike,
            @Param("dateStart") LocalDateTime dateStart,
            @Param("dateEnd") LocalDateTime dateEnd
    );
}
