package com.bada.cali.common.enums;

/**
 * 보통 실무에서는 java/spring의 enum 상수값은 대문자로 사용하는 것이 관례
 */
public enum YnType {
	y, n;
	
	public boolean isY() {
		return this == y;
	}
	
	public boolean isN() {
		return this == n;
	}
}
