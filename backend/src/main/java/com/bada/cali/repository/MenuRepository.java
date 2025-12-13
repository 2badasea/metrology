package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
	
	// isVisible = 'y' 이면서, 로그인한 유저가 접근 가능한 메뉴만 depth, sort_order 순으로 조회
	List<Menu> findByIsVisibleAndIdInOrderByDepthAscSortOrderAsc(YnType isVisible,
																 Collection<Long> ids);
}
