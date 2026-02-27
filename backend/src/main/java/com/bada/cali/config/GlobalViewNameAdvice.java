package com.bada.cali.config;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Menu;
import com.bada.cali.repository.MenuRepository;
import com.bada.cali.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.*;

// view를 리턴하는 컨트롤러에서만 동작하도록 패키지 제한
@ControllerAdvice(basePackages = "com.bada.cali.controller")
@RequiredArgsConstructor
@Log4j2
public class GlobalViewNameAdvice {
	private final MenuRepository menuRepository;
	private final ResourceLoader resourceLoader;
	
	// viewName과 renderMode 속성값 기본값으로 추가
	@ModelAttribute
	public void addGlobalPageAttributes(HttpServletRequest request, Model model, @AuthenticationPrincipal CustomUserDetails user) {
		
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		
		// contextPath 제거
		if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
			uri = uri.substring(contextPath.length());
		}
		
		// 쿼리스트링 제거
		int queryIndex = uri.indexOf("?");
		if (queryIndex != -1) {
			uri = uri.substring(0, queryIndex);
		}
		// 앞의 '/' 제거
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		// model에 viewName 속성값 담기
		model.addAttribute("viewName", uri);

		// CSS 파일 존재 여부 확인 (없으면 <link> 태그 자체를 렌더링하지 않음)
		Resource cssResource = resourceLoader.getResource("classpath:static/css/" + uri + ".css");
		model.addAttribute("hasCss", cssResource.exists());
		
		// renderMode 세팅
		String renderMode = request.getParameter("renderMode");
		// model에 담기
		model.addAttribute("renderMode", renderMode);
		
		// 기본 title '교정관리'라고 표시
		model.addAttribute("title", "교정관리 프로젝트");
		
		// 유저
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return;
		}
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserDetails gUser)) {
			return;
		} else {
			boolean isAdmin = gUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> "ROLE_ADMIN".equals(a));
			model.addAttribute("gLoginAuth", isAdmin ? "admin" : "user");
			model.addAttribute("gName", gUser.getName());
			model.addAttribute("gId", gUser.getId());
		}
		
		
		// 조회하는 페이지가 모달이 아닌 경우에만 사이드메뉴를 세팅할 수 있또록 한다.
		if (!"modal".equalsIgnoreCase(renderMode)) {
			
			// =========================== 사이드 메뉴 세팅 ==========================
			if (user == null) {
				return;
			}
			// 로그인 시, 유저객체에 할당한 접근가능 메뉴들을 가져온다.
			Set<Long> readableMenuIds = user.getReadableMenuIds();
			if (readableMenuIds == null || readableMenuIds.isEmpty()) {
				return;
			}
			
			// isVisible  = 'y'이면서, 유저가 접근 가능한 메뉴만 depth/ sort_order 순으로 가져오기
			List<Menu> accessibleMenus = menuRepository.findByIsVisibleAndIdInOrderByDepthAscSortOrderAsc(YnType.y, readableMenuIds);
			// 계층 구조로 변환 (depth, parent_id 기준)
			List<MenuNode> sideMenus = buildMenuTree(accessibleMenus, uri);
			
			// 3) 모델에 담기 (sidebar.html 에서 사용)
			model.addAttribute("sideMenus", sideMenus);
			
		}
		
	}
	
	
	/**
	 * flat 한 Menu 리스트를 계층 구조(MenuNode 트리)로 변환
	 * - menus 는 depth ASC, sortOrder ASC 로 정렬되어 있다고 가정
	 */
	private List<MenuNode> buildMenuTree(List<Menu> menus, String currentPath) {
		Map<Long, MenuNode> nodeMap = new LinkedHashMap<>();
		List<MenuNode> roots = new ArrayList<>();
		
		// 파라미터로 넘어오는uri의 경우, 맨앞 경로('/')가 짤린 상태로 오기 때문에 임의로 붙여서 비교를 한다.
		currentPath = "/" + currentPath;
		
		for (Menu menu : menus) {
			Long id = menu.getId();
			
			// 현재 메뉴에 대한 노드 생성/재사용
			MenuNode current = nodeMap.computeIfAbsent(id, key -> MenuNode.from(menu));
			
			// 현재 URL과 메뉴 url이 같으면 active 표시
			if (menu.getUrl() != null && menu.getUrl().equals(currentPath)) {
				current.setActive(true);
			}
			
			// parent 없으면 depth=1 루트 메뉴
			if (menu.getParent() == null) {
				roots.add(current);
				continue;
			}
			
			// parent 있는 경우 → 부모 노드 찾아서 children 에 추가
			Long parentId = menu.getParent().getId();
			MenuNode parentNode = nodeMap.get(parentId);
			
			// parent 도 이 유저가 접근 가능한 메뉴라고 가정 (member_permission_read 에도 존재)
			// 만약 parent 가 리스트에 없다면, 그냥 루트로 올려버리거나 스킵하는 로직 추가 가능
			if (parentNode == null) {
				// menus 가 depth 기준으로 정렬되어 있다면, 보통 여기 안 탄다.
				parentNode = MenuNode.from(menu.getParent());
				nodeMap.put(parentId, parentNode);
				roots.add(parentNode);
			}
			
			parentNode.getChildren().add(current);
		}
		
		for (MenuNode root : roots) {
			propagateExpanded(root);
		}
		
		return roots;
	}
	
	/**
	 * 자식 중에 active 가 있으면 부모 expanded=true 로
	 */
	private boolean propagateExpanded(MenuNode node) {
		boolean childHasActive = false;
		
		for (MenuNode child : node.getChildren()) {
			if (propagateExpanded(child)) {
				childHasActive = true;
			}
		}
		
		if (node.isActive() || childHasActive) {
			node.setExpanded(true);
			return true;
		}
		return false;
	}
	
	
	/**
	 * 뷰에 넘길 사이드메뉴용 DTO
	 * - 엔티티 직접 넘기지 않고, 필요한 필드만 노출
	 */
	@Getter
	@Setter
	public static class MenuNode {
		// ===== getter (Thymeleaf에서 접근 필요) =====
		private final Long id;
		private final String alias;
		private final String code;
		private final String url;
		private final Integer depth;
		private final Integer sortOrder;
		private final List<MenuNode> children = new ArrayList<>();
		
		private boolean active;        // 현재 페이지와 매칭되는 메뉴인지
		private boolean expanded;    // 자식 중에 active가 있어서 펼쳐져야 하는 상위메뉴인지
		
		private MenuNode(Long id, String alias, String code, String url,
						 Integer depth, Integer sortOrder) {
			this.id = id;
			this.alias = alias;
			this.code = code;
			this.url = url;
			this.depth = depth;
			this.sortOrder = sortOrder;
		}
		
		public static MenuNode from(Menu menu) {
			return new MenuNode(
					menu.getId(),
					menu.getMenuAlias(),
					menu.getMenuCode(),
					menu.getUrl(),
					menu.getDepth(),
					menu.getSortOrder()
			);
		}
	}
	
}
