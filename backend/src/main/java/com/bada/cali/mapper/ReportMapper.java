package com.bada.cali.mapper;

import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface ReportMapper {
	
	Report toEntity(ReportDTO.addReportReq dto);
}
