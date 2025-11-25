package com.bada.cali.repository;

import com.bada.cali.common.YnType;
import com.bada.cali.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Integer> {
	
	Agent findByIsVisibleAndId(YnType isVisible, Integer id);
	
	// 삭제대상 업체정보
	List<Agent> findAllByIdInAndIsVisible(Collection<Integer> ids, YnType isVisible);
	
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
	
	// 업체 삭제 처리
	// NOTE @parma에 명시된 문자열과 쿼리문 내 ':필드명' 부분과 일치해야 함
	// => 앞에 a.xxx 부분은 entity 객체의 필드명 실제 쿼리문이 수행될 때는 @Column에 명시된 필드명으로 매핑되어서 처리된다.
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update Agent a
			          set a.isVisible = :isVisible,
			              a.deleteDatetime = :deleteDatetime,
			              a.deleteMemberId = :deleteMemberId
			        where a.id in :agentIds
			""")
	int delAgentByIds(@Param("agentIds") Collection<Integer> agentIds,
					  @Param("isVisible") YnType isVisible,
					  @Param("deleteDatetime") LocalDateTime deleteDatetime,
					  @Param("deleteMemberId") Integer deleteMemberId);
	
}
