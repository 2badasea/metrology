package com.bada.cali.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	// 추가 의존성 없이 spring-context 내장 ConcurrentMapCacheManager 사용
	// TTL 없음 — 메뉴 데이터는 거의 변경되지 않으므로 적합
	// 메뉴 변경 시 MenuQueryService.evictMenuCache() 호출 또는 서버 재시작으로 무효화
	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("allVisibleMenus");
	}

}