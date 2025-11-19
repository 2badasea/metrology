package com.bada.cali.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResDuplicateLoginIdDTO {
	private boolean isDuplicate;
	private String agentName;
	private String agentAddr;
}
