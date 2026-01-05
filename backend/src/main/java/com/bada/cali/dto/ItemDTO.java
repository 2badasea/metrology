package com.bada.cali.dto;

import com.bada.cali.common.enums.CaliType;
import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.Setter;

public class ItemDTO {
	
	// 네임스페이스 용도. 생성자 선언 불가
	private ItemDTO() {};
	
	@Getter
	@Setter
	public static class ListReq extends TuiGridDTO.Request {
		// 검색창에서 넘어오는 값들
		private YnType isInhousePossible;	// 당사가능여부 (전체일 경우 null로 초기화됨)
		private Long middleItemCodeId;		// 중분류코드id
		private Long smallItemCodeId;		// 소분류코드id
		private String searchType;        // 검색타입
		private String keyword;            // 검색 입력 키워드
	}
	
	
	
}
