package com.bada.cali.repository.projection;

// 성적서 등록 시, nativeQuery의 결과를 반환받는 프로젝션
public interface LastReportNumByOrderType {
	String getOrderType();
	String getReportNum();
}
