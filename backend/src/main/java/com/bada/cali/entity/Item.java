package com.bada.cali.entity;

import com.bada.cali.common.enums.CreateType;
import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// NOTE DDL 자동생성(ddl-auto) 기능은 꺼져있지만, 문서/메타데이터 역할을 위해 명시
@Entity
@Table(
		name = "item",
		indexes = {        // where조건으로 자주 사용되는 필드에 대해서 인덱스 생성
				@Index(name = "is_visible", columnList = "is_visible")
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 생성타입 (BASIC: 기본, AUTO: 자동등록)
	@Column(name = "create_type", nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private CreateType createType = CreateType.BASIC;
	
	// 중분류 코드 고유 id
	@Column(name = "middle_item_code_id")
	private Long middleItemCodeId;
	
	// 소분류코드 고유 id
	@Column(name = "small_item_code_id")
	private Long smallItemCodeId;
	
	// 품목명
	@Column(name = "name", nullable = false, length = 200)
	private String name;
	
	// 품목명 (영문)
	@Column(name = "name_en", length = 200)
	private String nameEn;
	
	// 품목 형식
	@Column(name = "format", length = 200)
	private String format;
	
	// 기기번호
	@Column(name = "num", length = 200)
	private String num;
	
	// 제작회사
	@Column(name = "make_agent", length = 200)
	private String makeAgent;
	
	// 제작회사 (영문)
	@Column(name = "make_agent_en", length = 200)
	private String makeAgentEn;
	
	// 교정수수료
	@Column(name = "fee", nullable = false)
	@Builder.Default
	private Long fee = 0L;
	
	// 교정주기
	@Column(name = "cali_cycle", nullable = false)
	private Integer caliCycle;
	
	// 당사가능여부
	@Column(name = "is_inhouse_possible", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isInhousePossible = YnType.y;
	
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
	
	
}
