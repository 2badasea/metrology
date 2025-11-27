package com.bada.cali.mapper;

import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.AgentManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface AgentManagerMapper {
	
	// 회원가입 -> 업체담당자 추가
	@Mapping(target = "tel", source = "phone")
	AgentManager toEntityFromMemberJoinReq(MemberDTO.MemberJoinReq memberJoinReq);
	
	// entity -> dto
	AgentManagerDTO.AgentManagerRowData toAgentManagerRowDataFromEntity(AgentManager agentManager);
	
	
	
}
