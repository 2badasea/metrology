package com.bada.cali.entity;

import com.bada.cali.common.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_manager")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentManager {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;        // 담당자 고유번호(기본키)
	
	@Column(name = "main_yn")
	@Builder.Default
	@Enumerated(EnumType.STRING)        // NOTATION 9번
	private YnType mainYn = YnType.n;        // y: 대표담당자(업체대표X), n: 일반담당자
	
	@Column(name = "agent_id", nullable = false)
	@Builder.Default
	private Long agentId = 0L;        // 업체id (내부 직원의 경우 모두 0)
	
	@Column(name = "name", length = 100, nullable = false)
	private String name;        // 담당자명
	
	@Column(name = "email", length = 125)
	private String email;        // 담당자 이메일 주소
	
	@Column(name = "tel", length = 20)
	private String tel;            // 담당자 연락처
	
	// 삭제여부
	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType isVisible = YnType.y;
	
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime;		// 생성일시
	
	@Column(name = "create_member_id", nullable = false)
	@Builder.Default
	private Long createMemberId = 0L;			// 생성자id
	
	@Column(name = "update_datetime", nullable = true)
	private LocalDateTime updateDatetime;		// 수정일시
	
	@Column(name = "update_member_id", nullable = false)
	@Builder.Default
	private Long updateMemberId = 0L;			// 수정자id
	
	@Column(name = "delete_datetime", nullable = true)
	private LocalDateTime deleteDatetime;		// 삭제일시
	
	@Column(name = "delete_member_id", nullable = false)
	@Builder.Default
	private Long deleteMemberId = 0L;			// 삭제자id
	
	
}
