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

public interface AgentRepository extends JpaRepository<Agent, Long> {
	
	@Query("""
			SELECT a
			FROM Agent a
			WHERE a.isVisible = :isVisible
			  AND (:isClose IS NULL OR a.isClose = :isClose)
			  AND (
			        :keyword = '' OR
			        (
			            (:searchType = 'name'     AND a.name     LIKE %:keyword%)
			         OR (:searchType = 'agentNum' AND a.agentNum LIKE %:keyword%)
			         OR (:searchType = 'addr'     AND a.addr     LIKE %:keyword%)
			         OR (:searchType = 'all'      AND (
			                a.name     LIKE %:keyword%
			             OR a.agentNum LIKE %:keyword%
			             OR a.addr     LIKE %:keyword%
			         ))
			        )
			  )
			""")
	Page<Agent> searchAgents(
			@Param("isVisible") YnType isVisible,
			@Param("isClose") YnType isClose,          // null이면 필터 미적용(전체선택)
			@Param("searchType") String searchType,    // all/name/agentNum/addr
			@Param("keyword") String keyword,          // ""이면 검색조건 미적용
			Pageable pageable
	);
	
	// 단건 조회
	Agent findByIsVisibleAndId(YnType isVisible, Long id);
	
	// 삭제대상 업체정보
	List<Agent> findAllByIdInAndIsVisible(Collection<Long> ids, YnType isVisible);
	
	// 기본: 삭제된 것을 제외하고 전체 가져오기
	Page<Agent> findByIsVisible(YnType isVisible, Pageable pageable);
	
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
	int delAgentByIds(@Param("agentIds") Collection<Long> agentIds,
					  @Param("isVisible") YnType isVisible,
					  @Param("deleteDatetime") LocalDateTime deleteDatetime,
					  @Param("deleteMemberId") Long deleteMemberId
	);
	
	// 존재하는 업체 그룹명들 반환 (null과 빈문자열 모두 배제하기 위해 JPQL 작성 && 중복그룹명 제외 by distinct)
	@Query("""
				select distinct a.groupName
				from Agent a
				where 1 =1
					and a.isVisible = :isVisible
					and (a.groupName <> '' and a.groupName IS NOT NULL)
			
			""")
	List<String> findAllByIsVisibleAndGroupNameIsNotBlank(YnType isVisible);
	
	// 업체 그룹명 업데이트
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
						UPDATE Agent a
						SET a.groupName = :groupName
						WHERE a.id in :ids
			""")
	int updateAgentGroupName(
			@Param("ids") List<Long> ids,
			@Param("groupName") String groupName
	);
	
	
}
