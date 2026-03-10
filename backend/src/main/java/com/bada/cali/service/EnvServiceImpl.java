package com.bada.cali.service;

import com.bada.cali.config.NcpStorageProperties;
import com.bada.cali.dto.EnvDTO;
import com.bada.cali.entity.Env;
import com.bada.cali.entity.Log;
import com.bada.cali.repository.EnvRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class EnvServiceImpl {

	private static final Set<String> ALLOWED_IMAGE_FIELDS = Set.of("kolas", "ilac", "company");
	private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10MB

	private final EnvRepository    envRepository;
	private final LogRepository    logRepository;
	private final NcpStorageProperties storageProps;
	private final S3Client         ncloudS3Client;

	// ──────────────────────────────── 조회 ────────────────────────────────

	@Transactional(readOnly = true)
	public EnvDTO.EnvRes getEnv() {
		Env env = envRepository.findById((byte) 1).orElse(null);
		if (env == null) {
			return null;
		}
		return toRes(env);
	}

	// ──────────────────────────────── 입력폼 저장 ────────────────────────────────

	@Transactional
	public void updateEnv(EnvDTO.EnvUpdateReq req, CustomUserDetails user, HttpServletRequest httpReq) {
		Env env = envRepository.findById((byte) 1)
				.orElseGet(() -> Env.builder().id((byte) 1).build());

		env.setName(trimOrNull(req.name()));
		env.setNameEn(trimOrNull(req.nameEn()));
		env.setCeo(trimOrNull(req.ceo()));
		env.setTel(trimOrNull(req.tel()));
		env.setFax(trimOrNull(req.fax()));
		env.setHp(trimOrNull(req.hp()));
		env.setAddr(trimOrNull(req.addr()));
		env.setAddrEn(trimOrNull(req.addrEn()));
		env.setEmail(trimOrNull(req.email()));
		env.setReportIssueAddr(trimOrNull(req.reportIssueAddr()));
		env.setReportIssueAddrEn(trimOrNull(req.reportIssueAddrEn()));
		env.setSiteAddr(trimOrNull(req.siteAddr()));
		env.setSiteAddrEn(trimOrNull(req.siteAddrEn()));
		env.setAgentNum(trimOrNull(req.agentNum()));
		env.setBackAccount(trimOrNull(req.backAccount()));
		env.setUpdateMemberId(user.getId());
		env.setUpdateDatetime(LocalDateTime.now());

		envRepository.save(env);

		saveLog("u", "회사정보 수정", user, httpReq);
	}

	// ──────────────────────────────── 이미지 저장 ────────────────────────────────

	@Transactional
	public void saveEnvImage(String field, MultipartFile file, CustomUserDetails user, HttpServletRequest httpReq) {
		validateField(field);
		validateImageFile(file);

		Env env = envRepository.findById((byte) 1)
				.orElseGet(() -> Env.builder().id((byte) 1).build());

		String extension  = getExtension(file.getOriginalFilename());
		String objectPath = "env/" + field + (StringUtils.hasText(extension) ? "." + extension : "");
		String objectKey  = storageProps.getRootDir() + "/" + objectPath;

		// 기존 이미지가 있고 경로가 다르면(확장자 변경) 기존 파일 삭제
		String existingPath = getFieldValue(env, field);
		if (StringUtils.hasText(existingPath) && !existingPath.equals(objectPath)) {
			deleteFromStorage(storageProps.getRootDir() + "/" + existingPath);
		}

		// 스토리지 업로드
		uploadToStorage(objectKey, file);

		// DB 경로 갱신
		setFieldValue(env, objectPath, field);
		env.setUpdateMemberId(user.getId());
		env.setUpdateDatetime(LocalDateTime.now());
		envRepository.save(env);

		saveLog("u", "회사정보 이미지 저장 - " + field, user, httpReq);
	}

	// ──────────────────────────────── 이미지 삭제 ────────────────────────────────

	@Transactional
	public void deleteEnvImage(String field, CustomUserDetails user, HttpServletRequest httpReq) {
		validateField(field);

		Env env = envRepository.findById((byte) 1)
				.orElseGet(() -> Env.builder().id((byte) 1).build());

		String existingPath = getFieldValue(env, field);
		if (StringUtils.hasText(existingPath)) {
			deleteFromStorage(storageProps.getRootDir() + "/" + existingPath);
		}

		setFieldValue(env, null, field);
		env.setUpdateMemberId(user.getId());
		env.setUpdateDatetime(LocalDateTime.now());
		envRepository.save(env);

		saveLog("d", "회사정보 이미지 삭제 - " + field, user, httpReq);
	}

	// ──────────────────────────────── private 유틸 ────────────────────────────────

	private EnvDTO.EnvRes toRes(Env env) {
		return new EnvDTO.EnvRes(
				env.getName(), env.getNameEn(), env.getCeo(),
				env.getTel(), env.getFax(), env.getHp(),
				env.getAddr(), env.getAddrEn(), env.getEmail(),
				env.getReportIssueAddr(), env.getReportIssueAddrEn(),
				env.getSiteAddr(), env.getSiteAddrEn(),
				env.getAgentNum(), env.getBackAccount(),
				buildImageUrl(env.getKolas()),
				buildImageUrl(env.getIlac()),
				buildImageUrl(env.getCompany())
		);
	}

	private String buildImageUrl(String path) {
		if (!StringUtils.hasText(path)) return null;
		return String.format("%s/%s/%s/%s",
				storageProps.getEndpoint(), storageProps.getBucketName(),
				storageProps.getRootDir(), path);
	}

	private String trimOrNull(String v) {
		if (v == null) return null;
		String trimmed = v.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void validateField(String field) {
		if (!ALLOWED_IMAGE_FIELDS.contains(field)) {
			throw new IllegalArgumentException("유효하지 않은 이미지 필드입니다: " + field);
		}
	}

	private void validateImageFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("파일이 비어 있습니다.");
		}
		if (file.getSize() > MAX_IMAGE_SIZE) {
			throw new IllegalArgumentException("이미지 크기는 10MB를 초과할 수 없습니다.");
		}
		String ct = file.getContentType();
		if (ct == null || !ct.startsWith("image/")) {
			throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
		}
	}

	private String getExtension(String filename) {
		if (!StringUtils.hasText(filename)) return "";
		int dot = filename.lastIndexOf('.');
		return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1).toLowerCase() : "";
	}

	private String getFieldValue(Env env, String field) {
		return switch (field) {
			case "kolas"   -> env.getKolas();
			case "ilac"    -> env.getIlac();
			case "company" -> env.getCompany();
			default        -> null;
		};
	}

	private void setFieldValue(Env env, String value, String field) {
		switch (field) {
			case "kolas"   -> env.setKolas(value);
			case "ilac"    -> env.setIlac(value);
			case "company" -> env.setCompany(value);
		}
	}

	private void uploadToStorage(String objectKey, MultipartFile file) {
		PutObjectRequest putReq = PutObjectRequest.builder()
				.bucket(storageProps.getBucketName())
				.key(objectKey)
				.acl(ObjectCannedACL.PUBLIC_READ)
				.contentType(file.getContentType())
				.build();
		try (InputStream is = file.getInputStream()) {
			ncloudS3Client.putObject(putReq, RequestBody.fromInputStream(is, file.getSize()));
		} catch (Exception e) {
			log.error("이미지 업로드 실패 - key: {}", objectKey, e);
			throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
		}
	}

	private void deleteFromStorage(String objectKey) {
		try {
			ncloudS3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(storageProps.getBucketName())
					.key(objectKey)
					.build());
		} catch (Exception e) {
			log.warn("스토리지 이미지 삭제 실패 (계속 진행) - key: {}", objectKey, e);
		}
	}

	private void saveLog(String type, String content, CustomUserDetails user, HttpServletRequest req) {
		logRepository.save(Log.builder()
				.logType(type)
				.refTable("env")
				.refTableId(1L)
				.logContent(content)
				.workerName(user.getName())
				.createMemberId(user.getId())
				.logIp(req.getRemoteAddr())
				.createDatetime(LocalDateTime.now())
				.build());
	}
}
