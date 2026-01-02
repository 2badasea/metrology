package com.bada.cali.entity;

import com.bada.cali.common.enums.CaliCycleUnit;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
		name = "item_code",
		indexes = {
				@Index(name = "parent_id", columnList = "parent_id"),
				@Index(name = "is_visible", columnList = "is_visible")
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCode {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 참조하는 상위 분류코드 id
	@Column(name = "parent_id")
	private Long parentId;
	
	// 분류코드번호
	@Column(name = "code_num", nullable = false)
	private String codeNum;
	
	// 분류코드 레벨 (깊이)
	// NULL을 허용하되, 신규등록 시, 무조건 값을 넣어줄 것
	@Column(name = "code_level")
	@Enumerated(EnumType.STRING)
	private CodeLevel codeLevel;
	
	// 분류코드명(국문) - NUL 허용 X
	@Column(name = "code_name", nullable = false)
	private String codeName;
	
	// 분류코드명(영문)
	@Column(name = "code_name_en")
	private String codeNameEn;
	
	// 교정주기 단위
	@Column(name = "cali_cycle_unit")
	@Builder.Default
	@Enumerated(EnumType.STRING)
	private CaliCycleUnit caliCycleUnit = CaliCycleUnit.MONTHS;
	
	// 교정주기 (고정용 표준기) - 기본값은 NULL
	@Column(name = "std_cali")
	private Integer stdCali;
	
	
	// 교정주기 (정밀 기기) - 기본값은 NULL
	@Column(name = "pre_cali")
	private Integer preCali;
	
	// 소급성문구 (json 형태로 관리)
	@Column(name = "tracestatement_info", columnDefinition = "json")
	private String tracestatementInfo;
	
	// KOLAS 표준 분류코드 여부
	@Column(name = "is_kolas_standard", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isKolasStandard = YnType.n;    // 초기 기초데이터를 제외하곤 모두 비공식
	
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
