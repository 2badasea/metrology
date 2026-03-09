package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sample")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sample {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "middle_item_code_id")
	private Long middleItemCodeId;

	@Column(name = "small_item_code_id")
	private Long smallItemCodeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false)
	@Builder.Default
	private YnType isVisible = YnType.y;

	@Column(name = "create_datetime", nullable = false, updatable = false)
	private LocalDateTime createDatetime;

	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;

	@Column(name = "update_datetime", insertable = false, updatable = false)
	private LocalDateTime updateDatetime;

	@Column(name = "update_member_id")
	private Long updateMemberId;

	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;

	@Column(name = "delete_member_id")
	private Long deleteMemberId;

	// 조회 전용 연관관계 (JOIN 쿼리 지원용)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "middle_item_code_id", insertable = false, updatable = false)
	private ItemCode middleItemCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "small_item_code_id", insertable = false, updatable = false)
	private ItemCode smallItemCode;
}
