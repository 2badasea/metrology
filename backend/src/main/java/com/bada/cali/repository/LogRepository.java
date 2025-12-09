package com.bada.cali.repository;

import com.bada.cali.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
	
	// 아래처럼 선언하지 않아도, save() 자체가 insert/update를 처리하는 표준 메서드로 JPARepository가 제공
	//	Integer saveLog(Log log);
}
