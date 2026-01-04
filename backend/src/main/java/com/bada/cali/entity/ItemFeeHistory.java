package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// NOTE DDL 자동생성(ddl-auto) 기능은 꺼져있지만, 문서/메타데이터 역할을 위해 명시
@Entity
@Table(
		name = "item_fee_history",
		indexes = {        // where조건으로 자주 사용되는 필드에 대해서 인덱스 생성
				@Index(name = "is_visible", columnList = "is_visible")
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFeeHistory {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 품목id
	@Column(name = "item_id", nullable = false)
	private Long itemId;
	
	// 기준일자
	@Column(name = "base_date")
	private LocalDate baseDate;
	
	// 기준수수료
	@Column(name = "base_fee", nullable = false)
	@Builder.Default
	private Long baseFee = 0L;
	
	// 비고
	@Column(name = "remark", length = 250)
	private String remark;
	
	// 삭제유무
	@Column(name = "is_visible", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isVisible = YnType.y;
	
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;       // 생성자 id
	
	@Column(name = "update_member_id")
	private Long updateMemberId;       // 수정자 id (nullable)
	
	@Column(name = "delete_member_id")
	private Long deleteMemberId;       // 삭제자 id (nullable)
	
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime;  // 생성일시
	
	// insertable = false, updatable = false 명시를 통해 읽기 전용으로 선언. insert/update 시 hibernate가 이 컬럼을 절대로 건드리지 않음
	@Column(name = "update_datetime", insertable = false, updatable = false)
	private LocalDateTime updateDatetime;  // 수정일시 (nullable)
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;  // 삭제일시 (nullable)
	
	// 품목 테이블과의 연관관계 정의
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", insertable = false, updatable = false)
	private Item item;
}
