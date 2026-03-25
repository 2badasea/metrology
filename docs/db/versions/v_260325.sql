-- =============================================================================
-- v_260325: 출장일정(business_trip) 테이블 신설
-- =============================================================================

CREATE TABLE IF NOT EXISTS `business_trip` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT               COMMENT '출장일정 고유 id',
  `type`                  varchar(50)   DEFAULT NULL                          COMMENT '출장유형 (추후 코드 정의 예정)',
  `title`                 varchar(200)  NOT NULL                              COMMENT '출장제목',
  `start_datetime`        datetime      NOT NULL                              COMMENT '출장시작일시',
  `end_datetime`          datetime      NOT NULL                              COMMENT '출장종료일시',

  -- 신청업체 정보
  `cust_agent_id`         bigint        DEFAULT NULL                          COMMENT '신청업체 agent.id (업체 조회 후 선택값)',
  `cust_agent`            varchar(200)  DEFAULT NULL                          COMMENT '신청업체명',
  `cust_agent_addr`       varchar(300)  DEFAULT NULL                          COMMENT '신청업체 주소',
  `cust_manager`          varchar(100)  DEFAULT NULL                          COMMENT '신청업체 담당자명',
  `cust_manager_tel`      varchar(50)   DEFAULT NULL                          COMMENT '신청업체 담당자 연락처',
  `cust_manager_email`    varchar(125)  DEFAULT NULL                          COMMENT '신청업체 담당자 이메일',

  -- 성적서발행처 정보
  `report_agent_id`       bigint        DEFAULT NULL                          COMMENT '성적서발행처 agent.id (업체 조회 후 선택값)',
  `report_agent`          varchar(200)  DEFAULT NULL                          COMMENT '성적서발행처명',
  `report_agent_addr`     varchar(300)  DEFAULT NULL                          COMMENT '성적서발행처 주소',
  `report_manager`        varchar(100)  DEFAULT NULL                          COMMENT '성적서발행처 담당자명',
  `report_manager_tel`    varchar(50)   DEFAULT NULL                          COMMENT '성적서발행처 담당자 연락처',

  -- 소재지 정보
  `site_addr`             varchar(300)  DEFAULT NULL                          COMMENT '소재지 주소',
  `site_manager`          varchar(100)  DEFAULT NULL                          COMMENT '소재지 담당자명',
  `site_manager_tel`      varchar(50)   DEFAULT NULL                          COMMENT '소재지 담당자 연락처',
  `site_manager_email`    varchar(125)  DEFAULT NULL                          COMMENT '소재지 담당자 이메일',

  -- 출장자 / 출장차량 (콤마 구분 id 문자열)
  `traveler_ids`          varchar(500)  DEFAULT NULL                          COMMENT '출장자 member.id 목록 (콤마 구분, 예: "1,5,12")',
  `car_ids`               varchar(255)  DEFAULT NULL                          COMMENT '출장차량 car.id 목록 (콤마 구분, car 테이블 미구현 - 추후 연동)',

  `remark`                text          DEFAULT NULL                          COMMENT '비고',

  -- 감사 컬럼
  `create_datetime`       datetime      NOT NULL DEFAULT (now())              COMMENT '등록일시',
  `create_member_id`      bigint        NOT NULL                              COMMENT '등록자 member.id',
  `update_datetime`       datetime      DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `update_member_id`      bigint        DEFAULT NULL                          COMMENT '수정자 member.id',
  `delete_datetime`       datetime      DEFAULT NULL                          COMMENT '삭제일시',
  `delete_member_id`      bigint        DEFAULT NULL                          COMMENT '삭제자 member.id',

  PRIMARY KEY (`id`),
  KEY `idx_btrip_period`  (`start_datetime`, `end_datetime`),
  KEY `idx_btrip_create`  (`create_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='출장일정 관리';
