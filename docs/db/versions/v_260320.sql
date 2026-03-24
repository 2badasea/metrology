-- =============================================================================
-- v_260320: 성적서작성/결재 작업 관리 구조 추가
-- 변경 내용:
--   1. file_info — type 컬럼 추가 (파일 역할 구분: origin / signed)
--   2. report    — write_status 컬럼 추가 (성적서작성 진행상태)
--   3. report_job_batch 테이블 신설 (배치 작업 단위 추적)
--   4. report_job_item  테이블 신설 (배치 내 개별 성적서 작업 추적)
-- =============================================================================


-- 1. file_info 테이블 — type 컬럼 추가
--    성적서 관련 파일 역할 구분용. 그 외 첨부파일은 NULL.
--    origin : 성적서작성 결과 또는 직접 업로드한 원본 엑셀
--    signed : 서명이 삽입된 최신 엑셀/PDF (실무자 or 기술책임자 결재 완료 후)
ALTER TABLE `file_info`
    ADD COLUMN `type` VARCHAR(50) NULL
        COMMENT '파일 역할 구분 (origin: 원본성적서 / signed: 서명삽입본. 성적서 관련에만 사용, 그 외 NULL)'
    AFTER `content_type`;


-- 2. report 테이블 — write_status 컬럼 추가
--    성적서작성(워커 앱) 진행상태. work_status / approval_status 와 동일한 패턴.
--    IDLE     : 미작성 (기본값)
--    PROGRESS : 작업서버에서 처리 중
--    SUCCESS  : 작성 완료 (origin 파일 생성 및 스토리지 저장 완료)
--    FAIL     : 작성 실패
ALTER TABLE `report`
    ADD COLUMN `write_status` VARCHAR(50) NOT NULL DEFAULT 'IDLE'
        COMMENT '성적서작성 진행상태 (IDLE: 미작성, PROGRESS: 작성중, SUCCESS: 작성완료, FAIL: 작성실패)'
    AFTER `approval_status`;


-- 3. report_job_batch 테이블 신설
--    사용자의 버튼 클릭 1번 = 배치 1건.
--    job_type 으로 성적서작성 / 실무자결재 / 기술책임자결재를 구분한다.
CREATE TABLE IF NOT EXISTS `report_job_batch` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT          COMMENT '배치 고유 id',
    `job_type`          VARCHAR(30) NOT NULL
        COMMENT '작업 유형 (WRITE: 성적서작성 / WORK_APPROVAL: 실무자결재 / MANAGER_APPROVAL: 기술책임자결재)',
    `request_member_id` BIGINT      NOT NULL                         COMMENT '작업 요청자 member.id',
    `sample_id`         BIGINT      DEFAULT NULL
        COMMENT 'WRITE 타입에만 사용. 선택한 샘플 sample.id. 그 외 NULL',
    `total_count`       INT         NOT NULL DEFAULT 0               COMMENT '전체 처리 대상 건수',
    `success_count`     INT         NOT NULL DEFAULT 0               COMMENT '성공 건수',
    `fail_count`        INT         NOT NULL DEFAULT 0               COMMENT '실패 건수',
    `status`            VARCHAR(30) NOT NULL DEFAULT 'READY'
        COMMENT '배치 전체 상태 (READY / PROGRESS / SUCCESS / FAIL / CANCEL_REQUESTED / CANCELED)',
    `create_datetime`   DATETIME    NOT NULL DEFAULT (now())         COMMENT '배치 생성일시',
    `start_datetime`    DATETIME    DEFAULT NULL                     COMMENT '작업서버에서 처리를 시작한 시각',
    `end_datetime`      DATETIME    DEFAULT NULL                     COMMENT '배치 전체 처리 완료 시각',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='성적서 작업 배치 — 성적서작성/결재 작업의 묶음 단위 (버튼 클릭 1번 = 1배치)';


-- 4. report_job_item 테이블 신설
--    배치 내 성적서 1건 = item 1건. item 단위 독립 트랜잭션으로 처리.
--    step : 작업서버에서 현재 처리 중인 세부 단계명
--           WRITE          : DOWNLOADING_TEMPLATE / FILLING_DATA / UPLOADING_ORIGIN / DONE
--           WORK_APPROVAL  : LOADING_ORIGIN / APPLYING_SIGN / GENERATING_PDF / UPLOADING / DONE
--           MANAGER_APPROVAL: LOADING_ORIGIN / APPLYING_SIGN / INSERTING_QR / GENERATING_PDF / UPLOADING / DONE
CREATE TABLE IF NOT EXISTS `report_job_item` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '아이템 고유 id',
    `batch_id`       BIGINT      NOT NULL                 COMMENT '소속 배치 id (report_job_batch.id)',
    `report_id`      BIGINT      NOT NULL                 COMMENT '처리 대상 성적서 id (report.id)',
    `status`         VARCHAR(30) NOT NULL DEFAULT 'READY'
        COMMENT '개별 처리 상태 (READY / PROGRESS / SUCCESS / FAIL / CANCELED)',
    `step`           VARCHAR(50) DEFAULT NULL
        COMMENT '현재 처리 단계 상세명 (작업서버가 진행 중 업데이트. job_type별 단계 정의는 claudeReq.md 참조)',
    `message`        TEXT        DEFAULT NULL             COMMENT '실패 사유 또는 처리 메시지',
    `retry_count`    INT         NOT NULL DEFAULT 0       COMMENT '재시도 횟수',
    `start_datetime` DATETIME    DEFAULT NULL             COMMENT '이 아이템 처리 시작 시각',
    `end_datetime`   DATETIME    DEFAULT NULL             COMMENT '이 아이템 처리 완료 시각',
    PRIMARY KEY (`id`),
    KEY `idx_batch_id`  (`batch_id`),
    KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='성적서 작업 개별 항목 — batch 내 성적서 1건 = item 1건, item 단위로 독립 처리';