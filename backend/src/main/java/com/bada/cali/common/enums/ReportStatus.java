package com.bada.cali.common.enums;

public enum ReportStatus {
	WAIT,            // 기본(대기)
	REPAIR,            // 수리
	IMPOSSIBLE,        // 불가
	WORK_RETURN,            // 실무자 반려
	APPROV_RETURN,            // 기술책임자 반려
	REUPLOAD,        // 재업로드(실무자의 재상신)
	COMPLETE        // 교정완료
}
