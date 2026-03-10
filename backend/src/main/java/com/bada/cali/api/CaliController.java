package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.CaliOrderServiceImpl;
import com.bada.cali.service.ExcelServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController("ApiCaliController")
@RequestMapping("/api/caliOrder")
@Log4j2
@RequiredArgsConstructor
public class CaliController {

	private final CaliOrderServiceImpl caliOrderService;
	private final ExcelServiceImpl excelServiceImpl;

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
	 * 처리 흐름:
	 *   1. ExcelServiceImpl.buildOrderExcel() 호출
	 *      → 스토리지에서 양식 다운로드 → 데이터 삽입 → temp/UUID/order.xlsx 생성
	 *      → 내부에서 다운로드 로그(log 테이블) 저장
	 *   2. 생성된 파일을 InputStreamResource로 응답
	 *   3. finally에서 temp/UUID 디렉토리 정리 (파일 포함)
	 *
	 * @param id      교정접수 ID
	 * @param user    인증된 사용자 정보 (로그용)
	 * @param request HTTP 요청 (IP 추출용)
	 */
	@GetMapping(value = "/downloadOrderForm")
	public ResponseEntity<Resource> downloadOrderForm(
			@RequestParam Long id,
			@AuthenticationPrincipal CustomUserDetails user,
			HttpServletRequest request
	) {
		Path tempExcelPath = null;
		try {
			// 1. 엑셀 생성 + 로그 저장 (서비스 계층에서 일괄 처리)
			tempExcelPath = excelServiceImpl.buildOrderExcel(
					id, user.getName(), user.getId(), request.getRemoteAddr());

			long contentLength = Files.size(tempExcelPath);

			String fileName = "교정신청서.xlsx";
			String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

			// 2. 파일 스트림 응답 — try-with-resources 미사용(응답 전 스트림 닫힘 방지)
			//    temp 정리는 finally에서 처리
			Path finalPath = tempExcelPath;
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"" + encodedName + "\"")
					.contentLength(contentLength)
					.body(new InputStreamResource(Files.newInputStream(finalPath)));

		} catch (EntityNotFoundException e) {
			// 접수 데이터 없음 → GlobalApiExceptionHandler에서 404 처리
			throw e;
		} catch (Exception e) {
			log.error("[교정신청서] 다운로드 실패 - orderId={}", id, e);
			throw new RuntimeException("교정신청서 다운로드 중 오류가 발생했습니다.", e);
		} finally {
			// 3. 응답 여부와 무관하게 temp/UUID 디렉토리 정리
			if (tempExcelPath != null) {
				excelServiceImpl.cleanupTempDir(tempExcelPath.getParent());
			}
		}
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