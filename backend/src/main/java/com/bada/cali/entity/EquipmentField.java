package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
		name = "equipment_field"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentField {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 분야명
	@Column(name = "name", nullable = false, length = 100)
	private String name;
	
	// 분야 코드(대표 이니셜 1~2자리)
	@Column(name = "code", nullable = false, length = 2)
	private String code;
	
	// 사용여부 (y인 항목만 선택 가능)
	@Column(name = "is_use", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isUse = YnType.y;
	
	// 비고
	@Lob
	@Column(name = "remark", columnDefinition = "TEXT")
	private String remark;
	
	// 삭제유무(soft delete)
	@Column(name = "is_visible", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isVisible = YnType.y;
}
