package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class EquipmentDTO {
	
	// 본 DTO클래스는 네임스페이스 용도
	private EquipmentDTO () {
	}
	
	// 표준장비 분야 코드 데이터
	public record EquipFieldData (
			Long id,
			String name,
			String code,
			YnType isUse,
			String remark
	) {
	}
	
	@Getter
	@Setter
	public static class GetLiseReq extends TuiGridDTO.Request {
		YnType isUse;				// 사용여부(유휴여부)
		YnType isDispose;			// 폐기여부
		Long equipmentFieldId;		// 분야코드 id
		String searchType;			// 검색타입
		String keyword;				// 검색키워드
	}
	
}
