package com.bada.cali.repository.projection;

import java.time.LocalDate;

public interface ItemFeeHistory {
	Long getId();
	Long getItemId();
	LocalDate getBaseDate();
	Long getBaseFee();
	String getRemark();
}
