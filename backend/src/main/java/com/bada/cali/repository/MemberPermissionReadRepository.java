package com.bada.cali.repository;

import com.bada.cali.entity.MemberPermissionRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPermissionReadRepository extends JpaRepository<MemberPermissionRead, Long> {
	
	// 이 멤버가 읽기 권한 가진 메뉴 id 목록만 가져오기
	@Query("select mpr.menu.id " +
			"from MemberPermissionRead mpr " +
			"where mpr.member.id = :memberId")
	List<Long> findMenuIdsByMemberId(@Param("memberId") int memberId);
	
}
