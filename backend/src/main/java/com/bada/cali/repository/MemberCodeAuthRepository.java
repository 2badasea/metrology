package com.bada.cali.repository;

import com.bada.cali.entity.MemberCodeAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberCodeAuthRepository extends JpaRepository<MemberCodeAuth, MemberCodeAuth.MemberCodeAuthId> {
	
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				DELETE FROM MemberCodeAuth AS m
				WHERE m.memberId = :memberId
			""")
	int deleteMemberCodeAuth(
			@Param("memberId") Long memberId);
}
