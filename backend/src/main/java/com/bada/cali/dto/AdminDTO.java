package com.bada.cali.dto;

import java.util.List;

public class AdminDTO {

	// 네임스페이스 역할이므로 생성 방지
	private AdminDTO() {}

	/**
	 * 세션 유효성 확인 응답 DTO
	 * GET /api/admin/session
	 *
	 * @param id       회원 고유 id
	 * @param username 로그인 아이디 (사업자번호)
	 * @param name     이름
	 * @param roles    보유 권한 목록 (예: ["ROLE_ADMIN"])
	 */
	public record SessionRes(
			Long id,
			String username,
			String name,
			List<String> roles
	) {}

}
