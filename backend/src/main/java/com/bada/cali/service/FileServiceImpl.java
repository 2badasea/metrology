package com.bada.cali.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.bada.cali.common.YnType;
import com.bada.cali.config.NcpStorageProperties;
import com.bada.cali.dto.FileInfoDTO;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class FileServiceImpl {
	
	private final FileInfoRepository fileInfoRepository;
	private final AmazonS3 ncloudS3Client;
	private final NcpStorageProperties storageProps; // endpoint, bucketName 등
	private final NcpStorageProperties ncpStorageProperties;
	
	@Transactional
	public void saveFiles(String refTableName,
						  Long refTableId,
						  String dir,        // '폴더명/' or '폴더명/id/' 형태로 넘어올 수 있음
						  List<MultipartFile> files,
						  CustomUserDetails user
	) {
		
		LocalDateTime now = LocalDateTime.now();
		String bucket = storageProps.getBucketName();
		String rootDir = storageProps.getRootDir();
		
		List<String> uploadedKeys = new ArrayList<>();
		
		try {
			for (MultipartFile file : files) {
				// 비어 있는 항목은 skip
				if (file == null || file.isEmpty()) {
					continue;
				}
				
				String originName = file.getOriginalFilename();    // 풀네임
				if (originName == null) {
					originName = "";
				}
				// 일부 브라우저에서 경로까지 오는 경우 처리
				else {
					// 역슬래시 기반 경로로 한번 잘라보고, 슬래시 경로로 한번 더 잘라서 -> windows/unix 둘 다 커버
					originName = originName
							.substring(originName.lastIndexOf("\\") + 1)
							.substring(originName.lastIndexOf("/") + 1);
					// 아래처럼 표현도 가능
					// originName = originName.replace("\\", "/");
					// originName = originName.substring(originName.lastIndexOf("/") + 1);
				}
				
				String extension = getFileExtension(originName);        // 파일 확장자 구하기
				String name = getFileNameWithoutExtension(originName);    // 파일명 얻기
				
				FileInfo fileInfo = FileInfo.builder()
						.refTableId(refTableId)
						.refTableName(refTableName)
						.originName(originName)
						.name(name)
						.extension(extension)
						.fileSize(file.getSize())
						.contentType(file.getContentType())
						.dir(dir)
						.isVisible(YnType.y)
						.createDatetime(now)
						.createMemberId(user.getId())
						.build();
				// 파일 저장
				
				FileInfo savedFile = fileInfoRepository.save(fileInfo);
				// 오브젝트 키 만들기 ('dir' + 'file_info.id' + '.확장자')
				String objectKey = rootDir + "/";
				if (dir.endsWith("/")) {
					objectKey += dir + savedFile.getId();
				} else {
					objectKey += dir + "/" + savedFile.getId();
				}
				
				if (extension != null && !extension.isEmpty()) {
					objectKey = objectKey + "." + extension;
				}
				// 스토리지 업로드
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(file.getSize());
				if (file.getContentType() != null) {
					metadata.setContentType(file.getContentType());
				}
				
				
				try (InputStream is = file.getInputStream()) {
					
					try {
						ncloudS3Client.putObject(bucket, objectKey, is, metadata);
					} catch (com.amazonaws.services.s3.model.AmazonS3Exception e) {
						log.error("S3 API 에러: status={}, code={}, message={}",
								e.getStatusCode(), e.getErrorCode(), e.getMessage(), e);
						throw e;
					} catch (com.amazonaws.SdkClientException e) {
						log.error("S3 클라이언트 에러: {}", e.getMessage(), e);
						throw e;
					}
				}
				uploadedKeys.add(objectKey);
				
			}    // End for
			
		} catch (Exception e) {
			// 필요 시 보상 트랜잭션: 이미 업로드된 파일 삭제
			for (String key : uploadedKeys) {
				try {
					ncloudS3Client.deleteObject(bucket, key);
				} catch (Exception ex) {
					log.error("스토리지 롤백 실패 - key: {}", key, ex);
				}
			}
			throw new RuntimeException("파일 업로드 중 오류 발생", e);
		}
	}
	
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadFile(Long fileId) {
		
		// DB에서 file조회
		FileInfo fileInfo = fileInfoRepository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("파일 정보를 찾을 수 없습니다. id= " + fileId));
		
		String bucket = storageProps.getBucketName();
		String rootDir = storageProps.getRootDir();
		String dir = fileInfo.getDir();
		String extension = fileInfo.getExtension();
		
		// 업로드 시 사용했던 규칙 그대로 objectKey 생성
		String objectKey = rootDir + "/";
		if (dir.endsWith("/")) {
			objectKey += dir + fileInfo.getId();
		} else {
			objectKey += dir + "/" + fileInfo.getId();
		}
		if (extension != null && !extension.isEmpty()) {
			objectKey = objectKey + "." + extension;
		}
		
		try {
			// NCP Object Storage에서 파일 가져오기 (스트리밍 방식)
			S3Object s3Object = ncloudS3Client.getObject(bucket, objectKey);
			S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
			Long contentLength = s3Object.getObjectMetadata().getContentLength();
			
			Resource resource = new InputStreamResource(s3ObjectInputStream);
			
			// 헤더용 파일명, 컨텐츠 타입 세팅
			String originName = fileInfo.getOriginName();
			if (originName == null || originName.isBlank()) {
				originName = "file";
			}
			String encodedName = URLEncoder.encode(originName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");    // 공백깨짐 방지
			
			String contentType = fileInfo.getContentType();
			MediaType mediaType;
			try {
				if (contentType != null && !contentType.isBlank()) {
					mediaType = MediaType.parseMediaType(contentType);
				} else {
					mediaType = MediaType.APPLICATION_OCTET_STREAM;
				}
			} catch (InvalidMediaTypeException e) {
				mediaType = MediaType.APPLICATION_OCTET_STREAM;
			}
			
			// 다운로드 응답 생성
			return ResponseEntity.ok().contentType(mediaType).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"").contentLength(contentLength).body(resource);
			
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == 404) {
				throw new IllegalArgumentException("스토리지에서 파일을 찾을 수 없습니다. key=" + objectKey, e);
			}
			throw new IllegalStateException("스토리지 파일 다운로드 중 오류 발생", e);
		}
	}
	
	// 파일 삭제(스토리지에선 그대로)
	@Transactional
	public int deleteFile(Long fileId, CustomUserDetails user) {
		return fileInfoRepository.deleteFile(fileId, YnType.n, LocalDateTime.now(), user.getId());
	}
	
	
	// "파일명.확장자" 에서 확장자를 추출 (없으면 빈 문자열)
	public String getFileExtension(String originName) {
		if (originName == null || originName.isEmpty()) {
			return "";
		}
		int lastDot = originName.lastIndexOf('.');
		if (lastDot == -1 || lastDot == originName.length() - 1) {
			return "";
		}
		return originName.substring(lastDot + 1);  // pdf, xlsx, png ...
	}
	
	// "파일명.확장자" 에서 확장자를 제외한 이름만 추출
	public String getFileNameWithoutExtension(String originName) {
		if (originName == null || originName.isEmpty()) {
			return "";
		}
		int lastDot = originName.lastIndexOf('.');
		if (lastDot == -1) {
			return originName;
		}
		return originName.substring(0, lastDot);
	}
	
	// join을 고려하지 않은 단순 file_info 정보만 반환
	public List<FileInfo> getFileInfos(String refTableName, Long refTableId) {
		return fileInfoRepository.findByRefTableNameAndRefTableIdAndIsVisible(refTableName, refTableId, YnType.y);
	}
	
	public List<FileInfoDTO.FileListRes> getFileInfosWithJoin(String refTableName, Long refTableId) {
		return fileInfoRepository.getFileInfosWithJoin(refTableName, refTableId, YnType.y);
	}
	
	
}
