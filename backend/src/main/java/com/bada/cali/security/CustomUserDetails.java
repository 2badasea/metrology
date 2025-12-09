package com.bada.cali.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

// UserDetails를 커스텀(principal)
// CustomUserDetailService class에서 loadUserByUsername에서 리턴하는 객체의 형태를 커스텀하기 위함
@Getter
public class CustomUserDetails implements UserDetails {
	private final Long id;
	private final String username;
	private final String password;
	private final String name;
	private final Collection<? extends GrantedAuthority> authorities;
	private final Set<Long> readableMenuIds;        // 사용자별 접근이 가능한 메뉴id
	
	public CustomUserDetails(Long id, String username, String password, String name, Collection<? extends GrantedAuthority> authorities, Set<Long> readableMenuIds) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.name = name;
		this.authorities = authorities;
		this.readableMenuIds = readableMenuIds;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public String getPassword() {
		return password;
	}
	
	@Override
	public String getUsername() {
		return username;
	}
	// 아래는 굳이 오버라이드 하지 않아도 됨. 지우거나 return 문에 UserDetails.super.xxx()로 리턴문 변경해야 함
//	@Override public boolean isAccountNonExpired() { return true; }
//	@Override public boolean isAccountNonLocked() { return true; }
//	@Override public boolean isCredentialsNonExpired() { return true; }
//	@Override public boolean isEnabled() { return true; }
	
	
}
