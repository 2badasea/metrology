package com.bada.cali.mapper;


import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.Member;
import org.mapstruct.*;

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
	
	
	// 직원 등록 record -> entity
	Member toMemberByCreateReq(MemberDTO.CreateMemberReq req);


	// 직원 수정용 record -> entity 덮어쓰기
	@Mapping(target = "loginId", ignore = true)
	@Mapping(target = "pwd", ignore = true)			// 비밀번호는 별도로 더티체킹
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateMemberByReq(MemberDTO.UpdateMemberReq req, @MappingTarget Member member);
	
	
	
}
