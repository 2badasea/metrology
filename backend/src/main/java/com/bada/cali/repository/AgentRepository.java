package com.bada.cali.repository;

import com.bada.cali.common.YnType;
import com.bada.cali.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentRepository extends JpaRepository<Agent, Integer> {
	
	// 기본: 삭제된 것을 제외하고 전체 가져오기
	Page<Agent> findByIsVisible(YnType isVisible, Pageable pageable);
	
	// 업체명만 검색
	Page<Agent> findByIsVisibleAndNameContaining(
			YnType isVisible,
			String keyword,
			Pageable pageable)
	;
	
	// 사업자번호 검색
	Page<Agent> findByIsVisibleAndAgentNumContaining(
			YnType isVisible,
			String keyword,
			Pageable pageable
	);
	
	// 주소 검색
	Page<Agent> findByIsVisibleAndAddrContaining(
			YnType isVisible,
			String keyword,
			Pageable pageable
	);
	
	// ✅ 전체검색(all): name OR agentNum OR addr (+ isVisible='y')
	@Query("""
        SELECT a
        FROM Agent a
        WHERE a.isVisible = :isVisible
          AND (
                a.name     LIKE %:keyword%
             OR a.agentNum LIKE %:keyword%
             OR a.addr    LIKE %:keyword%
          )
        """)
	Page<Agent> searchAllVisible (
		@Param("isVisible") YnType isVisible,
		@Param("keyword") String keyword,
		Pageable pageable
	);
	
}
