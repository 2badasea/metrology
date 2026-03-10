package com.bada.cali.repository.projection;

/**
 * 접수자 select 구성용 프로젝션.
 * 사내 직원 목록에서 id와 이름만 내려준다.
 */
public interface MemberSelectRow {
    Long getId();
    String getName();
}
