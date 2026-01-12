package com.bada.cali.repository;

import com.bada.cali.entity.StandardEquipmentRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 표준장비관리 참조 관련 리파지토리
public interface EquipmentRefRepository extends JpaRepository<StandardEquipmentRef, Long> {
	

}
