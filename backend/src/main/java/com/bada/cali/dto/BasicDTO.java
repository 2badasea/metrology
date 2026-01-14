package com.bada.cali.dto;

import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.MemberLevelListPr;

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
			String type,		// 'memberLevel' | 'department'
			List<BasicInfo> saveData,
			List<Long> deleteIds
	) {
	}
	
	public record DepartmentAndMemberLevel(
			List<DepartmentListPr> departmentData,
			List<MemberLevelListPr> memberLevelData
	){};
}
