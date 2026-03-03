package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Menu;
import com.bada.cali.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuQueryService {

	private final MenuRepository menuRepository;

	// 전체 visible 메뉴를 depth/sortOrder 순으로 조회하여 캐시
	// GlobalViewNameAdvice와 MenuAccessInterceptor에서 공유하는 단일 캐시
	// 캐시 키: 인자 없음 → 고정 단일 엔트리
	@Cacheable("allVisibleMenus")
	@Transactional(readOnly = true)
	public List<Menu> getAllVisibleMenus() {
		return menuRepository.findByIsVisibleOrderByDepthAscSortOrderAsc(YnType.y);
	}

	// 메뉴 데이터 변경 시 호출하여 캐시 무효화
	// 메뉴 CRUD 서비스 메서드에 @CacheEvict 또는 이 메서드를 직접 호출
	@CacheEvict(value = "allVisibleMenus", allEntries = true)
	public void evictMenuCache() {
	}

}