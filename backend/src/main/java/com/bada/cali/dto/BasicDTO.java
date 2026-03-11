package com.bada.cali.dto;

import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.repository.projection.MemberLevelListPr;
import com.bada.cali.repository.projection.MemberSelectRow;

import java.util.List;

public class BasicDTO {
	
	private BasicDTO() {
	}
	
	// 기준정보 기본 필드
	public record BasicInfo(
			Long id,
			String name,
			Integer seq
	) {
	}
	
	// 기준정보 저장 데이터셋
	public record BasicSaveDataSet(
			String type,        // 'memberLevel' | 'department'
			List<BasicInfo> saveData,
			List<Long> deleteIds
	) {
	}
	
	public record MemberModifySetting(
			List<DepartmentListPr> departmentData,
			List<MemberLevelListPr> memberLevelData,
			List<ItemCodeList> isUseMiddleCodeData
	) {
	}

	/**
	 * 중분류코드별 실무자/기술책임자 목록 응답 DTO.
	 * workers  : authBitmask & 1 > 0 인 직원 (실무자 권한)
	 * approvers: authBitmask & 6 > 0 인 직원 (기술책임자 부=2 or 정=4 권한)
	 */
	public record MembersByMiddleCodeRes(
			List<MemberSelectRow> workers,
			List<MemberSelectRow> approvers
	) {
	}

	;
}
