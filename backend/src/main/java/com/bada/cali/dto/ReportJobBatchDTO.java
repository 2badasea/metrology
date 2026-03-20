package com.bada.cali.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 성적서 작업 배치(report_job_batch) 관련 DTO
 */
public class ReportJobBatchDTO {

    /**
     * 성적서작성 배치 생성 요청 DTO (WRITE 타입 전용)
     *
     * 프론트에서 reportWrite 모달 내 샘플 행 클릭 후
     * 확인 시 이 DTO를 POST /api/report/jobs/batches 로 전송한다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "성적서작성 배치 생성 요청")
    public static class CreateWriteBatchReq {

        /**
         * 성적서작성 대상 성적서 id 목록 (1건 이상)
         * workApproval 페이지에서 체크박스로 선택한 행들의 id,
         * 또는 reportModify 모달에서 단건으로 호출 시 1개 원소 리스트
         */
        @NotEmpty(message = "대상 성적서를 1건 이상 선택해야 합니다.")
        @Schema(description = "대상 성적서 id 목록", example = "[1, 2, 3]")
        private List<Long> reportIds;

        /**
         * 선택한 샘플 id (sample.id)
         * 작업서버는 이 샘플 파일을 스토리지에서 다운로드하여 성적서를 생성한다.
         */
        @NotNull(message = "샘플을 선택해야 합니다.")
        @Schema(description = "선택한 샘플 id (sample.id)", example = "10")
        private Long sampleId;
    }

    /**
     * 성적서작성 배치 생성 응답 DTO
     * 생성된 배치 id와 대상 건수를 반환한다.
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "성적서작성 배치 생성 응답")
    public static class CreateBatchRes {

        @Schema(description = "생성된 배치 id", example = "123")
        private Long batchId;

        @Schema(description = "대상 성적서 건수", example = "3")
        private Integer totalCount;
    }
}