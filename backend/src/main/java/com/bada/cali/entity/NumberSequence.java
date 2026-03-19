package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "number_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumberSequence {

    /** 시퀀스 키 (예: order_2026, report_123_ACCREDDIT, manage_ACCREDDIT_2026) */
    @Id
    @Column(name = "seq_key", length = 50)
    private String seqKey;

    /** 다음에 발급할 순번 (1-based) */
    @Column(name = "next_val", nullable = false)
    private int nextVal;

    /** 최종 갱신일시 (DB ON UPDATE CURRENT_TIMESTAMP 자동 관리, 읽기 전용) */
    @Column(name = "update_datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updateDatetime;
}