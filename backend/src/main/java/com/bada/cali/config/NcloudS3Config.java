package com.bada.cali.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// AmazonS3 Bean 생성
// Naver Cloud Object Storage (S3 호환)용 AmazonS3 클라이언트 설정
@Configuration
@RequiredArgsConstructor
public class NcloudS3Config {
	
	private final NcpStorageProperties storageProps;
	
	@Bean
	public AmazonS3 ncloudS3Client() {
		
		AwsClientBuilder.EndpointConfiguration endpointConfig =
				new AwsClientBuilder.EndpointConfiguration(
						storageProps.getEndpoint(),
						storageProps.getRegionName()
				);
		
		BasicAWSCredentials credentials =
				new BasicAWSCredentials(
						storageProps.getAccessKey(),
						storageProps.getSecretKey()
				);
		
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setSignerOverride("S3SignerType");      // ★ 중요
		
		
		
		return AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(endpointConfig)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				// NCP Object Storage는 path-style을 권장하는 케이스가 많아서 켜두는 걸 추천
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfig)       // ★ 중요
				.build();
	}
	
}
