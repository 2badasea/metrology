package com.bada.cali.repository;

import com.bada.cali.common.enums.AlarmType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Alarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // 미읽음 카운트
    long countByMemberIdAndIsRead(Long memberId, YnType isRead);

    // 특정 회원의 알림 목록 (최신순) — 전체
    Page<Alarm> findByMemberIdOrderByCreateDatetimeDesc(Long memberId, Pageable pageable);

    // 특정 회원의 알림 목록 (최신순) — 유형 필터
    Page<Alarm> findByMemberIdAndAlarmTypeOrderByCreateDatetimeDesc(Long memberId, AlarmType alarmType, Pageable pageable);

    // 개별 읽음 처리
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = com.bada.cali.common.enums.YnType.y, a.readDatetime = :now " +
           "WHERE a.id = :id AND a.memberId = :memberId AND a.isRead = com.bada.cali.common.enums.YnType.n")
    int markReadById(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // 전체 읽음 처리
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = com.bada.cali.common.enums.YnType.y, a.readDatetime = :now " +
           "WHERE a.memberId = :memberId AND a.isRead = com.bada.cali.common.enums.YnType.n")
    int markReadAll(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // ref 기준 일괄 읽음 처리 (개발팀 문의 상세 진입 시 해당 work 알림 자동 읽음)
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = com.bada.cali.common.enums.YnType.y, a.readDatetime = :now " +
           "WHERE a.memberId = :memberId AND a.refType = :refType AND a.refId = :refId " +
           "AND a.isRead = com.bada.cali.common.enums.YnType.n")
    int markReadByRef(@Param("memberId") Long memberId, @Param("refType") String refType,
                      @Param("refId") Long refId, @Param("now") LocalDateTime now);

    // 배치 삭제: 읽은 알림 중 create_datetime이 cutoffRead 이전인 것
    @Modifying
    @Query("DELETE FROM Alarm a WHERE a.isRead = com.bada.cali.common.enums.YnType.y " +
           "AND a.createDatetime < :cutoffRead")
    int deleteReadAlarmsBefore(@Param("cutoffRead") LocalDateTime cutoffRead);

    // 배치 삭제: 미읽음 알림 중 create_datetime이 cutoffUnread 이전인 것
    @Modifying
    @Query("DELETE FROM Alarm a WHERE a.isRead = com.bada.cali.common.enums.YnType.n " +
           "AND a.createDatetime < :cutoffUnread")
    int deleteUnreadAlarmsBefore(@Param("cutoffUnread") LocalDateTime cutoffUnread);
}
