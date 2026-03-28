package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.AlarmDTO;
import com.bada.cali.service.AlarmServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 대시보드 → cali webhook 수신 컨트롤러
 *
 * 경로: /api/callback/**
 * 인증: Spring Security 세션 인증 없음.
 *      X-Api-Key 헤더(app.dashboard.callback-key)로 요청 출처를 검증한다.
 *      app.dashboard.callback-key가 비어 있으면 개발 모드로 간주하여 검증을 생략한다.
 *
 * ⚠️ SecurityConfig에서 /api/callback/** 를 permitAll로 설정해야 동작함.
 *    대시보드는 세션 없이 cali에 webhook을 전송하므로 인증 없이 접근 가능해야 한다.
 */
@Tag(name = "대시보드 콜백", description = "대시보드 → cali webhook 수신 API (X-Api-Key 인증, 세션 불필요)")
@RestController("DashboardCallbackController")
@RequestMapping("/api/callback")
@RequiredArgsConstructor
@Log4j2
public class DashboardCallbackController {

    private final AlarmServiceImpl alarmService;

    /** dashboard → cali webhook 인증 키 검증용 */
    @Value("${app.dashboard.callback-key:}")
    private String callbackKey;

    /**
     * 알림 webhook 수신
     *
     * 대시보드에서 댓글·공지 등록 후 cali에 알림 생성을 요청하는 엔드포인트.
     * 수신 후 AlarmServiceImpl.createAlarm()을 호출하여 alarm 레코드를 생성한다.
     */
    @Operation(summary = "알림 webhook 수신",
            description = "대시보드가 댓글/공지 등록 후 cali에 알림 생성을 요청하는 webhook 수신 엔드포인트. " +
                          "X-Api-Key 헤더로 요청 출처를 검증하고, AlarmServiceImpl.createAlarm()을 호출함")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 생성 성공"),
            @ApiResponse(responseCode = "403", description = "X-Api-Key 불일치",
                    content = @Content(schema = @Schema(implementation = ResMessage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ResMessage.class)))
    })
    @PostMapping("/alarm")
    public ResponseEntity<ResMessage<Void>> receiveAlarm(
            @RequestHeader(value = "X-Api-Key", defaultValue = "") String apiKey,
            @RequestBody AlarmDTO.DashboardCallbackReq req
    ) {
        if (!isValidCallbackKey(apiKey)) {
            log.warn("[DashboardCallback] API 키 불일치 — 요청 거부");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResMessage<>(-1, "유효하지 않은 API 키입니다.", null));
        }

        alarmService.createAlarm(
                req.memberId(),
                req.alarmType(),
                req.refType(),
                req.refId(),
                req.content(),
                req.senderName()
        );

        log.info("[DashboardCallback] 알림 생성 완료: alarmType={}, refId={}, memberId={}",
                req.alarmType(), req.refId(), req.memberId());
        return ResponseEntity.ok(new ResMessage<>(1, "알림 생성 완료", null));
    }

    /**
     * callback-key 검증.
     * app.dashboard.callback-key가 비어 있으면 개발 모드로 간주하여 검증을 생략한다.
     */
    private boolean isValidCallbackKey(String apiKey) {
        if (callbackKey == null || callbackKey.isBlank()) {
            log.warn("[DashboardCallback] app.dashboard.callback-key 미설정 — 검증 생략 (개발 모드)");
            return true;
        }
        return callbackKey.equals(apiKey);
    }
}
