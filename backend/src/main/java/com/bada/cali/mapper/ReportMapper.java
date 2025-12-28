package com.bada.cali.mapper;

import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.Report;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface ReportMapper {
	
	Report toEntity(ReportDTO.addReportReq dto);
	
	// dto -> entity로 변환하면서 값 덮어씌우기 (성적서 수정)
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromDto(ReportDTO.ReportUpdateReq record, @MappingTarget Report report);
	
	// 자식성적서 수정용
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateChildEntityFromDto(ReportDTO.ChildReportInfo record, @MappingTarget Report report);
	
	Report insertChildEntityFromDto(ReportDTO.ChildReportInfo record);
}
