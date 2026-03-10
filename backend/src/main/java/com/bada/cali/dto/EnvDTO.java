package com.bada.cali.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class EnvDTO {

	@Schema(description = "회사정보 입력폼 저장 요청")
	public record EnvUpdateReq(
			@Schema(description = "회사명") String name,
			@Schema(description = "회사명(영문)") String nameEn,
			@Schema(description = "대표자") String ceo,
			@Schema(description = "전화번호") String tel,
			@Schema(description = "FAX") String fax,
			@Schema(description = "휴대폰연락처") String hp,
			@Schema(description = "주소") String addr,
			@Schema(description = "주소(영문)") String addrEn,
			@Schema(description = "이메일") String email,
			@Schema(description = "성적서발행처주소") String reportIssueAddr,
			@Schema(description = "성적서발행처주소(영문)") String reportIssueAddrEn,
			@Schema(description = "소재지주소") String siteAddr,
			@Schema(description = "소재지주소(영문)") String siteAddrEn,
			@Schema(description = "사업자등록번호") String agentNum,
			@Schema(description = "거래은행(계좌번호)") String backAccount
	) {}

	@Schema(description = "회사정보 조회 응답")
	public record EnvRes(
			@Schema(description = "회사명") String name,
			@Schema(description = "회사명(영문)") String nameEn,
			@Schema(description = "대표자") String ceo,
			@Schema(description = "전화번호") String tel,
			@Schema(description = "FAX") String fax,
			@Schema(description = "휴대폰연락처") String hp,
			@Schema(description = "주소") String addr,
			@Schema(description = "주소(영문)") String addrEn,
			@Schema(description = "이메일") String email,
			@Schema(description = "성적서발행처주소") String reportIssueAddr,
			@Schema(description = "성적서발행처주소(영문)") String reportIssueAddrEn,
			@Schema(description = "소재지주소") String siteAddr,
			@Schema(description = "소재지주소(영문)") String siteAddrEn,
			@Schema(description = "사업자등록번호") String agentNum,
			@Schema(description = "거래은행(계좌번호)") String backAccount,
			@Schema(description = "KOLAS 이미지 URL (오브젝트 스토리지 전체 경로)") String kolasUrl,
			@Schema(description = "아일락 이미지 URL (오브젝트 스토리지 전체 경로)") String ilacUrl,
			@Schema(description = "사내 로고 이미지 URL (오브젝트 스토리지 전체 경로)") String companyUrl
	) {}

}