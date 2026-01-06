package com.bada.cali.mapper;

import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.entity.Item;
import com.bada.cali.entity.Report;
import com.bada.cali.repository.projection.ItemList;
import org.mapstruct.*;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE  // 매핑 안 한 엔티티 필드는 일단 무시
)
public interface ItemMapper {
	
	// 품목 entity -> 프로젝션 인터페이스
	ItemDTO.ItemData toRecordFromEntity(Item itemEntity);
	
	// 품목정보 record -> 품목 entity
	Item toEntityFromRecord(ItemDTO.ItemData itemData);
	
	// 품목정보 record(dto) -> Item엔티티로 덮어씌우기
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRecord(ItemDTO.ItemData itemData, @MappingTarget Item itemEntity);
	
}
