package com.bada.cali.repository.projection;

import com.bada.cali.common.enums.CaliCycleUnit;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;

// 분류코드관리 페이지 리스트에 표시할 데이터용 프로젝션
public interface ItemCodeList {
	Long getId();
	Long getParentId();
	CodeLevel getCodeLevel();
	String getCodeNum();
	String getCodeName();
	String getCodeNameEn();
	CaliCycleUnit getCaliCycleUnit();
	Integer getStdCali();
	Integer getPreCali();
	String getTracestatementInfo();
	YnType getIsKolasStandard();	// 코라스 표준 여부
	YnType getIsVisible();
}
