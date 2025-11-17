package com.bada.cali.repository;

import com.bada.cali.entity.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Sprig Data JPA에서 JpaRepository를 상속한 인터페이스는 별도 애너테이션 없이도 빈으로 등록됨(명시적으로 @Repository 애너테이션 명시해도 됨)
public interface MemberRepository extends JpaRepository<Member, Integer> {
	
	// loginId(username)으로 유저 정보 조회
	@Query("SELECT m FROM Member m WHERE m.loginId = :username AND m.isVisible = :isVisible ")
	Optional<Member> findByLoginId(String username, Member.YnType isVisible);
	
	// 로그인 시도 횟수 초기화
	// 트랜잭션 애너테이션의 경우, 서비스 계층에서 선언 권장.(서비스 계층이 '비즈니스 로직의 단위')
	@Modifying
	@Query("UPDATE Member m SET m.loginCount = :setCount, m.lastLoginFailDatetime = CASE WHEN :setCount >= 1 AND :setCount <= 5 THEN CURRENT_TIMESTAMP ELSE m.lastLoginFailDatetime END  WHERE m.id = :id")
	@Transactional
	int updateMemberLoginCount(@Param("id") Integer id, @Param("setCount") Integer setCount);
	
	// 마지막 로그인일시 업데이트
	@Modifying
	@Query("UPDATE Member m SET m.lastLoginDatetime = CURRENT_TIMESTAMP  WHERE m.id = :id")
	@Transactional
	int updateLastLogin(@Param("id") Integer id);
	
}
