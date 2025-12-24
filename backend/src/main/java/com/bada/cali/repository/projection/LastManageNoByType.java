package com.bada.cali.repository.projection;

// 접수구분별 가장 마지막 관리번호를 가져온다.
public interface LastManageNoByType {
	String getOrderType();
	String getManageNo();
}
