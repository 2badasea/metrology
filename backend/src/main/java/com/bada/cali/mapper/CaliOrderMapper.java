package com.bada.cali.mapper;


import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.CaliOrder;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface CaliOrderMapper {
	
	CaliDTO.OrderRowData toOrderDataFromEntity(CaliOrder caliOrder);
	
	// entity로 변환
	CaliOrder toSaveEntity(CaliDTO.saveCaliOrder saveCaliOrder);
	
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromDto(CaliDTO.saveCaliOrder dto, @MappingTarget CaliOrder entity);
	
	// 접수데이터 정보 entity 조회 후 dto변환
	CaliDTO.saveCaliOrder toDtoFromCaliOrderInfo(CaliOrder caliOrder);
}
