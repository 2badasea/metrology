package com.bada.cali.entity;

import com.bada.cali.common.enums.AuthType;
import com.bada.cali.common.enums.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor               // 매개변수 없는 기본 생성자
@AllArgsConstructor              // 모든 필드를 파라미터로 받는 생성자
@Builder                        // 빌더패턴 자동 생성. (NOTATION 4번)
public class Member {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // member 고유번호
	
	@Column(name = "login_id", length = 20, nullable = false)
	private String loginId;
	
	@Column(name = "pwd", length = 255, nullable = false)
	private String pwd;
	
	@Column(name = "agent_id", nullable = false)
	@Builder.Default
	private Long agentId = 0L;
	
	@Column(name = "name", length = 100, nullable = false)
	private String name;
	
	@Column(name = "name_eng", length = 100, nullable = false)
	private String nameEng;
	
	@Column(name = "birth", length = 10)
	private String birth;
	
	// 사번
	@Column(name = "company_no", length = 30)
	private String companyNo;
	
	// 휴대폰
	@Column(name = "hp", length = 20)
	private String hp;
	
	@Column(name = "email", length = 125)
	private String email;
	
	// 전화번호
	@Column(name = "tel", length = 20)
	private String tel;
	
	// 주소
	@Column(name = "addr1", length = 100)
	private String addr1;
	
	@Column(name = "addr2", length = 100)
	private String addr2;
	
	// 비고
	@Lob
	@Column(name = "remark")
	private String remark;
	
	// 직무
	@Column(name = "task_id", nullable = false)
	@Builder.Default
	private Long taskId = 0L;
	
	// 부서
	@Column(name = "department_id", nullable = false)
	@Builder.Default
	private Long departmentId = 0L;
	
	// 직급
	@Column(name = "level_id", nullable = false)
	@Builder.Default
	private Long levelId = 0L;
	
	// 입사일
	@Column(name = "join_date", length = 20)
	private LocalDate joinDate;
	
	// 퇴사일
	@Column(name = "leave_date", length = 20)
	private LocalDate leaveDate;
	
	// 재직유형
	@Column(name = "work_type", nullable = false)
	@Builder.Default
	private Byte workType = 0;
	
	// 급여타입
	@Column(name = "salary_type", nullable = false)
	@Builder.Default
	private Byte salaryType = 0;
	
	// 실무자 & 기술책임자
	@Column(name = "calbr_manager_type", nullable = false)
	@Builder.Default
	private Byte calbrManagerType = 0;
	
	// 서명이미지(파일명)
	@Column(name = "img", length = 255)
	private String img;
	
	// 품질문서 권한
	@Column(name = "doc_permisson", length = 255)
	private String docPermisson;
	
	// 유저 권한
	@Enumerated(EnumType.STRING)
	@Column(name = "auth", columnDefinition = "ENUM('admin','user') default 'user'")
	@Builder.Default
	private AuthType auth = AuthType.user;
	
	// 로그인 횟수
	@Column(name = "login_count", nullable = false)
	@Builder.Default
	private Integer loginCount = 0;
	
	// 모바일 접근가능여부
	@Enumerated(EnumType.STRING)
	@Column(name = "mobile_access", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType mobileAccess = YnType.y;
	
	// ip 접근차단여부
	@Enumerated(EnumType.STRING)
	@Column(name = "ip_access", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType ipAccess = YnType.y;
	
	// 남은 연차
	@Column(name = "day_off_total", nullable = false, precision = 5, scale = 1)
	@Builder.Default
	private BigDecimal dayOffTotal = BigDecimal.ZERO;
	
	// 삭제여부
	@Enumerated(EnumType.STRING)
	@Column(name = "is_visible", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType isVisible = YnType.y;
	
	// 아이디 활성화 여부
	@Enumerated(EnumType.STRING)
	@Column(name = "is_active", nullable = false, columnDefinition = "ENUM('y','n') default 'n'")
	@Builder.Default
	private YnType isActive = YnType.n;    // 로그인 가능여부 (기본값이 y)
	
	// 도래알람여부
	@Enumerated(EnumType.STRING)
	@Column(name = "dorae_alarm", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType doraeAlarm = YnType.y;
	
	// 표준장비 도래알림여부
	@Enumerated(EnumType.STRING)
	@Column(name = "equipage_dorae_alarm", nullable = false, columnDefinition = "ENUM('y','n') default 'y'")
	@Builder.Default
	private YnType equipageDoraeAlarm = YnType.y;
	
	// 마지막 비밀번호 변경일시
	@Column(name = "last_pwd_updated", nullable = true)
	private LocalDateTime lastPwdUpdated;
	
	// 마지막 로그인 일시
	@Column(name = "last_login_datetime", nullable = true)
	private LocalDateTime lastLoginDatetime;
	
	// 마지막 로그인 실패일시
	@Column(name = "last_login_fail_datetime", nullable = true)
	private LocalDateTime lastLoginFailDatetime;
	
	@Column(name = "create_datetime", nullable = false)
	private LocalDateTime createDatetime;
	
	@Column(name = "create_member_id", nullable = false)
	@Builder.Default
	private Long createMemberId = 0L;
	
	@Column(name = "update_datetime", insertable = false, updatable = false)
	private LocalDateTime updateDatetime;
	
	@Column(name = "update_member_id", nullable = false)
	@Builder.Default
	private Long updateMemberId = 0L;
	
	@Column(name = "delete_datetime", nullable = true)
	private LocalDateTime deleteDatetime;
	
	@Column(name = "delete_member_id", nullable = false)
	@Builder.Default
	private Long deleteMemberId = 0L;
	
	// 비밀번호 업데이트 (더티체킹용으로만 사용할 것)
	public void updatePwd(String pwd) {
		this.pwd = pwd;
	}
	
}
