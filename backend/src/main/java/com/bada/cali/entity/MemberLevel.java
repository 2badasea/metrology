package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_level")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLevel {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 직급명
	@Column(name = "name", nullable = false, length = 100)
	private String name;
	
	// 순서
	@Column(name = "seq", nullable = false)
	@Builder.Default
	private Integer seq = 0;
	
	// 사용유무 y:사용, n:미사용
	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false, length = 1)
	@Builder.Default
	private YnType isVisible = YnType.y;
	
	// 생성자
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;
	
	// 생성일시
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime;
	
	// 수정자
	@Column(name = "update_member_id")
	private Long updateMemberId;
	
	// 수정일시
	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime;
	
	// 삭제자
	@Column(name = "delete_member_id")
	private Long deleteMemberId;
	
	// 삭제일시
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;
	
	// 수정처리 메서드
	public void updateInfo(String name, Integer seq, Long userId) {
		this.name = name;
		this.seq = seq;
		this.updateMemberId = userId;
		this.updateDatetime = LocalDateTime.now();
	}
	
	// 삭제처리 메서드 (setter를 사용하지 않고 메서드 기반 변경)
	public void disable(Long userId) {
		this.isVisible = YnType.n;
		this.deleteDatetime = LocalDateTime.now();
		this.deleteMemberId = userId;
	}
}
