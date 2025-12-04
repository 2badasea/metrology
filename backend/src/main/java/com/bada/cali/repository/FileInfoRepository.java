package com.bada.cali.repository;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.FileInfoDTO;
import com.bada.cali.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {
	
	// 특정 조건에 맞는 파일 개수 리턴
	List<FileInfo> findByRefTableNameAndRefTableIdAndIsVisible(
			String refTableName,
			Integer refTableId,
			YnType isVisible
	);
	
	// FileInfoDTO 클래스 내 inner class FileListRes에서 순서대로 매핑이 됨
	@Query("select new com.bada.cali.dto.FileInfoDTO$FileListRes(" +
			"  f.id, " +
			"  f.originName, " +
			"  m.name, " +          // Member 엔티티의 필드명에 맞게 수정 (예: m.memberName)
			"  f.createDatetime" +
			") " +
			"from FileInfo f " +
			"join f.createMember m " +
			"where f.refTableName = :refTableName " +
			"and f.refTableId = :refTableId " +
			"and f.isVisible = :isVisible")
	List<FileInfoDTO.FileListRes> getFileInfosWithJoin(
			@Param("refTableName") String refTableName,
			@Param("refTableId") Integer refTableId,
			@Param("isVisible") YnType isVisible
	);
	
	
}
