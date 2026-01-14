package com.bada.cali.mapper;

import com.bada.cali.dto.BasicDTO;
import com.bada.cali.entity.Department;
import com.bada.cali.entity.MemberLevel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface BasicMapper {
	
	// record(department) -> entity
	Department toDepartmentEntityFromRecord(BasicDTO.BasicInfo data);
	
	// record(memberLevel) -> entity
	MemberLevel toMemberLevelEntityFromRecord(BasicDTO.BasicInfo data);
}
