-- =====================================================================
-- v_260317.sql  |  실무자결재 1차 작업 선행 DB 변경
-- =====================================================================

-- 1. report 테이블: 완료예정일(expect_complete_date) 컬럼 추가
--    - cali_date(교정일자) 바로 뒤에 위치
ALTER TABLE report
    ADD COLUMN `expect_complete_date` DATE NULL COMMENT '완료예정일'
        AFTER `cali_date`;

-- 2. report 테이블: report_status 컬럼 COMMENT에 CANCEL 추가
ALTER TABLE report
    MODIFY COLUMN `report_status` varchar(50)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
        NOT NULL DEFAULT 'WAIT'
        COMMENT '진행상태 (WAIT: 대기, REPAIR: 수리, IMPOSSIBLE: 불가, WORK_RETURN: 실무자반려, APPROV_RETURN: 기술책임자반려, REUPLOAD: 재업로드, COMPLETE: 완료, CANCEL: 취소)';