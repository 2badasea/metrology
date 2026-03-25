package com.bada.cali.repository;

import com.bada.cali.entity.BusinessTrip;
import com.bada.cali.repository.projection.BusinessTripCalendarRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
