package com.bada.cali.dto;

import lombok.Getter;
import lombok.Setter;

// 교정관련 DTO 클래스
public class CaliDTO {
	
	// 'CaliDTO' 클래스는 네임스페이스 용도. 임의로 생성 방지
	private CaliDTO() {};
	
	@Getter @Setter
	public static class GetOrderListReq extends TuiGridDTO.Request {
		// 검색창에서 넘어오는 값들 정리
		private String searchType;		// 검색타입
		private String keyword;			// 검색 입력 키워드
	}
}
