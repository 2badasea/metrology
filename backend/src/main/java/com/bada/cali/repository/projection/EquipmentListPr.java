package com.bada.cali.repository.projection;

import com.bada.cali.common.enums.YnType;

// 표준장비관리 리스트 데이터 반환
public interface EquipmentListPr {
	Long getId();
	
	String getManageNo();
	String getName();
	String getMakeAgent();
	String getSerialNo();
	
	YnType getIsUse();
	YnType getIsDispose();
	
	String getModelName();
	String getInstallLocation();
	
	// JOIN으로 가져온 별칭들
	String getFieldName();
	String getPrimaryManager();
	String getSecondaryManager();
	
}
