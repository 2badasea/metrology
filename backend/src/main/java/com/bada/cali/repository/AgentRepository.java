package com.bada.cali.repository;

import com.bada.cali.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, Integer> {

	// 업체정보 조회하기
	
}
