package com.bada.cali.dto;


import com.bada.cali.common.enums.CaliCycleUnit;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ItemCodeDTO {
	
	// 생성자를 통해서 호출할수 없도록. 네임스페이스 용도로만 클래스가 사용된다.
	private ItemCodeDTO() {
	}
	
	// 분류코드관리 리스트(그리드)에서 조회를 하는 경우
	public record ItemCodeListReq(
			CodeLevel codeLevel,    // 분류코드 레벨(깊이)
			Long parentId            // 부모id
	) {
	}
	
	public record ItemCodeData (
			Long id,
			Long parentId,
			CodeLevel codeLevel,
			String codeNum,
			String codeName,
			String codeNameEn,
			CaliCycleUnit caliCycleUnit,
			Integer stdCali,
			Integer preCali,
			String tracestatementInfo,
			YnType isKolasStandard
	){
	
	}
}
