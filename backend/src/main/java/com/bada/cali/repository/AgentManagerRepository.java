package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.entity.AgentManager;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgentManagerRepository extends CrudRepository<AgentManager, Long> {
	
	AgentManager findByAgentIdAndIsVisible(Long agentId, YnType ynType);
	
	List<AgentManager> findAllByAgentIdInAndIsVisible(Collection<Long> agentIds, YnType isVisible);
	
	// 업체 담당자 삭제 (업체 기준)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update AgentManager am
			          set am.isVisible = :isVisible,
			              am.deleteDatetime = :deleteDatetime,
			              am.deleteMemberId = :deleteMemberId
			        where am.agentId in :agentIds
			""")
	void delAgentManagerByAgentIds(@Param("agentIds") Collection<Long> agentIds,
					  @Param("isVisible") YnType isVisible,
					  @Param("deleteDatetime") LocalDateTime deleteDatetime,
					  @Param("deleteMemberId") Long deleteMemberId);
	
	// 업체 담당자 삭제 (담당자별id값 기준)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update AgentManager am
			          set am.isVisible = :isVisible,
			              am.deleteDatetime = :deleteDatetime,
			              am.deleteMemberId = :deleteMemberId
			        where am.id in :agentManagerIds
			""")
	void delAgentManagerByIds(@Param("agentManagerIds") Collection<Long> agentManagerIds,
								   @Param("isVisible") YnType isVisible,
								   @Param("deleteDatetime") LocalDateTime deleteDatetime,
								   @Param("deleteMemberId") Long deleteMemberId);
	
	
	
	@Query("""
						SELECT am
						FROM AgentManager am
						WHERE 1 = 1
						AND am.agentId = :agentId
						AND am.isVisible = :isVisible
						ORDER BY
								case when am.mainYn = :mainYn then 0 else 1 END ASC,
								am.id ASC
								
			""")
	List<AgentManager> searchAgentManagers(
			@Param("isVisible") YnType isVisible,
			@Param("agentId") Long agentId,
			@Param("mainYn") YnType mainYn
//			Pageable pageable
	);
	
	// 업체담당자 조회 모달 내 리스트 표시 (대표여부 y가 우선 순위)
	// NOTE native query에서는 new생성자 사용 못 함.
	@Query(value = """
				    select new com.bada.cali.dto.AgentManagerDTO$AgentManagerRowData(
			         am.id,
			         am.agentId,
			         am.name,
			         am.tel,
			         am.email,
			         am.mainYn
			     )
			     from AgentManager am
			     where am.agentId = :agentId
			       and am.isVisible = com.bada.cali.common.enums.YnType.y
			     order by
			       case when am.mainYn = com.bada.cali.common.enums.YnType.y then 0 else 1 end,
			       am.id asc
			""")
	List<AgentManagerDTO.AgentManagerRowData> getManagerListOrderByMainYn(@Param("agentId") Long agentId);
	
}
	

