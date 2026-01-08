package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
		name = "standard_equipment_ref",
		indexes = {
				@Index(name = "idx_std_eq_ref_lookup", columnList = "ref_table, ref_table_id, seq"),
				@Index(name = "idx_std_eq_ref_equipment_id", columnList = "equipment_id")
		},
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_std_eq_ref",
						columnNames = {"ref_table", "ref_table_id", "equipment_id"}
				)
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardEquipmentRef {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 표준장비 id (standard_equipment.id)
	@Column(name = "equipment_id", nullable = false)
	private Long equipmentId;
	
	// 참조 테이블명 (ex: report, business_trip)
	@Column(name = "ref_table", nullable = false, length = 50)
	private String refTable;
	
	// 참조 테이블의 id
	@Column(name = "ref_table_id", nullable = false)
	private Long refTableId;
	
	// 표시 순서 (0=미지정, 실무에선 저장 시 1..N으로 세팅 권장)
	@Column(name = "seq", nullable = false)
	@Builder.Default
	private Integer seq = 0;
	
}
