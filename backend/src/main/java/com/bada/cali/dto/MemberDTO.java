package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.repository.projection.GetMemberInfoPr;
import com.bada.cali.repository.projection.MemberCodeAuthPr;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public class MemberDTO {
	
	// MemberDTO 클래스는 네임스페이스 용도이므로 생성방지
	private MemberDTO() {
	}
	
	;
	
	// 회원가입 사업자번호 중복검사
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DuplicateLoginIdRes {
		private boolean isDuplicate;
		private String agentName;
		private String agentAddr;
	}
	
	
	// 회원가입 요청 DTO
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MemberJoinReq {
		// 사용자계정(업체) & 업체정보(Agent)의 사업자번호(agent_num)
		private String loginId;
		
		// 업체명(agent)
		private String name;
		
		// 업체(agent) 우편번호
		private String agentZipCode;
		
		// 업체(agent) 주소
		private String addr;
		
		// 사용자(member) 비밀번호 (암호화 후 DB에 삽입)
		private String pwd;
		
		// 업체담당자명(agent) & 업체담당자(agent_manager)의 담당자명
		private String manager;
		
		// 업체대표명(agent)
		private String ceo;
		
		// 담당자(agent, agent_manager)
		private String email;
		
		// 담당자 연락처(agent, agent_manager)
		private String phone;
		
		// 업체번호(agent)
		private String agentTel;
	}
	
	// 회원가입 응답(response) DTO
	@Getter
	@AllArgsConstructor
//	@Builder 필드 2개만 있는 경우, 굳이 builder패턴보다는 new연산자로 생성해서 반환할 것
	public static class MemberJoinRes {
		private final int code;        // 생성시점에 불변에 가깝게 동작시키기 위해 final & @Setter 미선언
		private final String msg;
	}
	
	
	// 직원관리 토스트 그리드 dto
	@Setter
	@Getter
	@NoArgsConstructor
	public static class GetMemberListReq extends TuiGridDTO.Request {
		Integer workType;        // 재직여부 (0: 재직, 1: 휴직, 2: 퇴사)
		String searchType;        // 검색타입
		String keyword;            // 검색 키워드
	}
	
	
	// 직원정보 등록 데이터셋 (직원 수정)
	public record SaveMemberInfo(
			Long id,
			String loginId,          // name="loginId"
			String name,              // name="name"
			String nameEng,           // name="nameEng"
			String hp,                // name="hp"
			String companyNo,         // name="companyNo"
			LocalDate birth,          // name="birth"
			String addr1,             // name="addr1"
			String addr2,             // name="addr2"
			String email,             // name="email"
			String tel,               // name="tel"
			Long departmentId,        // name="departmentId"  (select)
			Long levelId,             // name="levelId"       (select)
			YnType isActive,          // name="isActive"      (y/n)
			String pwd,               // name="pwd"
			// 재직/상태
			LocalDate joinDate,       // name="joinDate"
			LocalDate leaveDate,      // name="leaveDate"
			Integer workType,         // name="workType"      (0/1/2)
			// 비고
			String remark,            // name="remark"
			
			// 성적서 이미지 파일
			MultipartFile memberImage, // name="memberImage"
			// 중분류 권한
			List<MemberAuthData> itemAuthData
	) {
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class MemberAuthData {
		Long memberId;
		Long middleItemCodeId;    // 중분류코드 id(클라이언트에서 넘어옴)
		Integer authBitmask;    // 비트값 (클라이언트에서 넘어옴)
	}
	
	// // 직원정보 조회용
	// @Setter
	// @NoArgsConstructor
	// public static class GetMemberInfo {
	// 	String loginId;         // name="loginId"
	// 	String name;              // name="name"
	// 	String nameEng;           // name="nameEng"
	// 	String hp;                // name="hp"
	// 	String companyNo;         // name="companyNo"
	// 	LocalDate birth;          // name="birth"
	// 	String addr1;          // name="addr1"
	// 	String addr2;             // name="addr2"
	// 	String email;             // name="email"
	// 	String tel;               // name="tel"
	// 	Long departmentId;        // name="departmentId"  (select)
	// 	Long levelId;             // name="levelId"       (select)
	// 	YnType isActive;          // name="isActive"      (y/n)
	// 	// 재직/상태
	// 	LocalDate joinDate;       // name="joinDate"
	// 	LocalDate leaveDate;      // name="leaveDate"
	// 	Integer workType;         // name="workType"      (0/1/2)
	// 	// 비고
	// 	String remark;            // name="remark"
	// 	// 이미지 파일 경로
	// 	Long imgFileId;
	// }
	
	// 조합( record + class)
	public record GetMemberInfoSet(
			GetMemberInfoPr basicMemberInfo,
			String memberImgPath,
			List<MemberCodeAuthPr> itemAuthData        // 중분류 권한
	) {
	
	}
	
	
}
