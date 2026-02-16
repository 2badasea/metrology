CREATE TABLE `agent` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '업체 고유번호',
	`create_type` VARCHAR(100) NOT NULL DEFAULT 'basic' COMMENT 'basic : 기본데이터(직접등록) , auto: 자동등록(접수), join(회원가입)' COLLATE 'utf8mb4_0900_ai_ci',
	`group_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '업체그룹명' COLLATE 'utf8mb4_0900_ai_ci',
	`name` VARCHAR(100) NULL DEFAULT NULL COMMENT '업체명' COLLATE 'utf8mb4_0900_ai_ci',
	`name_en` VARCHAR(100) NULL DEFAULT NULL COMMENT '업체명 영문' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_flag` INT NOT NULL DEFAULT '0' COMMENT '업체형태(1:고객사 , 2:대리점 , 4:성적서업체 , 8:교정기관 , 16 : 판매기관)',
	`ceo` VARCHAR(50) NULL DEFAULT NULL COMMENT '대표자' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_num` VARCHAR(20) NULL DEFAULT NULL COMMENT '사업자등록번호' COLLATE 'utf8mb4_0900_ai_ci',
	`business_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '업태(업태코드 따로 생성)' COLLATE 'utf8mb4_0900_ai_ci',
	`business_kind` VARCHAR(50) NULL DEFAULT NULL COMMENT '종목(종목코드 따로 생성)' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '형태(형태코드 따로 생성)' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_zip_code` CHAR(50) NULL DEFAULT NULL COMMENT '우편번호' COLLATE 'utf8mb4_0900_ai_ci',
	`addr` VARCHAR(100) NULL DEFAULT NULL COMMENT '성적서 발행주소' COLLATE 'utf8mb4_0900_ai_ci',
	`addr_en` VARCHAR(100) NULL DEFAULT NULL COMMENT '성적서 발행주소 영문' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_tel` VARCHAR(100) NULL DEFAULT NULL COMMENT '전화번호' COLLATE 'utf8mb4_0900_ai_ci',
	`phone` VARCHAR(100) NULL DEFAULT NULL COMMENT '휴대전화번호' COLLATE 'utf8mb4_0900_ai_ci',
	`email` VARCHAR(100) NULL DEFAULT NULL COMMENT '메일' COLLATE 'utf8mb4_0900_ai_ci',
	`manager` VARCHAR(100) NULL DEFAULT NULL COMMENT '업체 담당자명' COLLATE 'utf8mb4_0900_ai_ci',
	`manager_email` VARCHAR(50) NULL DEFAULT NULL COMMENT '업체 담당자 이메일' COLLATE 'utf8mb4_0900_ai_ci',
	`manager_tel` VARCHAR(50) NULL DEFAULT NULL COMMENT '업체 담당자 연락처' COLLATE 'utf8mb4_0900_ai_ci',
	`fax` VARCHAR(100) NULL DEFAULT NULL COMMENT '팩스' COLLATE 'utf8mb4_0900_ai_ci',
	`account_number` VARCHAR(250) NULL DEFAULT NULL COMMENT '계좌정보(ex. 은행명, 예금주, 계좌번호 등등)' COLLATE 'utf8mb4_0900_ai_ci',
	`remark` LONGTEXT NULL DEFAULT NULL COMMENT '업체 특이사항' COLLATE 'utf8mb4_0900_ai_ci',
	`calibration_cycle` VARCHAR(50) NOT NULL DEFAULT 'SELF_CYCLE' COMMENT '\'self_cycle\',\'next_cycle\',\'none\'' COLLATE 'utf8mb4_0900_ai_ci',
	`self_discount` DECIMAL(4,1) NULL DEFAULT NULL COMMENT '자체 할인율',
	`out_discount` DECIMAL(4,1) NULL DEFAULT NULL COMMENT '대행 할인율',
	`is_visible` ENUM('y','n') NOT NULL DEFAULT 'y' COMMENT 'y:사용, n:미사용' COLLATE 'utf8mb4_0900_ai_ci',
	`is_close` ENUM('y','n') NOT NULL DEFAULT 'n' COMMENT '폐업 구분' COLLATE 'utf8mb4_0900_ai_ci',
	`create_datetime` DATETIME NOT NULL DEFAULT (now()) COMMENT '등록일시',
	`create_member_id` BIGINT NOT NULL COMMENT '등록자',
	`update_datetime` DATETIME NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
	`update_member_id` BIGINT NULL DEFAULT NULL COMMENT '수정자',
	`delete_datetime` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
	`delete_member_id` BIGINT NULL DEFAULT NULL COMMENT '삭제자',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `create_member_id` (`create_member_id`) USING BTREE,
	INDEX `update_member_id` (`update_member_id`) USING BTREE,
	INDEX `delete_member_id` (`delete_member_id`) USING BTREE,
	INDEX `is_visible` (`is_visible`) USING BTREE
)
COMMENT='업체(거래처)관리'
COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=15
;


