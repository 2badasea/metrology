package com.bada.cali.dto;


import com.bada.cali.common.enums.CodeLevel;
import lombok.Getter;
import lombok.Setter;

public class ItemCodeDTO {
	
	// 생성자를 통해서 호출할수 없도록. 네임스페이스 용도로만 클래스가 사용된다.
	private ItemCodeDTO() {
	}
	
	// 분류코드관리 리스트(그리드)에서 조회를 하는 경우
	@Getter
	@Setter
	public static class ItemCodeListReq {
		CodeLevel codeLevel;	// 분류코드 레벨(깊이)
		Long parentId;			// 부모id
	}
}
