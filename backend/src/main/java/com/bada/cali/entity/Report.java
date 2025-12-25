package com.bada.cali.entity;

import com.bada.cali.common.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// NOTE DDL 자동생성(ddl-auto) 기능은 꺼져있지만, 문서/메타데이터 역할을 위해 명시
@Entity
@Table(
		name = "report",
		uniqueConstraints = {        // 성적서번호 유니크 제약
				@UniqueConstraint(name = "report_num", columnNames = "report_num"),
				@UniqueConstraint(name = "manage_no", columnNames = "manage_no")
		},
		indexes = {        // where조건으로 자주 사용되는 필드에 대해서 인덱스 생성
				@Index(name = "is_visible", columnList = "is_visible"),
				@Index(name = "cali_order_id", columnList = "cali_order_id")
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 접수번호 id
	@Column(name = "cali_order_id", nullable = false)
	private Long caliOrderId;
	
	// 성적서번호 (NULL을 허용한다. 자식성적서는 성적서번호가 부모를 따라감)
	@Column(name = "report_num", length = 200)
	private String reportNum;
	
	// 부모성적서 id
	@Column(name = "parent_id")
	private Long parentId;
	
	// 분동성적서 부모id
	@Column(name = "parent_scale_id")
	private Long parentScaleId;
	
	// 견적서 품목 id
	@Column(name = "es_sub_id")
	private Long esSubId;
	
	// 교정주기
	@Column(name = "item_cali_cycle", nullable = false)
	private Integer itemCaliCycle;
	
	// 성적서 상태 (WAIT: 대기, REPAIR: 수리, IMPOSSIBLE: 불가, WORK_RETURN:  실무자반려, REUPLOAD: 재업로드, APPROV_RETURN: 기술책임자 반려 COMPLETE: 완료)
	@Column(name = "report_status", nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ReportStatus reportStatus = ReportStatus.WAIT;	// 기본: 대기
	
	// 취소, 불가, 반려 사유
	@Column(name = "status_remark")
	private String statusRemark;		// 기본값은 NULL
	
	// 성적서 타입 (자체/ 대행)
	@Column(name = "report_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ReportType reportType = ReportType.SELF;
	
	// 접수타입 (ACCREDDIT: 공인, UNACCREDDIT: 비공식, TESTING: 시험)
	@Column(name = "order_type", nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private OrderType orderType = OrderType.ACCREDDIT;	// 기본적으로 공인 부여
	
	// 문서타입 기본적으로 ISO
	@Column(name = "doc_type", nullable = false, length = 10)
	@Builder.Default
	@Enumerated(EnumType.STRING)
	private DocType docType = DocType.ISO;
	
	// 발행타입 (기본값은 국문)
	@Column(name = "report_lang", nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ReportLang reportLang = ReportLang.KR;         // kr / en / both
	
	// 품목 고유 id (null 가능)
	@Column(name = "item_id")
	private Long itemId;
	
	// 품목명
	@Column(name = "item_name", nullable = false, length = 200)
	private String itemName;
	
	// 품목 형식
	@Column(name = "item_format", length = 200)
	private String itemFormat;
	
	// 기기번호
	@Column(name = "item_num", length = 200)
	private String itemNum;
	
	// 제작회사
	@Column(name = "item_make_agent", length = 200)
	private String itemMakeAgent;
	
	// 교정수수료
	@Column(name = "cali_fee", nullable = false)
	private Long caliFee = 0L;		// null 허용
	
	// 관리번호
	@Column(name = "manage_no", length = 50)
	private String manageNo;
	
	// 중분류 코드 고유 id
	@Column(name = "middle_item_code_id")
	private Long middleItemCodeId;
	
	// 소분류코드 고유 id
	@Column(name = "small_item_code_id")
	private Long smallItemCodeId;
	
	// 비고/요청사항
	@Column(name = "remark", columnDefinition = "LONGTEXT")
	@Lob	// JPA에게 “이 필드는 일반적인 VARCHAR 같은 짧은 문자열/바이너리가 아니라, 대용량(Large Object) 으로 저장하라” 알림
	private String remark;
	
	// 작성자 고유 id
	@Column(name = "write_member_id")
	private Long writeMemberId;
	
	// 작성일시
	@Column(name = "write_datetime")
	private LocalDateTime writeDatetime;
	
	// 실무자 id
	@Column(name = "work_member_id")
	private Long workMemberId;
	
	// 실무자 결재일시
	@Column(name = "work_datetime")
	private LocalDateTime workDatetime;
	
	// 실무자 결재 상태
	@Column(name = "work_status", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private AppStatus workStatus = AppStatus.IDLE;	// 기본 대기 상태
	
	// 기술책임자 id
	@Column(name = "approval_member_id")
	private Long approvalMemberId;
	
	// 기술책임자 결재일시
	@Column(name = "approval_datetime")
	private LocalDateTime approvalDatetime;
	
	// 기술책임자 결재 상태
	@Column(name = "approval_status", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private AppStatus approvalStatus = AppStatus.IDLE;	// 기본 대기 상태
	
	
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
	@Column(name = "update_datetime" , insertable = false, updatable = false)
	private LocalDateTime updateDatetime;  // 수정일시 (nullable)
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;  // 삭제일시 (nullable)
	
}
