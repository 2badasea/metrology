-- v_260319: 성적서시트 셀위치/형식 설정 컬럼 추가
ALTER TABLE `env`
    ADD COLUMN `sheet_info_setting` json DEFAULT NULL COMMENT '성적서시트 셀위치/형식 설정 (JSON)';