CREATE TABLE `agent_manager` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '담당자 고유번호 (기본키)',
	`main_yn` ENUM('y','n') NOT NULL DEFAULT 'n' COMMENT '대표 담당자 여부' COLLATE 'utf8mb4_0900_ai_ci',
	`agent_id` BIGINT NULL DEFAULT NULL COMMENT 'agent 고유번호 (내부 직원은 무조건 NULL. 즉, 값이 없음)',
	`name` VARCHAR(100) NULL DEFAULT NULL COMMENT '담당자명' COLLATE 'utf8mb4_0900_ai_ci',
	`email` VARCHAR(125) NULL DEFAULT NULL COMMENT '담당자 메일주소' COLLATE 'utf8mb4_0900_ai_ci',
	`tel` VARCHAR(20) NULL DEFAULT NULL COMMENT '담당자 전화번호' COLLATE 'utf8mb4_0900_ai_ci',
	`is_visible` ENUM('y','n') NOT NULL DEFAULT 'y' COMMENT '삭제 유무 - y: 노출됨(삭제되지 않음),  n: 삭제처리(노출되지 않음)' COLLATE 'utf8mb4_0900_ai_ci',
	`create_datetime` DATETIME NOT NULL DEFAULT (now()) COMMENT '등록일시',
	`create_member_id` BIGINT NOT NULL COMMENT '등록자',
	`update_datetime` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
	`update_member_id` BIGINT NULL DEFAULT NULL COMMENT '수정자',
	`delete_datetime` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
	`delete_member_id` BIGINT NULL DEFAULT NULL COMMENT '삭제자',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `is_visible` (`is_visible`) USING BTREE,
	INDEX `agent_id` (`agent_id`) USING BTREE
)
COMMENT='업체 담당자 관리 테이블'
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=21
;


