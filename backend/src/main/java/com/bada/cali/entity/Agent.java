package com.bada.cali.entity;

import com.bada.cali.common.enums.CalibrationCycleType;
import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 업체 정보
@Entity
@Table(name = "agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;        // 고유번호(기본키)
	
	// DB상에서는 enum이지만, String 형으로 값 전달 가능
	@Column(name = "create_type", nullable = false, length = 100)
	@Builder.Default
	private String createType = "basic";
	
	@Column(name = "name", length = 100)
	private String name;                  // 업체명
	
	@Column(name = "name_en", length = 100)
	private String nameEn;                // 업체명 영문
	
	// 업체형태. 업체별로 반드시 값을 가짐. 애플리케이션 단에서 값을 삽입해주어야 함.
	@Column(name = "agent_flag", nullable = false)
	private Integer agentFlag;            // 업체형태(1,2,4,8,16 bit flag)
	
	@Column(name = "ceo", length = 50)
	private String ceo;                   // 대표자
	
	@Column(name = "group_name", length = 100)
	private String groupName;
	
	@Column(name = "agent_num", length = 20)
	private String agentNum;              // 사업자등록번호
	
	@Column(name = "business_type", length = 50)
	private String businessType;          // 업태
	
	@Column(name = "business_kind", length = 50)
	private String businessKind;          // 종목
	
	@Column(name = "agent_type", length = 50)
	private String agentType;             // 형태
	
	@Column(name = "agent_zip_code", length = 50)
	private String agentZipCode;          // 우편번호
	
	@Column(name = "addr", length = 100)
	private String addr;                  // 성적서 발행주소
	
	@Column(name = "addr_en", length = 100)
	private String addrEn;                // 성적서 발행주소 영문
	
	@Column(name = "agent_tel", length = 100)
	private String agentTel;              // 전화번호
	
	@Column(name = "phone", length = 100)
	private String phone;                 // 휴대전화번호
	
	@Column(name = "email", length = 100)
	private String email;                 // 메일
	
	@Column(name = "manager", length = 100)
	private String manager;               // 업체 담당자명
	
	@Column(name = "manager_email", length = 50)
	private String managerEmail;          // 업체 담당자 이메일
	
	@Column(name = "manager_tel", length = 50)
	private String managerTel;            // 업체 담당자 연락처
	
	@Column(name = "fax", length = 100)
	private String fax;                   // 팩스
	
	@Column(name = "account_number", length = 250)
	private String accountNumber;         // 계좌정보
	
	@Lob
	@Column(name = "remark")
	private String remark;                // 업체 특이사항 (LONGTEXT)
	
	// 교정주기 NOT NULL
	@Enumerated(EnumType.STRING)
	@Column(name = "calibration_cycle", nullable = false, length = 20)
	@Builder.Default
	private CalibrationCycleType calibrationCycle = CalibrationCycleType.self_cycle;  // 교정주기
	
	// 'nullable = true'는 명시할 필요가 없음. 기본값이 true이기 때문.
	@Column(name = "self_discount", nullable = true)
	@Builder.Default
	private BigDecimal selfDiscount = BigDecimal.ZERO;   // 자체 할인율
	
	@Column(name = "out_discount", nullable = true)
	@Builder.Default
	private BigDecimal outDiscount = BigDecimal.ZERO;      // 대행 할인율
	
	// NOTATION 8번 (@Builder.Default)
	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType isVisible = YnType.y;  // y:사용, n:미사용
	
	@Enumerated(EnumType.STRING)
	@Column(name = "is_close", nullable = false, columnDefinition = "ENUM('y','n') default 'n'")
	@Builder.Default
	private YnType isClose = YnType.n;    // 폐업 구분
	
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime; // 등록일시
	
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;   // 등록자
	
	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime; // 수정일시
	
	@Column(name = "update_member_id")
	private Long updateMemberId;   // 수정자
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime; // 삭제일시
	
	@Column(name = "delete_member_id")
	private Long deleteMemberId;   // 삭제자
	
}
