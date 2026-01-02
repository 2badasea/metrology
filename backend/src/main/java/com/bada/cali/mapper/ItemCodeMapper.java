package com.bada.cali.mapper;

import com.bada.cali.dto.ItemCodeDTO;
import com.bada.cali.entity.ItemCode;
import org.mapstruct.*;

// 품목코드(item_code) 관련 dto <-> entity 변환
@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface ItemCodeMapper {
	
	// toEntity
	ItemCode toEntity(ItemCodeDTO.ItemCodeData itemCodeData);
	
	// toEntity for update mapping
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void toEntityForUpdate(ItemCodeDTO.ItemCodeData itemCodeData, @MappingTarget ItemCode itemCode);
	
}
