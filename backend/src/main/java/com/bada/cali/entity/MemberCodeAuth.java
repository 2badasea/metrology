package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

// 직원별 중분류 관게 매핑
@Entity
@Table(
		name = "member_code_auth",
		indexes = {
				@Index(name = "idx_member_code_auth_middle", columnList = "middle_item_code_id")
		}
)
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
	
}
