package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.FileInfoDTO;
import com.bada.cali.dto.SampleDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.SampleReportWriteRow;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.SampleServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "샘플관리", description = "샘플(기기) 목록·파일 관리 API")
@RestController("ApiSampleController")
@RequestMapping("/api/sample")
@Log4j2
@RequiredArgsConstructor
public class SampleController {

	private final SampleServiceImpl sampleService;

	@Operation(summary = "샘플 목록 조회", description = "검색 조건으로 샘플 목록을 페이지네이션하여 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/list")
	public TuiGridDTO.Res<?> getSampleList(@ModelAttribute SampleDTO.GetListReq req) {
		return sampleService.getSampleList(req);
	}

	@Operation(summary = "성적서작성 모달 샘플 목록 조회",
			description = "소분류 코드 기준으로 해당 소분류의 샘플 파일 목록 전체 조회. 페이지네이션 없이 전체 반환하며 프론트에서 스크롤로 표시")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/reportWriteList")
	public ResponseEntity<ResMessage<List<SampleReportWriteRow>>> getReportWriteList(
			@ModelAttribute SampleDTO.GetReportWriteListReq req) {
		return ResponseEntity.ok(new ResMessage<>(1, "조회 성공", sampleService.getReportWriteList(req)));
	}

	@Operation(summary = "샘플 파일 목록 조회", description = "특정 샘플에 연결된 파일 목록을 최신순으로 조회")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "샘플 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/{sampleId}/files")
	public ResponseEntity<ResMessage<List<FileInfoDTO.FileListRes>>> getSampleFiles(@PathVariable Long sampleId) {
		return ResponseEntity.ok(new ResMessage<>(1, "조회 성공", sampleService.getSampleFiles(sampleId)));
	}

	@Operation(summary = "중복 확인", description = "중분류 + 소분류 + 기기명 조합으로 동일한 샘플 존재 여부 확인")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "확인 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/checkDuplicate")
	public ResponseEntity<ResMessage<SampleDTO.DuplicateCheckRes>> checkDuplicate(
			@RequestParam Long middleItemCodeId,
			@RequestParam Long smallItemCodeId,
			@RequestParam String name
	) {
		return ResponseEntity.ok(new ResMessage<>(1, "확인 성공",
				sampleService.checkDuplicate(middleItemCodeId, smallItemCodeId, name)));
	}

	@Operation(summary = "샘플 등록", description = "새 샘플 등록. 파일(Excel)은 선택 항목")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "등록 성공"),
			@ApiResponse(responseCode = "400", description = "요청 파라미터 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResMessage<Long>> createSample(
			@RequestPart("data") SampleDTO.SampleSaveReq req,
			@RequestPart(value = "file", required = false) MultipartFile file,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ResMessage<>(1, "등록 성공", sampleService.createSample(req, file, user)));
	}

	@Operation(summary = "샘플 수정", description = "기존 샘플 정보 수정 및 파일 추가")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "404", description = "샘플 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PatchMapping(value = "/{sampleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResMessage<Void>> updateSample(
			@PathVariable Long sampleId,
			@RequestPart("data") SampleDTO.SampleSaveReq req,
			@RequestPart(value = "file", required = false) MultipartFile file,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		sampleService.updateSample(sampleId, req, file, user);
		return ResponseEntity.ok(new ResMessage<>(1, "수정 성공", null));
	}

	@Operation(summary = "샘플 일괄 삭제", description = "선택된 샘플을 소프트 삭제. 연관 파일도 함께 소프트 삭제. ADMIN 권한 필요")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@DeleteMapping
	public ResponseEntity<ResMessage<Void>> deleteSamples(
			@RequestBody SampleDTO.DeleteReq req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		sampleService.deleteSamples(req, user);
		return ResponseEntity.ok(new ResMessage<>(1, "삭제 성공", null));
	}

	@Operation(summary = "샘플 파일 삭제", description = "특정 파일을 소프트 삭제. ADMIN 권한 필요")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "404", description = "파일 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@DeleteMapping("/files/{fileId}")
	public ResponseEntity<ResMessage<Void>> deleteSampleFile(
			@PathVariable Long fileId,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		sampleService.deleteSampleFile(fileId, user);
		return ResponseEntity.ok(new ResMessage<>(1, "삭제 성공", null));
	}
}
