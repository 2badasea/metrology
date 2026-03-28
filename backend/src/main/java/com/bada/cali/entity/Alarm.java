package com.bada.cali.entity;

import com.bada.cali.common.enums.AlarmType;
import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// NOTE DDL 자동생성(ddl-auto) 기능은 꺼져있지만, 문서/메타데이터 역할을 위해 명시
@Entity
@Table(
        name = "alarm",
        indexes = {
                @Index(name = "idx_alarm_member_read", columnList = "member_id, is_read, create_datetime")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm {

    // 알림 고유번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수신자 member.id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 알림 유형
    @Column(name = "alarm_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    // 참조 대상 종류 (WORK / REPORT / BOARD_NOTICE) — nullable
    @Column(name = "ref_type", length = 30)
    private String refType;

    // 참조 대상 ID (ref_type에 따라 다른 테이블의 PK) — nullable
    @Column(name = "ref_id")
    private Long refId;

    // 알림 내용 (제목 겸 본문 스냅샷)
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    // 발신자명 스냅샷 (NULL이면 시스템 발신)
    @Column(name = "sender_name", length = 50)
    private String senderName;

    // 읽음 여부 (y: 읽음, n: 미읽음)
    @Column(name = "is_read", nullable = false, length = 1, columnDefinition = "CHAR(1) DEFAULT 'n'")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private YnType isRead = YnType.n;

    // 읽은 일시
    @Column(name = "read_datetime")
    private LocalDateTime readDatetime;

    // 알림 생성일시
    @Column(name = "create_datetime", nullable = false,
            columnDefinition = "DATETIME NOT NULL DEFAULT (now())")
    @Builder.Default
    private LocalDateTime createDatetime = LocalDateTime.now();
}
