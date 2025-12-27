package com.bada.cali.common.enums;

// 교정상세유형
public enum CaliTakeType {
	// 교정유형이 고정표준실과 연동
	SEND,			// 택배
	SELF,			// 방문
	SITE,			// 현장반입
	PICKUP,			// 픽업
	ETC,			// 기타
	
	// 교정유형의 현장교정과
	SITE_SELF,		// 현장교정
	SITE_AGCY		// 현장대행
}
