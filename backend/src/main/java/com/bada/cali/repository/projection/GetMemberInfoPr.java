package com.bada.cali.repository.projection;

import com.bada.cali.common.enums.YnType;

import java.time.LocalDate;

public interface GetMemberInfoPr {
	String getLoginId();
	
	String getName();
	
	String getNameEng();
	
	String getHp();
	
	String getCompanyNo();
	
	LocalDate getBirth();
	
	String getAddr1();
	
	String getAddr2();
	
	String getEmail();
	
	String getTel();
	
	Long getDepartmentId();
	
	Long getLevelId();
	
	YnType getIsActive();
	
	LocalDate getJoinDate();
	
	LocalDate getLeaveDate();
	
	Integer getWorkType();
	
	String getRemark();
	
	Long getImgFileId();
	
	String getExtension();
	
	String getDir();
	
}
