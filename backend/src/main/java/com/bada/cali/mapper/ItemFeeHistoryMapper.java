package com.bada.cali.mapper;

import com.bada.cali.dto.ItemDTO;
import com.bada.cali.entity.Item;
import com.bada.cali.entity.ItemFeeHistory;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface ItemFeeHistoryMapper {
	
	// record(dto) -> entity
	ItemFeeHistory toEntityFromRecord(ItemDTO.ItemFeeData itemFeeData);
	
	// 품목정보 record(dto) -> ItemFeeHistory 엔티티로 덮어씌우기
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRecord(ItemDTO.ItemFeeData itemFeeData, @MappingTarget ItemFeeHistory itemFeeHistory);
	
}
