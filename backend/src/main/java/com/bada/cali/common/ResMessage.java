package com.bada.cali.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResMessage<T> {
	private int code = 0;
	private String msg = "";
	private T data;				// 리스트나 객체 데이터 담을 것
	
}
