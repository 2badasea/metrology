package com.bada.cali.mapper;


import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.entity.StandardEquipment;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface StandardEquipmentMapper {
	
	// 표준장비 record -> 품목 entity
	StandardEquipment toEntityFromRecord(EquipmentDTO.EquipmentData equipmentData);
	
	// 표준장비 record(dto) -> Item엔티티로 덮어씌우기
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRecord(EquipmentDTO.EquipmentData equipmentData, @MappingTarget StandardEquipment equipment);
	
	// entity -> record
	EquipmentDTO.EquipmentData toRecordFromEntity(StandardEquipment standardEquipment);
	
}
