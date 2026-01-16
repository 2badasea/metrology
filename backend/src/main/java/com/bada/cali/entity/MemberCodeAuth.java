package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(
		name = "member_code_auth",
		indexes = {
				@Index(name = "idx_member_code_auth_middle", columnList = "middle_item_code_id")
		}
)
@IdClass(MemberCodeAuth.MemberCodeAuthId.class) // 핵심 1: IdClass 지정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCodeAuth {
	
	@Id
	@Column(name = "member_id", nullable = false)
	private Long memberId;
	
	@Id
	@Column(name = "middle_item_code_id", nullable = false)
	private Long middleItemCodeId;
	
	// 권한 비트마스크(0=없음, 1=실무자, 2=기술책임자(부), 4=기술책임자(정))
	@Column(name = "auth_bitmask", nullable = false)
	@Builder.Default
	private Integer authBitmask = 0;
	
	
	// 핵심 2: 반드시 static 이어야 함
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class MemberCodeAuthId implements Serializable {
		private Long memberId;
		private Long middleItemCodeId;
	}
	
	
}
