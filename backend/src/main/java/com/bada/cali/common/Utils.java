package com.bada.cali.common;

import jakarta.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;

public class Utils {

	private static final DateTimeFormatter DEL_FMT =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	/**
	 * 클라이언트 IP 추출 (리버스 프록시 환경 대응)
	 * Nginx 등에서 X-Forwarded-For 헤더를 설정해야 실제 클라이언트 IP를 받을 수 있음
	 */
	public static String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
			// "client, proxy1, proxy2" 형태일 수 있으므로 첫 번째 값 사용
			return ip.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
