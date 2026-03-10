-- =============================================================================
-- 버전: v_260310
-- 작성일: 2026-03-10
-- 이전 버전(v_260309) 대비 변경사항:
--   [추가] env 테이블 신규 생성
--          - 회사 기본정보 단일 row 관리 (id=1 고정)
--          - 텍스트 정보: 회사명, 대표자, 연락처, 주소, 계좌 등
--          - 이미지 정보: kolas, ilac, company (오브젝트 스토리지 경로 저장)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `env` (
  `id` tinyint NOT NULL COMMENT '고정 row 키 (항상 1, 단일 row 운영)',
  `name` varchar(100) DEFAULT NULL COMMENT '회사명',
  `name_en` varchar(100) DEFAULT NULL COMMENT '회사명(영문)',
  `ceo` varchar(50) DEFAULT NULL COMMENT '대표자',
  `tel` varchar(30) DEFAULT NULL COMMENT '전화번호',
  `fax` varchar(30) DEFAULT NULL COMMENT 'FAX',
  `hp` varchar(30) DEFAULT NULL COMMENT '휴대폰 연락처',
  `addr` varchar(200) DEFAULT NULL COMMENT '주소',
  `addr_en` varchar(200) DEFAULT NULL COMMENT '주소(영문)',
  `email` varchar(100) DEFAULT NULL COMMENT '이메일',
  `report_issue_addr` varchar(300) DEFAULT NULL COMMENT '성적서발행처 주소',
  `report_issue_addr_en` varchar(300) DEFAULT NULL COMMENT '성적서발행처 주소(영문)',
  `site_addr` varchar(300) DEFAULT NULL COMMENT '소재지 주소',
  `site_addr_en` varchar(300) DEFAULT NULL COMMENT '소재지 주소(영문)',
  `agent_num` varchar(20) DEFAULT NULL COMMENT '사업자등록번호',
  `back_account` varchar(200) DEFAULT NULL COMMENT '거래은행(계좌번호)',
  `kolas` varchar(500) DEFAULT NULL COMMENT 'KOLAS 인증 이미지 경로 (오브젝트 스토리지 key)',
  `ilac` varchar(500) DEFAULT NULL COMMENT '아일락 이미지 경로 (오브젝트 스토리지 key)',
  `company` varchar(500) DEFAULT NULL COMMENT '사내 로고 이미지 경로 (오브젝트 스토리지 key)',
  `update_member_id` bigint DEFAULT NULL COMMENT '수정자 (member.id)',
  `update_datetime` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회사 기본정보 (단일 row, id=1 고정)';