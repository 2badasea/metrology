package com.bada.cali.config;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class NcloudS3Config {
	
	private final NcpStorageProperties storageProps;
	
	@Bean(destroyMethod = "close")
	public S3Client ncloudS3Client() {
		
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
				storageProps.getAccessKey(),
				storageProps.getSecretKey()
		);
		
		return S3Client.builder()
				.endpointOverride(URI.create(storageProps.getEndpoint()))
				.region(Region.of(storageProps.getRegionName()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				// S3 호환 스토리지(NCP 포함)에서 path-style이 필요한 케이스가 많음
				// (SDK 2.18+에서 endpointOverride 사용 시 기본이 virtual-hosted로 동작할 수 있어 강제 권장) :contentReference[oaicite:1]{index=1}
				.forcePathStyle(true)
				.build();
	}
}
