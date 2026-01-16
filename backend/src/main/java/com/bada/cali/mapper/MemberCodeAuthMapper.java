package com.bada.cali.mapper;

import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.MemberCodeAuth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface MemberCodeAuthMapper {
	
	@Mapping(target = "memberId", source = "memberId")
	MemberCodeAuth toEntity(MemberDTO.MemberAuthData dto, Long memberId);
}
