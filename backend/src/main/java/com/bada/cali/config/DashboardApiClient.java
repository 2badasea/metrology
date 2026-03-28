package com.bada.cali.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 대시보드(park302-dashboard) API 연동 클라이언트
 *
 * 모든 요청에 X-Api-Key 헤더를 자동 주입한다.
 * app.dashboard.url이 비어 있으면 비활성화(개발 모드)로 동작하며,
 * isEnabled() == false인 상태에서 메서드를 호출하면 IllegalStateException이 발생한다.
 */
@Log4j2
@Component
public class DashboardApiClient {

    /** 대시보드 베이스 URL. 비어 있으면 연동 비활성화. */
    @Value("${app.dashboard.url:}")
    private String dashboardUrl;

    /** cali → 대시보드 API 인증 키 (X-Api-Key 헤더에 자동 주입) */
    @Value("${app.dashboard.api-key:}")
    private String apiKey;

    private RestClient restClient;
    private boolean enabled = false;

    @PostConstruct
    private void init() {
        if (dashboardUrl == null || dashboardUrl.isBlank()) {
            log.warn("[DashboardApiClient] app.dashboard.url 미설정 — 대시보드 연동 비활성화 (개발 모드)");
            return;
        }
        restClient = RestClient.builder()
                .baseUrl(dashboardUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
        enabled = true;
        log.info("[DashboardApiClient] 초기화 완료: baseUrl={}", dashboardUrl);
    }

    /** 연동 활성화 여부 */
    public boolean isEnabled() {
        return enabled;
    }

    // ─── 문의(work) ───────────────────────────────────────────────────────────

    /**
     * 문의 목록 조회
     * GET /api/external/works
     */
    public Object getWorks() {
        return restClient.get()
                .uri("/api/external/works")
                .retrieve()
                .body(Object.class);
    }

    /**
     * 문의 단건 상세 조회
     * GET /api/external/works/{id}
     */
    public Object getWorkDetail(Long id) {
        return restClient.get()
                .uri("/api/external/works/{id}", id)
                .retrieve()
                .body(Object.class);
    }

    /**
     * 문의 등록 (multipart 중계)
     * POST /api/external/works
     *
     * 브라우저 → cali → 대시보드 방향의 multipart/form-data 중계.
     * RestClient의 MultipartBodyBuilder로 파트를 재조립하여 전송한다.
     *
     * @param category        문의 유형 (BUG / ERROR / NEW_FEATURE / INQUIRY)
     * @param priorityByAgent 업체 지정 중요도 (NORMAL / EMERGENCY)
     * @param title           제목
     * @param content         본문 (마크다운)
     * @param files           첨부파일 목록 (없으면 null 또는 빈 리스트)
     */
    public Object createWork(String category, String priorityByAgent,
                             String title, String content,
                             List<MultipartFile> files) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("category",        category);
        builder.part("priorityByAgent", priorityByAgent);
        builder.part("title",           title);
        builder.part("content",         content);

        if (files != null) {
            for (MultipartFile f : files) {
                MediaType mediaType = f.getContentType() != null
                        ? MediaType.parseMediaType(f.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM;
                builder.part("files", f.getResource())
                        .filename(f.getOriginalFilename() != null ? f.getOriginalFilename() : "file")
                        .contentType(mediaType);
            }
        }

        return restClient.post()
                .uri("/api/external/works")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(Object.class);
    }

    /**
     * 댓글 등록
     * POST /api/external/works/{id}/comments
     */
    public Object addComment(Long workId, String content) {
        return restClient.post()
                .uri("/api/external/works/{id}/comments", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", content))
                .retrieve()
                .body(Object.class);
    }

    // ─── 공지사항(board-notice) ───────────────────────────────────────────────

    /**
     * 공지사항 목록 조회
     * GET /api/external/board-notices
     */
    public Object getNotices() {
        return restClient.get()
                .uri("/api/external/board-notices")
                .retrieve()
                .body(Object.class);
    }
}
