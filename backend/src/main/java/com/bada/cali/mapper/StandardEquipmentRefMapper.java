package com.bada.cali.mapper;

import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.entity.StandardEquipmentRef;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface StandardEquipmentRefMapper {
	
	// record -> entity
	StandardEquipmentRef toEntityFromRecord(EquipmentDTO.UsedEquipment usedEquipment);
}
