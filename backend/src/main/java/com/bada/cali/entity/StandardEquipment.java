package com.bada.cali.entity;

import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "standard_equipment",
		indexes = {
				@Index(name = "idx_standard_equipment_is_visible", columnList = "is_visible"),
				@Index(name = "idx_standard_equipment_field_id", columnList = "equipment_field_id"),
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardEquipment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 관리번호
	@Column(name = "manage_no", length = 150)
	private String manageNo;
	
	// 장비명
	@Column(name = "name", nullable = false, length = 200)
	private String name;
	
	// 장비명(영문)
	@Column(name = "name_en", length = 200)
	private String nameEn;
	
	// 제작회사(국문)
	@Column(name = "make_agent", length = 200)
	private String makeAgent;
	
	// 제작회사(영문)
	@Column(name = "make_agent_en", length = 200)
	private String makeAgentEn;
	
	// 기기번호(시리얼)
	@Column(name = "serial_no", length = 200)
	private String serialNo;
	
	// 분야코드 FK (equipment_field)
	@Column(name = "equipment_field_id")
	private Long equipmentFieldId;
	
	// 폐기여부
	@Column(name = "is_dispose", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isDispose = YnType.n;
	
	// 모델명
	@Column(name = "model_name", length = 200)
	private String modelName;
	
	// 세부사양(정밀도)
	@Column(name = "accuracy_spec", length = 200)
	private String accuracySpec;
	
	// 관리담당자(정)
	@Column(name = "primary_manager_id")
	private Long primaryManagerId;
	
	// 관리담당자(부)
	@Column(name = "secondary_manager_id")
	private Long secondaryManagerId;
	
	// 설치위치
	@Column(name = "install_location", length = 100)
	private String installLocation;
	
	// 구입처
	@Column(name = "purchase_vendor", length = 200)
	private String purchaseVendor;
	
	// 구입처 담당자
	@Column(name = "purchase_contact_manager", length = 200)
	private String purchaseContactManager;
	
	// 구입처 연락처
	@Column(name = "purchase_vendor_tel", length = 50)
	private String purchaseVendorTel;
	
	// 구입가격
	@Column(name = "purchase_price")
	@Builder.Default
	private Long purchasePrice = 0L;
	
	// 구입일 (DATE지만 JPA에서 LocalDate를 더 권장. 일단 Item 스타일대로 LocalDateTime 유지 안 하고 LocalDate 추천)
	// 프로젝트 기준에 맞춰 LocalDate로 바꾸는 걸 권장합니다.
	// private LocalDate purchaseDate;
	@Column(name = "purchase_date")
	private LocalDate purchaseDate;
	
	// 설치일
	@Column(name = "install_date")
	private LocalDate installDate;
	
	// 세부사항
	@Column(name = "detail_note", length = 300)
	private String detailNote;
	
	// 설치조건 충족여부
	@Column(name = "is_install_condition", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isInstallCondition = YnType.y;
	
	// 중간점검여부
	@Column(name = "is_interim_check", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isInterimCheck = YnType.y;
	
	// 교정주기(개월)
	@Column(name = "cali_cycle_months")
	private Integer caliCycleMonths;
	
	// 용도
	@Column(name = "purpose", length = 200)
	private String purpose;
	
	// 관리부서
	@Column(name = "manage_department_id")
	private Long manageDepartmentId;
	
	// 도래알림일(며칠 전 알림)
	@Column(name = "due_notify_days")
	private Integer dueNotifyDays;
	
	// 사용여부
	@Column(name = "is_use", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isUse = YnType.y;		// n: 유휴상태
	
	// 매뉴얼 유무
	@Column(name = "has_manual", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType hasManual = YnType.y;
	
	// 펌웨어 유무
	@Column(name = "has_firmware", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType hasFirmware = YnType.y;
	
	// 펌웨어 버전
	@Column(name = "firmware_version", length = 50)
	private String firmwareVersion;
	
	@Column(name = "property", columnDefinition = "TEXT")
	private String property;
	
	// 부속품
	@Column(name = "accessories_note", columnDefinition = "TEXT")
	private String accessoriesNote;
	
	// 비고
	@Column(name = "remark", columnDefinition = "TEXT")
	private String remark;
	
	// 삭제여부(soft delete)
	@Column(name = "is_visible", nullable = false, length = 1)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private YnType isVisible = YnType.y;
	
	@Column(name = "create_datetime", nullable = false)
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
}
