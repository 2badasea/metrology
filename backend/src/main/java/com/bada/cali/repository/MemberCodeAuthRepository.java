package com.bada.cali.repository;

import com.bada.cali.entity.MemberCodeAuth;
import com.bada.cali.repository.projection.MemberCodeAuthPr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCodeAuthRepository extends JpaRepository<MemberCodeAuth, MemberCodeAuth.MemberCodeAuthId> {
	
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				DELETE FROM MemberCodeAuth AS m
				WHERE m.memberId = :memberId
			""")
	int deleteMemberCodeAuth(
			@Param("memberId") Long memberId);
	
	
	@Query("""
				select
					m.memberId as memberId,
					m.middleItemCodeId as middleItemCodeId,
					m.authBitmask as authBitmask
				from MemberCodeAuth as m
				where m.memberId = :memberId
			""")
	List<MemberCodeAuthPr> getMemberCodeAuth(
			@Param("memberId") Long memberId
	);
	
	
}