CREATE TABLE `cali_order` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '교정접수 고유 id',
	`completion_id` BIGINT NULL DEFAULT NULL COMMENT '완료통보서 id.    1개의 완료통보서는 n개의 접수로 구성될 수 있음.',
	`estimate_id` BIGINT NULL DEFAULT NULL COMMENT '견적서 id   1개의 견적서를 기반으로 여러 접수 건 생성가능',
	`btrip_id` BIGINT NULL DEFAULT NULL COMMENT '출장일정id - 접수와 출장일정은 1:1의 관계(출장일정에서 접수 생성 시 값을 가진다)',
	`order_type` VARCHAR(50) NOT NULL DEFAULT 'ACCREDDIT' COMMENT 'ACCREDDIT 공인, UNACCREDDIT비공인, TESTING: 시험' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_cali_cycle` VARCHAR(50) NOT NULL DEFAULT 'NEXT_CYCLE' COMMENT '신청업체 교정주기 SELF_CYCLE: 자체교정주기, NEXT_CYCLE: 차기교정주기' COLLATE 'utf8mb4_0900_ai_ci',
	`priority_type` VARCHAR(50) NOT NULL DEFAULT 'NORMAL' COMMENT '긴급여부 NORMALl: 일반, EMERGENCY: 긴급' COLLATE 'utf8mb4_0900_ai_ci',
	`cali_type` VARCHAR(50) NOT NULL DEFAULT 'STANDARD' COMMENT '교정유형: STANDARD(고정표준실), SITE(현장교정)' COLLATE 'utf8mb4_0900_ai_ci',
	`cali_take_type` VARCHAR(50) NOT NULL DEFAULT 'SELF' COMMENT '교정 상세유형 SEND: 택배, SELF:  방문, SITE: 현장반입, PICKUP: 픽업, ETC: 기타, SITE_SELF: 현장교정, SITE_AGCY: 현장대행' COLLATE 'utf8mb4_0900_ai_ci',
	`report_lang` VARCHAR(50) NOT NULL DEFAULT 'KR' COMMENT '발행타입 KR: 국문, EN: 영문, BOTH: 둘 다   - 최초 성적서 생성 시, 접수타입 따라감' COLLATE 'utf8mb4_0900_ai_ci',
	`doc_type` ENUM('ISO','B') NOT NULL DEFAULT 'ISO' COMMENT '문서타입 ISO: 공개, B: 비공개  ' COLLATE 'utf8mb4_0900_ai_ci',
	`btrip_start_date` DATETIME NULL DEFAULT NULL COMMENT '출장일정 시작일시',
	`btrip_end_date` DATETIME NULL DEFAULT NULL COMMENT '출장일정 종료일시',
	`responsible_manager_id` BIGINT NOT NULL COMMENT '책임담당자 id',
	`order_manager_id` BIGINT NOT NULL COMMENT '접수자 id',
	`order_date` DATE NOT NULL COMMENT '접수일자',
	`complete_date` DATE NULL DEFAULT NULL COMMENT '완료일자',
	`order_num` VARCHAR(100) NULL DEFAULT NULL COMMENT '접수번호' COLLATE 'utf8mb4_0900_ai_ci',
	`order_group_num` VARCHAR(100) NULL DEFAULT NULL COMMENT '접수그룹번호' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent` VARCHAR(200) NULL DEFAULT NULL COMMENT '신청업체명' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_tel` VARCHAR(100) NULL DEFAULT NULL COMMENT '신청업체 연락처' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_fax` VARCHAR(100) NULL DEFAULT NULL COMMENT '신청업체 FAX' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_en` VARCHAR(200) NULL DEFAULT NULL COMMENT '신청업체 영문명' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_id` BIGINT NOT NULL COMMENT '신청업체 id',
	`cust_agent_addr` VARCHAR(250) NULL DEFAULT NULL COMMENT '신청업체 주소' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_agent_addr_en` VARCHAR(250) NULL DEFAULT NULL COMMENT '신청업체 주소(영문명)' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_manager` VARCHAR(50) NULL DEFAULT NULL COMMENT '신청업체 담당자' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_manager_tel` VARCHAR(100) NULL DEFAULT NULL COMMENT '신청업체 담당자 연락처' COLLATE 'utf8mb4_0900_ai_ci',
	`cust_manager_email` VARCHAR(100) NULL DEFAULT NULL COMMENT '신청업체 담당자 이메일' COLLATE 'utf8mb4_0900_ai_ci',
	`report_agent` VARCHAR(100) NULL DEFAULT NULL COMMENT '성적서발행처' COLLATE 'utf8mb4_0900_ai_ci',
	`report_agent_en` VARCHAR(100) NULL DEFAULT NULL COMMENT '성적서발행처(영문명)' COLLATE 'utf8mb4_0900_ai_ci',
	`report_agent_id` BIGINT NOT NULL COMMENT '성적서발행처 id',
	`report_agent_addr` VARCHAR(200) NULL DEFAULT NULL COMMENT '성적서발행처 주소' COLLATE 'utf8mb4_0900_ai_ci',
	`report_agent_addr_en` VARCHAR(200) NULL DEFAULT NULL COMMENT '성적서발행처 주소 (영문)' COLLATE 'utf8mb4_0900_ai_ci',
	`report_manager` VARCHAR(50) NULL DEFAULT NULL COMMENT '성적서발행처 담당자' COLLATE 'utf8mb4_0900_ai_ci',
	`report_manager_tel` VARCHAR(50) NULL DEFAULT NULL COMMENT '성적서발행처 담당자 연락처' COLLATE 'utf8mb4_0900_ai_ci',
	`report_manager_email` VARCHAR(100) NULL DEFAULT NULL COMMENT '성적서발행처 담당자 이메일' COLLATE 'utf8mb4_0900_ai_ci',
	`site_addr` VARCHAR(200) NULL DEFAULT NULL COMMENT '소재지 주소 (실제 교정이 진행되는 곳)' COLLATE 'utf8mb4_0900_ai_ci',
	`site_addr_en` VARCHAR(200) NULL DEFAULT NULL COMMENT '소재지 주소 (영문)' COLLATE 'utf8mb4_0900_ai_ci',
	`remark` TEXT NULL DEFAULT NULL COMMENT '비고' COLLATE 'utf8mb4_0900_ai_ci',
	`status_type` ENUM('wait','complete','cancel','hold') NOT NULL DEFAULT 'wait' COMMENT '접수상태 wait: 기본, complete(완료), cancel(취소), hold(보류)' COLLATE 'utf8mb4_0900_ai_ci',
	`is_tax` ENUM('y','n') NOT NULL DEFAULT 'n' COMMENT '세금계산서 발행여부 ' COLLATE 'utf8mb4_0900_ai_ci',
	`is_order_send` ENUM('y','n') NOT NULL DEFAULT 'n' COMMENT '메일발송여부 - 교정신청서 발송여부' COLLATE 'utf8mb4_0900_ai_ci',
	`is_visible` ENUM('y','n') NOT NULL DEFAULT 'y' COMMENT '삭제여부 y: 미삭제, n:삭제  - 삭제 처리 시, 접수번호에 deleted 문구 추가' COLLATE 'utf8mb4_0900_ai_ci',
	`create_datetime` DATETIME NOT NULL DEFAULT (now()) COMMENT '생성일시',
	`create_member_id` BIGINT NOT NULL COMMENT '생성자 id',
	`update_datetime` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
	`update_member_id` BIGINT NULL DEFAULT NULL COMMENT '수정자 id',
	`delete_datetime` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
	`delete_member_id` BIGINT NULL DEFAULT NULL COMMENT '삭제자 id',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `order_num` (`order_num`) USING BTREE,
	INDEX `is_visible` (`is_visible`) USING BTREE
)
COMMENT='교정접수 테이블'
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=8
;


