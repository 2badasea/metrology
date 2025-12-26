package com.bada.cali.entity;

import com.bada.cali.common.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "cali_order",
		uniqueConstraints = {        // 접수번호 유니크 제약
				@UniqueConstraint(name = "order_num", columnNames = "order_num")
		},
		indexes = {        // where조건으로 자주 사용되는 필드에 대해서 인덱스 생성
				@Index(name = "is_visible", columnList = "is_visible")
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaliOrder {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;    // 교정접수 고유 id
	
	// ============ FK 성격 컬럼들 ============
	
	@Column(name = "completion_id")
	private Long completionId;    // 완료통보서 id (nullable)
	
	@Column(name = "estimate_id")
	private Long estimateId;      // 견적서 id (nullable)
	
	@Column(name = "btrip_id")
	private Long btripId;         // 출장일정 id (nullable)
	
	@Column(name = "responsible_manager_id", nullable = false)
	private Long responsibleManagerId; // 책임담당자 id
	
	@Column(name = "order_manager_id", nullable = false)
	private Long orderManagerId;       // 접수자 id
	
	@Column(name = "cust_agent_id", nullable = false)
	private Long custAgentId;          // 신청업체 id
	
	@Column(name = "report_agent_id", nullable = false)
	private Long reportAgentId;        // 성적서발행처 id
	
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;       // 생성자 id
	
	@Column(name = "update_member_id")
	private Long updateMemberId;       // 수정자 id (nullable)
	
	@Column(name = "delete_member_id")
	private Long deleteMemberId;       // 삭제자 id (nullable)
	
	// ============ ENUM / 코드값 컬럼들 (String 매핑) ============
	
	// ! 접수구분을 성적서 단위에서 구분할 수 있도록 정책 변경 (일단 기본값 명시)
	@Column(name = "order_type", nullable = false, length = 50)
	@Builder.Default
	private String orderType = "accredited";    // accredited(공인)/ non_accredited(비공인)/ testing(시험)
	
	// 긴급여부 (일반 NORMAL 이 기본값)
	@Column(name = "priority_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private PriorityType priorityType = PriorityType.NORMAL;
	
	@Column(name = "cali_type", nullable = false, length = 20)
	private String caliType;           // standard / site
	
	@Column(name = "cali_take_type", nullable = false, length = 20)
	private String caliTakeType;       // send / self / site / pickup
	
	@Column(name = "cust_agent_cali_cycle", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private CalibrationCycleType custAgentCaliCycle = CalibrationCycleType.NEXT_CYCLE;
	
	@Column(name = "report_lang", nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ReportLang reportLang = ReportLang.KR;         // kr / en / both
	
	// 문서타입. 사용 보류 -> 일단은 기본값으로 ISO로 저장되도록 함
	@Column(name = "doc_type", nullable = false, length = 10)
	@Builder.Default
	@Enumerated(EnumType.STRING)
	private DocType docType = DocType.ISO;
	
	// 대기/ 완료/ 취소/ 보류 선에서 모두 구분
	@Column(name = "status_type", nullable = false, length = 20)
	@Builder.Default
	private String statusType = "wait";         // wait / complete / cancel / hold
	
	/**
	 * 'columnDefinition' 속성은 JPA가 DDL을 생성할 때만 쓰는 힌트. (이미 DB에서 미리 만들어두고 entity를 정의하는 입장에선 의미가 없음)
	 */
	@Enumerated(EnumType.STRING)
	// EnumType.ORDINAL이 기본. JPA가 enum을 DB에 어떻게 저장할지 옵션. 기본상태라면 TINYINT/INT 타입의 1, 0 숫자로 들어가게 됨.
	@Column(name = "is_tax", nullable = false, columnDefinition = "ENUM('y','n') default 'n'")
	@Builder.Default
	private YnType isTax = YnType.n;              // y / n
	
	@Enumerated(EnumType.STRING)
	@Column(name = "is_order_send", nullable = false, columnDefinition = "ENUM('y','n') default 'n'")
	@Builder.Default
	private YnType isOrderSend = YnType.n;        // y / n
	
	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType isVisible = YnType.y;          // y / n
	
	// ============ 날짜 / 일시 ============
	
	@Column(name = "btrip_start_date")
	private LocalDateTime btripStartDate;  // 출장 시작일시 (nullable)
	
	@Column(name = "btrip_end_date")
	private LocalDateTime btripEndDate;    // 출장 종료일시 (nullable)
	
	@Column(name = "order_date", nullable = false)
	private LocalDate orderDate;   // 접수일시
	
	@Column(name = "complete_date")
	private LocalDate completeDate;        // 완료일자 (nullable)
	
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime;  // 생성일시
	
	@Column(name = "update_datetime", insertable = false, updatable = false)
	private LocalDateTime updateDatetime;  // 수정일시 (nullable)
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;  // 삭제일시 (nullable)
	
	// ============ 번호 / 그룹번호 ============
	
	@Column(name = "order_num", length = 100)
	private String orderNum;               // 접수번호 (UNIQUE, nullable)
	
	@Column(name = "order_group_num", length = 100)
	private String orderGroupNum;          // 접수그룹번호 (nullable)
	
	// ============ 신청업체 쪽 정보 ============
	
	@Column(name = "cust_agent", length = 200)
	private String custAgent;              // 신청업체명
	
	@Column(name = "cust_agent_tel", length = 100)
	private String custAgentTel;              // 신청업체 대표 연락처
	
	@Column(name = "cust_agent_fax", length = 100)
	private String custAgentFax;              // 신청업체 FAX
	
	@Column(name = "cust_agent_en", length = 200)
	private String custAgentEn;           // 신청업체 영문명
	
	@Column(name = "cust_agent_addr", length = 250)
	private String custAgentAddr;         // 신청업체 주소
	
	@Column(name = "cust_agent_addr_en", length = 250)
	private String custAgentAddrEn;      // 신청업체 주소(영문)
	
	@Column(name = "cust_manager", length = 50)
	private String custManager;           // 신청업체 담당자
	
	@Column(name = "cust_manager_tel", length = 100)
	private String custManagerTel;       // 신청업체 담당자 연락처
	
	@Column(name = "cust_manager_email", length = 100)
	private String custManagerEmail;     // 신청업체 담당자 이메일
	
	// ============ 성적서 발행처 정보 ============
	
	@Column(name = "report_agent", length = 100)
	private String reportAgent;          // 성적서발행처
	
	@Column(name = "report_agent_en", length = 100)
	private String reportAgentEn;       // 성적서발행처(영문명)
	
	@Column(name = "report_agent_addr", length = 200)
	private String reportAgentAddr;     // 성적서발행처 주소
	
	@Column(name = "report_agent_addr_en", length = 200)
	private String reportAgentAddrEn;   // 성적서발행처 주소(영문)
	
	@Column(name = "report_manager", length = 50)
	private String reportManager;       // 성적서발행처 담당자
	
	@Column(name = "report_manager_tel", length = 50)
	private String reportManagerTel;    // 성적서발행처 담당자 연락처
	
	@Column(name = "report_manager_email", length = 100)
	private String reportManagerEmail;  // 성적서발행처 담당자 이메일
	
	// ============ 소재지 / 비고 ============
	
	@Column(name = "site_addr", length = 200)
	private String siteAddr;            // 실제 교정이 진행되는 소재지
	
	@Column(name = "site_addr_en", length = 200)
	private String siteAddrEn;          // 소재지(영문)
	
	@Column(name = "remark", columnDefinition = "TEXT")
	private String remark;              // 비고
}
