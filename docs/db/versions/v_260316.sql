-- =============================================================================
-- 버전: v_260316
-- 작성일: 2026-03-16
-- 이전 버전(v_260310) 대비 변경사항:
--   [추가] number_sequence 테이블 신규 생성
--          - 접수번호 / 성적서번호 / 관리번호 채번 시 동시성 제어용 시퀀스 테이블
--          - seq_key 별로 next_val을 관리하며, SELECT FOR UPDATE 방식으로 원자적 채번
--          - seq_key 예시:
--              order_2026          → 2026년 접수번호 시퀀스
--              report_ACCREDDIT_2026   → 2026년 공인 성적서번호 시퀀스
--              report_UNACCREDDIT_2026 → 2026년 비공인 성적서번호 시퀀스
--              report_TESTING_2026     → 2026년 시험 성적서번호 시퀀스
--              manage_ACCREDDIT_2026   → 2026년 공인 관리번호 시퀀스
--              manage_UNACCREDDIT_2026 → 2026년 비공인 관리번호 시퀀스
--              manage_TESTING_2026     → 2026년 시험 관리번호 시퀀스
-- =============================================================================

CREATE TABLE IF NOT EXISTS `number_sequence` (
  `seq_key`    varchar(50)  NOT NULL COMMENT '시퀀스 키 (예: order_2026, report_ACCREDDIT_2026)',
  `next_val`   int          NOT NULL DEFAULT 1 COMMENT '다음에 발급할 순번 (1-based)',
  `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 갱신일시',
  PRIMARY KEY (`seq_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='채번 동시성 제어용 시퀀스 테이블';
