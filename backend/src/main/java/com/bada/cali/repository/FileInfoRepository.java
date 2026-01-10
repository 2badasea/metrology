package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.FileInfoDTO;
import com.bada.cali.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {
	
	// 특정 조건에 맞는 파일 개수 리턴
	List<FileInfo> findByRefTableNameAndRefTableIdAndIsVisible(
			String refTableName,
			Long refTableId,
			YnType isVisible
	);
	
	// 특정 조건에 맞는 파일 개수 리턴
	@Query("""
			select f
			from FileInfo as f
			where f.isVisible = :isVisible
			and f.refTableName = :refTableName
			and f.refTableId = :refTableId
			and (:dir IS NOT NULL AND f.dir = :dir)
	""")
	List<FileInfo> getFileInfoList(
			@Param("refTableName") String refTableName,
			@Param("refTableId") Long refTableId,
			@Param("dir") String dir,
			@Param("isVisible") YnType isVisible
	);
	
	// 단일 파일 정보를 가져오는 함수
	@Query("""
					select f
					from FileInfo as f
					where 1 = 1
					AND (:isVisible IS NULL OR f.isVisible = :isVisible)
					AND (:refTableName IS NULL OR f.refTableName = :refTableName)
					AND (:isVisible IS NULL OR f.refTableId = :refTableId)
					AND (:dir IS NULL OR f.dir = :dir)
			""")
	FileInfo getFileInfo(
			@Param("refTableName") String refTableName,
			@Param("refTableId") Long refTableId,
			@Param("dir") String dir,
			@Param("isVisible") YnType isVisible
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
			@Param("refTableId") Long refTableId,
			@Param("isVisible") YnType isVisible
	);
	
	// 파일 삭제 처리
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			       update FileInfo f
			          set f.isVisible = :isVisible,
			              f.deleteDatetime = :deleteDatetime,
			              f.deleteMemberId = :deleteMemberId
			        where f.id = :fileId
			""")
	int deleteFile(@Param("fileId") Long fileId,
					  @Param("isVisible") YnType isVisible,
					  @Param("deleteDatetime") LocalDateTime deleteDatetime,
					  @Param("deleteMemberId") Long deleteMemberId
	);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        update FileInfo f
           set f.isVisible = :isVisible,
               f.deleteDatetime = :now,
               f.deleteMemberId = :userId
         where f.refTableName = :refTableName
           and f.refTableId = :refTableId
           and f.dir = :dir
    """)
	int softDeleteVisibleByRefAndDir(
			@Param("refTableName") String refTableName,
			@Param("refTableId") Long refTableId,
			@Param("dir") String dir,
			@Param("isVisible") YnType isVisible,
			@Param("now") LocalDateTime now,
			@Param("userId") Long userId
	);
	
	
}
