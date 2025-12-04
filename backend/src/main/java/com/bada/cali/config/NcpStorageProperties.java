package com.bada.cali.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.properties / application-*.properties 의
 * ncp.storage.* 값을 바인딩하는 설정 클래스
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ncp.storage")
public class NcpStorageProperties {
	
	// ex) https://kr.object.ncloudstorage.com
	private String endpoint;
	
	// ex) kr-standard
	private String regionName;
	
	// NCP Object Storage 버킷명
	// dev, prod
	private String bucketName;
	
	// 접속용 Access Key
	private String accessKey;
	
	// 접속용 Secret Key
	private String secretKey;
	
	// (옵션) 루트 디렉토리 prefix
	// ex) dev, prod 등. 필요 없으면 안 써도 됨.
	private String rootDir;
}
