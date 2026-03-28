-- ============================================================
-- v_260328: alarm 테이블 신설
-- 용도: 사용자별 알림 저장 (WORK_COMMENT, WORK_NOTICE, REPORT_REJECT 등)
-- ============================================================

CREATE TABLE IF NOT EXISTS `alarm` (
  `id`              bigint        NOT NULL AUTO_INCREMENT             COMMENT '알림 고유번호',
  `member_id`       bigint        NOT NULL                            COMMENT '수신자 member.id',
  `alarm_type`      varchar(30)   NOT NULL                            COMMENT '알림 유형 (WORK_COMMENT/WORK_NOTICE/REPORT_REJECT/REPORT_SUBMIT/REPORT_REUPLOAD/BOARD_NOTICE)',
  `ref_type`        varchar(30)   DEFAULT NULL                        COMMENT '참조 대상 종류 (WORK/REPORT/BOARD_NOTICE)',
  `ref_id`          bigint        DEFAULT NULL                        COMMENT '참조 대상 ID (ref_type에 따라 다른 테이블의 PK)',
  `content`         varchar(500)  NOT NULL                            COMMENT '알림 내용 (제목 겸 본문 스냅샷)',
  `sender_name`     varchar(50)   DEFAULT NULL                        COMMENT '발신자명 스냅샷 (NULL이면 시스템 발신)',
  `is_read`         char(1)       NOT NULL DEFAULT 'n'                COMMENT '읽음 여부 (y: 읽음, n: 미읽음)',
  `read_datetime`   datetime      DEFAULT NULL                        COMMENT '읽은 일시',
  `create_datetime` datetime      NOT NULL DEFAULT (now())            COMMENT '알림 생성일시',

  PRIMARY KEY (`id`),
  KEY `idx_alarm_member_read` (`member_id`, `is_read`, `create_datetime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자 알림';
