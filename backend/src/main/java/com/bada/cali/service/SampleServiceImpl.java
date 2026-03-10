package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.FileInfoDTO;
import com.bada.cali.dto.SampleDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Sample;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.SampleRepository;
import com.bada.cali.repository.projection.SampleListRow;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class SampleServiceImpl {

	private final SampleRepository sampleRepository;
	private final FileInfoRepository fileInfoRepository;
	private final LogRepository logRepository;
	private final FileServiceImpl fileServiceImpl;

	// 샘플 목록 조회 (Toast UI Grid 서버 페이징)
	@Transactional(readOnly = true)
	public TuiGridDTO.Res<TuiGridDTO.ResData<SampleListRow>> getSampleList(SampleDTO.GetListReq req) {
		int pageIndex = req.getPage() - 1;
		int pageSize = req.getPerPage();
		Pageable pageable = PageRequest.of(pageIndex, pageSize);

		String searchType = req.getSearchType();
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		searchType = switch (searchType) {
			case "all", "middleCodeNum", "middleCodeName", "smallCodeNum", "smallCodeName", "name" -> searchType;
			default -> "all";
		};

		String keyword = req.getKeyword();
		if (keyword == null) keyword = "";

		Page<SampleListRow> page = sampleRepository.searchSamples(
				req.getMiddleItemCodeId(),
				req.getSmallItemCodeId(),
				searchType,
				keyword,
				pageable
		);

		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) page.getTotalElements())
				.build();

		TuiGridDTO.ResData<SampleListRow> resData = TuiGridDTO.ResData.<SampleListRow>builder()
				.contents(page.getContent())
				.pagination(pagination)
				.build();

		return new TuiGridDTO.Res<>(true, resData);
	}

	// 샘플 파일 목록 조회 (최신순)
	@Transactional(readOnly = true)
	public List<FileInfoDTO.FileListRes> getSampleFiles(Long sampleId) {
		sampleRepository.findById(sampleId)
				.filter(s -> s.getIsVisible() == YnType.y)
				.orElseThrow(() -> new EntityNotFoundException("샘플을 찾을 수 없습니다. id=" + sampleId));
		return fileInfoRepository.getFileInfosWithJoinOrderByDateDesc("sample", sampleId, YnType.y);
	}

	// 중복 체크 (중분류 + 소분류 + 기기명)
	@Transactional(readOnly = true)
	public SampleDTO.DuplicateCheckRes checkDuplicate(Long middleItemCodeId, Long smallItemCodeId, String name) {
		return sampleRepository
				.findByMiddleItemCodeIdAndSmallItemCodeIdAndNameAndIsVisible(
						middleItemCodeId, smallItemCodeId, name, YnType.y)
				.map(s -> new SampleDTO.DuplicateCheckRes(true, s.getId()))
				.orElse(new SampleDTO.DuplicateCheckRes(false, null));
	}

	// 샘플 등록
	@Transactional
	public Long createSample(SampleDTO.SampleSaveReq req, MultipartFile file, CustomUserDetails user) {
		LocalDateTime now = LocalDateTime.now();

		Sample sample = Sample.builder()
				.name(req.name())
				.middleItemCodeId(req.middleItemCodeId())
				.smallItemCodeId(req.smallItemCodeId())
				.isVisible(YnType.y)
				.createDatetime(now)
				.createMemberId(user.getId())
				.build();
		Sample saved = sampleRepository.save(sample);

		if (file != null && !file.isEmpty()) {
			String dir = "sample/" + saved.getId() + "/";
			fileServiceImpl.saveFiles("sample", saved.getId(), dir, List.of(file), user.getId());
		}

		logRepository.save(Log.builder()
				.logType("i")
				.refTable("sample")
				.refTableId(saved.getId())
				.logContent("고유번호 - [" + saved.getId() + "]")
				.workerName(user.getName())
				.createDatetime(now)
				.createMemberId(user.getId())
				.build());

		return saved.getId();
	}

	// 샘플 수정 (기기명·분류 수정 및 파일 추가)
	@Transactional
	public void updateSample(Long sampleId, SampleDTO.SampleSaveReq req, MultipartFile file, CustomUserDetails user) {
		Sample sample = sampleRepository.findById(sampleId)
				.filter(s -> s.getIsVisible() == YnType.y)
				.orElseThrow(() -> new EntityNotFoundException("샘플을 찾을 수 없습니다. id=" + sampleId));

		sample.setName(req.name());
		sample.setMiddleItemCodeId(req.middleItemCodeId());
		sample.setSmallItemCodeId(req.smallItemCodeId());
		sample.setUpdateMemberId(user.getId());

		if (file != null && !file.isEmpty()) {
			String dir = "sample/" + sampleId + "/";
			fileServiceImpl.saveFiles("sample", sampleId, dir, List.of(file), user.getId());
		}

		logRepository.save(Log.builder()
				.logType("u")
				.refTable("sample")
				.refTableId(sampleId)
				.logContent("고유번호 - [" + sampleId + "]")
				.workerName(user.getName())
				.createDatetime(LocalDateTime.now())
				.createMemberId(user.getId())
				.build());
	}

	// 샘플 일괄 소프트 삭제 (연관 파일 포함)
	@Transactional
	public void deleteSamples(SampleDTO.DeleteReq req, CustomUserDetails user) {
		List<Long> ids = req.ids();
		if (ids == null || ids.isEmpty()) return;

		List<Sample> targets = sampleRepository.findByIdInAndIsVisible(ids, YnType.y);
		if (targets.isEmpty()) return;

		List<Long> targetIds = targets.stream().map(Sample::getId).toList();
		LocalDateTime now = LocalDateTime.now();

		sampleRepository.softDeleteByIds(targetIds, YnType.n, now, user.getId());
		fileServiceImpl.softDeleteFilesByRefTableIds("sample", targetIds, user.getId());

		logRepository.save(Log.builder()
				.logType("d")
				.refTable("sample")
				.refTableId(targetIds.get(0))
				.logContent("고유번호 - " + targetIds)
				.workerName(user.getName())
				.createDatetime(now)
				.createMemberId(user.getId())
				.build());
	}

	// 파일 단건 소프트 삭제
	@Transactional
	public void deleteSampleFile(Long fileId, CustomUserDetails user) {
		fileInfoRepository.findById(fileId)
				.orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다. id=" + fileId));

		fileInfoRepository.deleteFile(fileId, YnType.n, LocalDateTime.now(), user.getId());

		logRepository.save(Log.builder()
				.logType("d")
				.refTable("file_info")
				.refTableId(fileId)
				.logContent("고유번호 - [" + fileId + "] (샘플 파일 삭제)")
				.workerName(user.getName())
				.createDatetime(LocalDateTime.now())
				.createMemberId(user.getId())
				.build());
	}
}
