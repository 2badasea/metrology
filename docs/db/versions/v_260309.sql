-- =============================================================================
-- 버전: v_260309
-- 작성일: 2026-03-09
-- 이전 버전(v_260216) 대비 변경사항:
--   [추가] sample 테이블 신규 생성
--          - 샘플(기기) 관리 기능을 위한 테이블
--          - middle_item_code_id, small_item_code_id 로 item_code 테이블 참조
--          - file_info 테이블이 ref_table_name='sample', ref_table_id=sample.id 로 파일 연결
-- =============================================================================

CREATE TABLE IF NOT EXISTS `sample` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '샘플명(기기명)',
  `middle_item_code_id` bigint DEFAULT NULL COMMENT '중분류코드 (item_code.id, code_level=MIDDLE)',
  `small_item_code_id` bigint DEFAULT NULL COMMENT '소분류코드 (item_code.id, code_level=SMALL)',
  `is_visible` enum('y','n') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'y' COMMENT '삭제 유무 - y: 노출됨(삭제되지 않음), n: 삭제처리(노출되지 않음)',
  `create_datetime` datetime NOT NULL DEFAULT (now()) COMMENT '등록일시',
  `create_member_id` bigint NOT NULL COMMENT '등록자',
  `update_datetime` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `update_member_id` bigint DEFAULT NULL COMMENT '수정자',
  `delete_datetime` datetime DEFAULT NULL COMMENT '삭제일시',
  `delete_member_id` bigint DEFAULT NULL COMMENT '삭제자',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `is_visible` (`is_visible`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='샘플(기기)관리';
