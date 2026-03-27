-- =============================================================================
-- v_260326 — 출장차량(car) 테이블 신규 생성
-- =============================================================================

CREATE TABLE IF NOT EXISTS `car` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT               COMMENT '차량 고유 id',

  -- 기본 정보
  `name`                varchar(100)  NOT NULL                              COMMENT '차량 이름 (예: 1호차)',
  `number`              varchar(20)   NOT NULL                              COMMENT '차량번호 (예: 서울 12가 3456)',
  `brand`               varchar(50)   DEFAULT NULL                          COMMENT '브랜드/제조사 (예: 현대, 기아)',
  `model_year`          smallint      DEFAULT NULL                          COMMENT '년식 (예: 2022)',
  `fuel_type`           varchar(20)   DEFAULT NULL                          COMMENT '연료 종류 (GASOLINE/DIESEL/LPG/ELECTRIC/HYBRID)',

  -- 운영 정보
  `capacity`            tinyint       DEFAULT NULL                          COMMENT '최대 탑승 인원 (출장 인원 배치 참고용)',
  `insurance_company`   varchar(100)  DEFAULT NULL                          COMMENT '보험사명',
  `insurance_expiry`    date          DEFAULT NULL                          COMMENT '보험 만료일',
  `inspection_expiry`   date          DEFAULT NULL                          COMMENT '정기검사 만료일',
  `remark`              text          DEFAULT NULL                          COMMENT '비고',

  -- 표시 여부 (soft delete)
  `is_visible`          char(1)       NOT NULL DEFAULT 'y'                  COMMENT '표시여부 (y: 표시, n: 삭제/숨김)',

  -- 감사 컬럼
  `create_datetime`     datetime      NOT NULL DEFAULT (now())              COMMENT '등록일시',
  `create_member_id`    bigint        NOT NULL                              COMMENT '등록자 member.id',
  `update_datetime`     datetime      DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `update_member_id`    bigint        DEFAULT NULL                          COMMENT '수정자 member.id',
  `delete_datetime`     datetime      DEFAULT NULL                          COMMENT '삭제일시',
  `delete_member_id`    bigint        DEFAULT NULL                          COMMENT '삭제자 member.id',

  PRIMARY KEY (`id`),
  KEY `idx_car_visible` (`is_visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='출장차량 관리';