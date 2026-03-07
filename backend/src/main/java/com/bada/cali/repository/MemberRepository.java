package com.bada.cali.repository;

import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.Member;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.repository.projection.GetMemberInfoPr;
import com.bada.cali.repository.projection.MemberListPr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	
	// 로그인 시도 횟수 초기화 - @Transactional은 Service 계층(processLoginSuccess/processLoginFailure)에서 관리
	@Modifying
	@Query("UPDATE Member m SET m.loginCount = :setCount, m.lastLoginFailDatetime = CASE WHEN :setCount >= 1 AND :setCount <= 5 THEN CURRENT_TIMESTAMP ELSE m.lastLoginFailDatetime END  WHERE m.id = :id")
	int updateMemberLoginCount(@Param("id") Long id, @Param("setCount") Integer setCount);

	// 마지막 로그인일시 업데이트 - @Transactional은 Service 계층(processLoginSuccess)에서 관리
	@Modifying
	@Query("UPDATE Member m SET m.lastLoginDatetime = CURRENT_TIMESTAMP  WHERE m.id = :id")
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
	
	
	// 직원관리 회원 리스트
	// NOTE ADMIN 계정은 나오지 않도록 변경
	@Query("""
					SELECT
						m.id AS id,
						m.companyNo as companyNo,
						m.loginId as loginId,
						m.email as email,
						m.name as name,
						m.hp as hp,
						m.tel as tel,
						m.workType as workType,
						d.name as departmentName,
						ml.name as levelName
					FROM Member AS m
					LEFT JOIN Department as d ON d.id = m.departmentId AND d.isVisible = :isVisible
					LEFT JOIN MemberLevel as ml ON ml.id = m.levelId AND ml.isVisible = :isVisible
					where m.isVisible = :isVisible
					and m.agentId = 0
					and m.isActive = 'y'
					and m.auth != 'ADMIN'
					and (:workType IS NULL OR m.workType = :workType)
					AND (
									:keyword = '' OR
									(
										(:searchType = 'loginId' AND m.loginId LIKE concat('%', :keyword, '%'))
										OR (:searchType = 'name' AND m.name LIKE concat('%', :keyword, '%'))
										OR (:searchType = 'email' AND m.email LIKE concat('%', :keyword, '%'))
										OR (:searchType = 'department' AND d.name LIKE concat('%', :keyword, '%'))
										OR (:searchType = 'level' AND ml.name LIKE concat('%', :keyword, '%'))
										OR (:searchType = 'all' AND (
																	m.loginId LIKE concat('%', :keyword, '%')
																	OR 	m.name LIKE concat('%', :keyword, '%')
																	OR 	m.email LIKE concat('%', :keyword, '%')
																	OR 	d.name LIKE concat('%', :keyword, '%')
																	OR 	ml.name LIKE concat('%', :keyword, '%')
																)
										)
									)
								)
			
			""")
	Page<MemberListPr> getMemberList(
			@Param("workType") Integer workType,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			@Param("isVisible") YnType isVisible,
			PageRequest pageRequest
	);
	
	// 직원 소프트삭제 (id 목록 기준, 이미 삭제된 건 제외)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Member m
			   set m.isVisible      = :isVisible,
			       m.deleteDatetime = :deleteDatetime,
			       m.deleteMemberId = :deleteMemberId
			 where m.id in :ids
			   and m.isVisible = 'y'
			""")
	int softDeleteByIds(@Param("ids") List<Long> ids,
						@Param("isVisible") YnType isVisible,
						@Param("deleteDatetime") LocalDateTime deleteDatetime,
						@Param("deleteMemberId") Long deleteMemberId);

	@Query("""
			   select
			     m.loginId AS loginId,
			     m.name AS name,
			     m.nameEng AS nameEng,
			     m.hp AS hp,
			     m.companyNo AS companyNo,
			     m.birth AS birth,
			     m.addr1 AS addr1,
			     m.addr2 AS addr2,
			     m.email AS email,
			     m.tel AS tel,
			     m.departmentId AS departmentId,
			     m.levelId AS levelId,
			     m.isActive AS isActive,
			     m.joinDate AS joinDate,
			     m.leaveDate AS leaveDate,
			     m.workType AS workType,
			     m.remark AS remark,
			     f.id AS imgFileId,
				 f.extension as extension,
				 f.dir as dir
			    FROM Member m
			    LEFT JOIN FileInfo f
			      ON f.refTableName = 'member'
			     and f.refTableId = :id
			     AND f.isVisible = :isVisible
			    WHERE m.id = :id
			""")
	GetMemberInfoPr getMemberInfo(
			@Param("id") Long id,
			@Param("isVisible") YnType isVisible
	);
	
}
