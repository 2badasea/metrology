package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.config.NcpStorageProperties;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.CaliOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController("ApiCaliController")
@RequestMapping("/api/caliOrder")
@Log4j2
@RequiredArgsConstructor
public class CaliController {

	private final CaliOrderServiceImpl caliOrderService;
	private final LogRepository logRepository;
	private final S3Client ncloudS3Client;
	private final NcpStorageProperties storageProps;
	
	// 교정접수 리스트 가져오기
	// NOTE 리턴타입 제네릭 제대로 명시할 것
	@GetMapping("/getOrderList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<CaliDTO.OrderRowData>>> getOrderList(
			@ModelAttribute CaliDTO.GetOrderListReq req
	) {
		// 리스트 데이터 및 페이지 정보를 담은 데이터 가져옴
		TuiGridDTO.ResData<CaliDTO.OrderRowData> resGridData = caliOrderService.getOrderList(req);
		// tui grid api 형식에 맞춘 형태로 리턴
		TuiGridDTO.Res<TuiGridDTO.ResData<CaliDTO.OrderRowData>> body = new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 교정접수 등록/수정
	@PostMapping(value = "/saveCaliOrder")
	public ResponseEntity<Map<String, String>> saveCaliOrder(
			@RequestBody CaliDTO.saveCaliOrder caliOrderData,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		log.info(caliOrderData);
		
		// 등록/수정된 id 반환하기
		Map<String, String> resMap = caliOrderService.saveCaliOrder(caliOrderData, user);
		
		return ResponseEntity.ok(resMap);
	}
	
	// 접수 데이터 가져오기
	@GetMapping(value = "/getCaliOrderInfo/{id}")
	public ResponseEntity<ResMessage<CaliDTO.saveCaliOrder>> getCaliOrderInfo(@PathVariable Long id) {
		
		CaliDTO.saveCaliOrder caliOrderData = caliOrderService.getCaliOrderInfo(id);
		int code = (caliOrderData == null) ? -1 : 1;
		ResMessage<CaliDTO.saveCaliOrder> resMessage = new ResMessage<>(code, null, caliOrderData);
		return ResponseEntity.ok(resMessage);
	}
	
	/**
	 * 교정신청서 엑셀 다운로드.
	 * Object Storage의 고정 경로({bucket}/{rootDir}/env/order.xlsx)에서 파일을 스트리밍으로 내려준다.
	 * 추후 접수 데이터 삽입 기능 구현 시 이 엔드포인트를 확장한다.
	 */
	@GetMapping(value = "/downloadOrderForm")
	public ResponseEntity<Resource> downloadOrderForm() {
		// rootDir(ex: dev/prod) + 고정 경로. 버킷명은 별도로 지정하므로 포함하지 않음
		final String objectKey = storageProps.getRootDir() + "/env/order.xlsx";
		final String fileName = "교정신청서.xlsx";

		GetObjectRequest getReq = GetObjectRequest.builder()
				.bucket(storageProps.getBucketName())
				.key(objectKey)
				.build();

		// 스트리밍 방식 — try-with-resources로 닫으면 응답 전에 스트림이 닫히므로 사용하지 않음
		ResponseInputStream<GetObjectResponse> s3is = ncloudS3Client.getObject(getReq);
		long contentLength = s3is.response().contentLength();

		String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
				.contentLength(contentLength)
				.body(new InputStreamResource(s3is));
	}

	// 세금계산서 발행여부 변경
	@PostMapping(value = "/updateIsTax")
	public ResponseEntity<ResMessage<?>> updateIsTax(
			@ModelAttribute CaliDTO.UpdateIsTaxReq req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		
		ResMessage<?> resMessage = caliOrderService.updateIsTax(req, user);
		return ResponseEntity.ok(resMessage);
	}
}
