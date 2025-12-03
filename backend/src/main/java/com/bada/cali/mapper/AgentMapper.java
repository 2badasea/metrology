package com.bada.cali.mapper;

import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.Agent;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface AgentMapper {
	
	// 회원가입 요청 DTO -> 업체등록 Entity
	// agent_zip_code, name, addr, manager, ceo, email, phone, agent_tel, agent_num(사업자번호 = 로그인아이디)
//	@Mapping(target = "email", source = "email")	// 이름이 같은 건 명시할 필요없음 (target 중복 선언X)
//	@Mapping(target = "createType", constant = "join")// 임의로 값 추가 setter 메서드로 추가
	@Mapping(target = "managerEmail", source = "email")
	@Mapping(target = "agentNum", source = "loginId")
	Agent toAgentFromMemberJoinReq(MemberDTO.MemberJoinReq memberJoinReq);
	
	// 업체 리스트 데이터 반환 (entity -> dto)
	AgentDTO.AgentRowData toAgentRowDataFromEntity(Agent agent);
	
	// 업체 등록 시, DTO -> Entity로 변환
	Agent toAgentEntityFromDTO(AgentDTO.SaveAgentDataReq agentRowData);
	
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	Agent toEntityFromUpdateDTO(AgentDTO.SaveAgentDataReq agentRowData, @MappingTarget Agent agent);
	
	
	
}
