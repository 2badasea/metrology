package com.bada.cali.api;

import com.bada.cali.config.DashboardApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 개발팀 문의 API (대시보드 중계)
 *
 * 경로: /api/dev/**
 * 인증: Spring Security 세션 인증 (로그인한 사용자만 접근 가능)
 *
 * 모든 요청을 park302-dashboard 의 /api/external/** 로 중계한다.
 * 대시보드 응답을 그대로 브라우저에 전달하므로 응답 구조는 대시보드 API 계약을 따른다.
 *
 * ⚠️ app.dashboard.url이 미설정(개발 모드)이면 연동 비활성화 메시지를 반환한다.
 */
@Tag(name = "개발팀 문의", description = "개발팀 문의 API (대시보드 중계)")
@RestController("ApiDevWorkController")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Log4j2
public class DevWorkController {

    private final DashboardApiClient dashboardClient;

    // ─── 문의(work) ───────────────────────────────────────────────────────────

    @Operation(summary = "문의 목록 조회",
            description = "로그인 업체의 문의 목록을 대시보드에서 조회하여 반환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 대시보드 연동 실패")
    })
    @GetMapping("/works")
    public ResponseEntity<Object> getWorks() {
        if (!dashboardClient.isEnabled()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "대시보드 연동이 비활성화되어 있습니다."));
        }
        return ResponseEntity.ok(dashboardClient.getWorks());
    }

    @Operation(summary = "문의 상세 조회",
            description = "문의 단건 상세 정보(본문·첨부파일·댓글 포함)를 대시보드에서 조회하여 반환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 대시보드 연동 실패")
    })
    @GetMapping("/works/{id}")
    public ResponseEntity<Object> getWorkDetail(
            @Parameter(description = "문의 ID") @PathVariable Long id) {
        if (!dashboardClient.isEnabled()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "대시보드 연동이 비활성화되어 있습니다."));
        }
        return ResponseEntity.ok(dashboardClient.getWorkDetail(id));
    }

    @Operation(summary = "문의 등록 (multipart)",
            description = "문의를 대시보드에 등록. 첨부파일은 multipart/form-data로 중계되며 " +
                          "대시보드가 NCP 오브젝트 스토리지에 직접 업로드함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 대시보드 연동 실패")
    })
    @PostMapping(value = "/works", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createWork(
            @Parameter(description = "문의 유형 (BUG/ERROR/NEW_FEATURE/INQUIRY)")
            @RequestParam String category,
            @Parameter(description = "중요도 (NORMAL/EMERGENCY)")
            @RequestParam String priorityByAgent,
            @Parameter(description = "제목")
            @RequestParam String title,
            @Parameter(description = "본문 (마크다운)")
            @RequestParam String content,
            @Parameter(description = "첨부파일 목록 (선택)")
            @RequestParam(required = false) List<MultipartFile> files
    ) throws IOException {
        if (!dashboardClient.isEnabled()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "대시보드 연동이 비활성화되어 있습니다."));
        }
        return ResponseEntity.ok(dashboardClient.createWork(category, priorityByAgent, title, content, files));
    }

    @Operation(summary = "댓글 등록",
            description = "문의에 댓글을 등록. 등록 후 대시보드가 cali webhook을 호출하여 알림을 생성함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 대시보드 연동 실패")
    })
    @PostMapping("/works/{id}/comments")
    public ResponseEntity<Object> addComment(
            @Parameter(description = "문의 ID") @PathVariable Long id,
            @RequestBody CommentReq req) {
        if (!dashboardClient.isEnabled()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "대시보드 연동이 비활성화되어 있습니다."));
        }
        return ResponseEntity.ok(dashboardClient.addComment(id, req.content()));
    }

    // ─── 공지사항(board-notice) ───────────────────────────────────────────────

    @Operation(summary = "공지사항 목록 조회",
            description = "개발팀 공지사항 목록을 대시보드에서 조회하여 반환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 대시보드 연동 실패")
    })
    @GetMapping("/notices")
    public ResponseEntity<Object> getNotices() {
        if (!dashboardClient.isEnabled()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "대시보드 연동이 비활성화되어 있습니다."));
        }
        return ResponseEntity.ok(dashboardClient.getNotices());
    }

    // ─── 내부 DTO ─────────────────────────────────────────────────────────────

    /** 댓글 등록 요청 */
    public record CommentReq(String content) {}
}
