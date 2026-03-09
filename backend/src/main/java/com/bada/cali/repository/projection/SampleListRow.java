package com.bada.cali.repository.projection;

// 샘플 목록 그리드용 프로젝션 (native SQL 결과 매핑)
public interface SampleListRow {
	Long getId();
	String getName();
	Long getMiddleItemCodeId();
	String getMiddleCodeNum();
	String getMiddleCodeName();
	Long getSmallItemCodeId();
	String getSmallCodeNum();
	String getSmallCodeName();
}
