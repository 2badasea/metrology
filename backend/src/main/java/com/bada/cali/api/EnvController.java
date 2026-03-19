package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.EnvDTO;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.EnvServiceImpl;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "어드민 - 회사정보", description = "회사정보 조회/수정 API (어드민 전용)")
@RestController
@RequestMapping("/api/admin/env")
@RequiredArgsConstructor
@Log4j2
public class EnvController {

	private final EnvServiceImpl envService;

	// ──────────────────────────────── 조회 ────────────────────────────────

	@Operation(summary = "회사정보 조회", description = "단일 row로 관리되는 회사정보 전체 조회. 데이터 없으면 data=null 반환")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping
	public ResponseEntity<ResMessage<EnvDTO.EnvRes>> getEnv() {
		EnvDTO.EnvRes res = envService.getEnv();
		return ResponseEntity.ok(new ResMessage<>(1, null, res));
	}

	// ──────────────────────────────── 입력폼 저장 ────────────────────────────────

	@Operation(summary = "회사정보 입력폼 저장", description = "회사 기본정보(텍스트 필드) 수정. 이미지와 별개로 처리됨")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "저장 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PatchMapping
	public ResponseEntity<ResMessage<Void>> updateEnv(
			@RequestBody EnvDTO.EnvUpdateReq req,
			@AuthenticationPrincipal CustomUserDetails user,
			HttpServletRequest httpReq) {
		envService.updateEnv(req, user, httpReq);
		return ResponseEntity.ok(new ResMessage<>(1, "저장되었습니다.", null));
	}

	// ──────────────────────────────── 성적서시트 설정 조회 ────────────────────────────────

	@Operation(summary = "성적서시트 설정 조회",
			description = "env 테이블에 저장된 성적서시트 셀위치·형식 설정 조회. 미설정이면 빈 맵 반환")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/sheetSetting")
	public ResponseEntity<ResMessage<EnvDTO.SheetInfoSettingRes>> getSheetInfoSetting() {
		EnvDTO.SheetInfoSettingRes res = envService.getSheetInfoSetting();
		return ResponseEntity.ok(new ResMessage<>(1, null, res));
	}

	// ──────────────────────────────── 성적서시트 설정 저장 ────────────────────────────────

	@Operation(summary = "성적서시트 설정 저장",
			description = "항목코드 → 셀 주소·형식 맵을 env.sheet_info_setting에 JSON으로 저장. " +
					"셀 주소 형식 검증은 프론트에서 사전 수행")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "저장 성공"),
			@ApiResponse(responseCode = "400", description = "요청 본문 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PatchMapping("/sheetSetting")
	public ResponseEntity<ResMessage<Void>> updateSheetInfoSetting(
			@RequestBody EnvDTO.SheetInfoSettingUpdateReq req,
			@AuthenticationPrincipal CustomUserDetails user,
			HttpServletRequest httpReq) {
		envService.updateSheetInfoSetting(req.settings(), user, httpReq);
		return ResponseEntity.ok(new ResMessage<>(1, "저장되었습니다.", null));
	}

	// ──────────────────────────────── 이미지 저장 ────────────────────────────────

	@Operation(summary = "회사정보 이미지 저장",
			description = "지정 필드(kolas·ilac·company) 이미지를 Object Storage에 업로드하고 DB 경로 갱신. " +
					"기존 이미지가 있으면 교체됨")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "저장 성공"),
			@ApiResponse(responseCode = "413", description = "파일 크기 초과 (10MB 한도)",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping("/images/{field}")
	public ResponseEntity<ResMessage<Void>> saveEnvImage(
			@Parameter(description = "이미지 필드명 (kolas | ilac | company)") @PathVariable String field,
			@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal CustomUserDetails user,
			HttpServletRequest httpReq) {
		envService.saveEnvImage(field, file, user, httpReq);
		return ResponseEntity.ok(new ResMessage<>(1, "이미지가 저장되었습니다.", null));
	}

	// ──────────────────────────────── 이미지 삭제 ────────────────────────────────

	@Operation(summary = "회사정보 이미지 삭제",
			description = "지정 필드(kolas·ilac·company) 이미지를 Object Storage에서 삭제하고 DB 경로를 null 처리")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@DeleteMapping("/images/{field}")
	public ResponseEntity<ResMessage<Void>> deleteEnvImage(
			@Parameter(description = "이미지 필드명 (kolas | ilac | company)") @PathVariable String field,
			@AuthenticationPrincipal CustomUserDetails user,
			HttpServletRequest httpReq) {
		envService.deleteEnvImage(field, user, httpReq);
		return ResponseEntity.ok(new ResMessage<>(1, "이미지가 삭제되었습니다.", null));
	}

}