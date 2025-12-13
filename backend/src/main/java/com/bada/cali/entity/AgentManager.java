package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
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
	@Enumerated(EnumType.STRING)        // NOTATION 9번
	@Builder.Default
	private YnType mainYn = YnType.n;        // y: 대표담당자(업체대표X), n: 일반담당자
	
	// 기본값을 주지 않음. 업체담당자에게 agent_id값은 필수이기 때문
	@Column(name = "agent_id")
	private Long agentId;        // 업체id (내부 직원의 경우 모두 0이거나 NULL)
	
	@Column(name = "name", length = 100)
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
	
	// NOTE create_member_id의 경우, 값이 반드시 존재해야 하기 때문에 null을 허용하지 않는 대신
	// 애플리케이션 단에서 반드시 값을 초기화 시키도록 한다.
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;			// 생성자id
	
	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime;		// 수정일시
	
	@Column(name = "update_member_id")
	private Long updateMemberId;			// 수정자id
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;		// 삭제일시
	
	@Column(name = "delete_member_id")
	private Long deleteMemberId;			// 삭제자id
	
}
