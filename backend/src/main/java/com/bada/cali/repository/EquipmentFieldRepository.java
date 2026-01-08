package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.entity.EquipmentField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentFieldRepository extends JpaRepository<EquipmentField, Integer> {
	
	@Query("""
		select f
		from EquipmentField f
		where f.isVisible = :isVisible
			and (:isUse IS NULL OR f.isUse = :isUse)
	""")
	List<EquipmentDTO.EquipFieldData> getEquipmentField(
			@Param("isVisible") YnType isVisible,
			@Param("isUse") YnType isUse);
}
