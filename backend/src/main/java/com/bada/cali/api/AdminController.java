package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.AdminDTO;
import com.bada.cali.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin SPA 공통 API 컨트롤러
 * - /api/admin 베이스 경로 전용
 * - 특정 도메인에 속하지 않는 admin 공통 기능을 담당
 */
@Tag(name = "어드민 - 공통", description = "Admin SPA 공통 API")
@RequestMapping("/api/admin")
@RestController("ApiAdminController")
@Log4j2
public class AdminController {

	/**
	 * Admin SPA 초기화 시 세션 유효성 확인용 엔드포인트.
	 * 미인증 상태로 호출하면 unauthenticatedEntryPoint가 401 JSON을 반환하고,
	 * React 앱은 이를 감지하여 백엔드 로그인 페이지로 리다이렉트한다.
	 */
	@Operation(
			summary = "세션 유효성 확인",
			description = "Admin SPA 초기 로드 시 로그인 상태를 확인. 미인증 시 Security 필터가 401 JSON 반환."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "세션 유효 (로그인 상태)"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/session")
	public ResponseEntity<ResMessage<AdminDTO.SessionRes>> getSession(
			@AuthenticationPrincipal CustomUserDetails user
	) {
		List<String> roles = user.getAuthorities().stream()
				.map(a -> a.getAuthority())
				.toList();

		AdminDTO.SessionRes res = new AdminDTO.SessionRes(
				user.getId(), user.getUsername(), user.getName(), roles
		);
		return ResponseEntity.ok(new ResMessage<>(1, null, res));
	}

}