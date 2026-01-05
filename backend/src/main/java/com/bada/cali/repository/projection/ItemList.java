package com.bada.cali.repository.projection;

import com.bada.cali.common.enums.CreateType;
import com.bada.cali.common.enums.YnType;

// 품목리스트 반환
public interface ItemList {
	Long getId();
	
	CreateType getCreateType();
	
	Long getMiddleItemCodeId();
	
	Long getSmallItemCodeId();
	
	String getName();
	
	String getNameEn();
	
	String getMakeAgent();
	
	String getMakeAgentEn();
	
	String getFormat();
	
	String getNum();
	
	Long getFee();
	
	Integer getCaliCycle();
	
	YnType getIsInhousePossible();
	
	// 엔티티에도 존재하므로 보통 함께 둡니다(리스트/검색에서 많이 씀)
	YnType getIsVisible();
	
}
