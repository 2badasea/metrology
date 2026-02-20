package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.repository.projection.GetMemberInfoPr;
import com.bada.cali.repository.projection.MemberCodeAuthPr;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
	@Schema(description = "사업자번호 중복 확인 응답")
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DuplicateLoginIdRes {
		@Schema(description = "중복 여부 (true: 이미 사용 중)", example = "false")
		private boolean isDuplicate;
		@Schema(description = "중복 시 등록된 업체명 (memberJoin 요청 시에만 반환)", example = "바다정밀")
		private String agentName;
		@Schema(description = "중복 시 등록된 업체 주소 (memberJoin 요청 시에만 반환)", example = "서울시 강남구")
		private String agentAddr;
	}
	
	
	// 회원가입 요청 DTO
	@Schema(description = "회원가입 요청")
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MemberJoinReq {
		// 사용자계정(업체) & 업체정보(Agent)의 사업자번호(agent_num)
		@Schema(description = "사업자번호 (하이픈 포함 12자리)", example = "123-45-67890")
		@NotBlank(message = "사업자번호를 입력해주세요.")
		@Size(min = 12, max = 12, message = "사업자번호는 12자리여야 합니다.")
		private String loginId;

		// 업체명(agent)
		@Schema(description = "업체명", example = "바다정밀")
		@NotBlank(message = "업체명을 입력해주세요.")
		private String name;

		// 업체(agent) 우편번호
		@Schema(description = "우편번호", example = "06234")
		private String agentZipCode;

		// 업체(agent) 주소
		@Schema(description = "업체 주소", example = "서울시 강남구 테헤란로 123")
		private String addr;

		// 사용자(member) 비밀번호 (암호화 후 DB에 삽입)
		@Schema(description = "비밀번호 (8~16자, 영어 소문자·대문자·숫자 포함)", example = "Password1")
		@NotBlank(message = "비밀번호를 입력해주세요.")
		@Size(min = 8, max = 16, message = "비밀번호는 8~16자리여야 합니다.")
		@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "비밀번호는 영어 소문자, 대문자, 숫자를 모두 포함해야 합니다.")
		private String pwd;

		// 업체담당자명(agent) & 업체담당자(agent_manager)의 담당자명
		@Schema(description = "담당자명", example = "홍길동")
		@NotBlank(message = "담당자명을 입력해주세요.")
		private String manager;

		// 업체대표명(agent)
		@Schema(description = "대표자명", example = "김대표")
		private String ceo;

		// 담당자(agent, agent_manager)
		@Schema(description = "담당자 이메일", example = "hong@example.com")
		@NotBlank(message = "이메일 주소를 입력해주세요.")
		private String email;

		// 담당자 연락처(agent, agent_manager)
		@Schema(description = "담당자 휴대폰번호", example = "010-1234-5678")
		@NotBlank(message = "휴대폰번호를 입력해주세요.")
		private String phone;

		// 업체번호(agent)
		@Schema(description = "업체 대표전화", example = "02-1234-5678")
		private String agentTel;
	}
	
	// 회원가입 응답(response) DTO
	@Schema(description = "회원가입 응답")
	@Getter
	@AllArgsConstructor
//	@Builder 필드 2개만 있는 경우, 굳이 builder패턴보다는 new연산자로 생성해서 반환할 것
	public static class MemberJoinRes {
		@Schema(description = "처리 결과 코드 (1: 성공, -1: 실패)", example = "1")
		private final int code;        // 생성시점에 불변에 가깝게 동작시키기 위해 final & @Setter 미선언
		@Schema(description = "처리 결과 메시지", example = "회원가입 신청이 완료되었습니다.")
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
	
	
	// 직원 등록 요청 DTO
	public record CreateMemberReq(
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

	// 직원 수정 요청 DTO (loginId 변경 불가, id는 path variable로 수신)
	public record UpdateMemberReq(
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
			String pwd,               // name="pwd" (선택, 값 없으면 변경 안 함)
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
