package com.bada.cali.mapper;


import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface MemberMapper {
	
	// 회원가입 요청 DTO -> MemberEntity
	@Mapping(target = "addr1", source = "addr")
	@Mapping(target = "hp", source = "phone")
	@Mapping(target = "tel", source = "agentTel")
	Member toMemberFromMemberJoin(MemberDTO.MemberJoinReq memberJoinReq);
	
	
}
