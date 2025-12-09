package com.bada.cali.repository;

import com.bada.cali.entity.Member;
import com.bada.cali.common.YnType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Sprig Data JPA에서 JpaRepository를 상속한 인터페이스는 별도 애너테이션 없이도 빈으로 등록됨(명시적으로 @Repository 애너테이션 명시해도 됨)
public interface MemberRepository extends JpaRepository<Member, Long> {
	
	// loginId(username)으로 유저 정보 조회
	@Query("SELECT m FROM Member m WHERE m.loginId = :username AND m.isVisible = :isVisible ")
	Optional<Member> findByLoginId(String username, YnType isVisible);
	
	// 로그인 시도 횟수 초기화
	// 트랜잭션 애너테이션의 경우, 서비스 계층에서 선언 권장.(서비스 계층이 '비즈니스 로직의 단위')
	@Modifying
	@Query("UPDATE Member m SET m.loginCount = :setCount, m.lastLoginFailDatetime = CASE WHEN :setCount >= 1 AND :setCount <= 5 THEN CURRENT_TIMESTAMP ELSE m.lastLoginFailDatetime END  WHERE m.id = :id")
	@Transactional
	int updateMemberLoginCount(@Param("id") Long id, @Param("setCount") Integer setCount);
	
	// 마지막 로그인일시 업데이트
	@Modifying
	@Query("UPDATE Member m SET m.lastLoginDatetime = CURRENT_TIMESTAMP  WHERE m.id = :id")
	@Transactional
	int updateLastLogin(@Param("id") Long id);
	
	// 개별 id로 회원 조회
	Member findByIdAndIsVisible(Long id, YnType isVisible);
	
	// 리스트로 넘어온 id로 n개 조회
	List<Member> findAllByIdInAndIsVisible(List<Long> ids, YnType isVisible);
	
	// 업체 id를 가진 member 데이터 조회
	List<Member> findAllByAgentIdInAndIsVisible(List<Long> ids, YnType isVisible);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update Member m
			          set m.isVisible = :isVisible,
			              m.deleteDatetime = :deleteDatetime,
			              m.deleteMemberId = :deleteMemberId
			        where m.agentId in :agentIds
			""")
	void delMemberByAgentIds(@Param("agentIds") Collection<Long> agentIds,
								  @Param("isVisible") YnType isVisible,
								  @Param("deleteDatetime") LocalDateTime deleteDatetime,
								  @Param("deleteMemberId") Long deleteMemberId);
	
}
