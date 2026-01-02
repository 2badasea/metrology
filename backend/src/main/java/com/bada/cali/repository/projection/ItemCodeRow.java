package com.bada.cali.repository.projection;

import com.bada.cali.common.enums.CaliCycleUnit;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;

public interface ItemCodeRow {
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
	YnType getIsVisible();
}
