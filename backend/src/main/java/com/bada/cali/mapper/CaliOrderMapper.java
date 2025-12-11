package com.bada.cali.mapper;


import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.CaliOrder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface CaliOrderMapper {
	
	CaliDTO.OrderRowData toOrderDataFromEntity(CaliOrder caliOrder);
}
