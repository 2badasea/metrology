package com.bada.cali.repository;

import com.bada.cali.common.YnType;
import com.bada.cali.entity.AgentManager;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgentManagerRepository extends CrudRepository<AgentManager, Integer> {
	
	AgentManager findByAgentIdAndIsVisible(Integer agentId, YnType ynType);
	
	List<AgentManager> findAllByAgentIdInAndIsVisible(Collection<Integer> agentIds, YnType isVisible);
	
	// 업체 담당자 삭제
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update AgentManager am
			          set am.isVisible = :isVisible,
			              am.deleteDatetime = :deleteDatetime,
			              am.deleteMemberId = :deleteMemberId
			        where am.agentId in :agentIds
			""")
	void delAgentManagerByAgentIds(@Param("agentIds") Collection<Integer> agentIds,
					  @Param("isVisible") YnType isVisible,
					  @Param("deleteDatetime") LocalDateTime deleteDatetime,
					  @Param("deleteMemberId") Integer deleteMemberId);
	
	
}
	

