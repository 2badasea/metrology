$(function () {
	console.log('++ basic/dataSheetSetting.js');

	// ── $modal 셋업 (기존 스크립트 공통 패턴) ──
	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	let $modal;
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		// 페이지로 직접 열린 경우 첫 번째 modal-view 사용
		$modal = $notModalViewAppliedEle.first();
	}
	let $modal_root = $modal.closest('.modal');

	// ──────────────────────────────── 상수 ────────────────────────────────

	// 형식(format) 옵션이 있는 항목코드 목록 (radio name: {fieldCode}_format)
	const FORMAT_FIELDS = ['caliDate', 'approvalDate', 'worker', 'workerEn', 'approval', 'approvalEn'];

	// 엑셀 셀 주소 유효성 검사 정규식 (예: A1, B12, AA3 / 열: 1~3자리 알파벳, 행: 1~7자리 숫자)
	const CELL_PATTERN = /^[A-Za-z]{1,3}[1-9][0-9]{0,6}$/;

	// ──────────────────────────────── 초기화 ────────────────────────────────

	$modal.init_modal = async (param) => {
		$modal.param = param;

		// 저장된 설정값 조회 후 폼에 반영
		await $modal.loadSheetSetting();
	};

	// ──────────────────────────────── 설정 조회 ────────────────────────────────

	/**
	 * API에서 성적서시트 설정을 조회하여 폼에 반영한다.
	 * - cell 값이 있으면 해당 input에 대문자로 설정
	 * - format 값이 있으면 해당 radio 선택
	 * @returns {Promise}
	 */
	$modal.loadSheetSetting = () => {
		return new Promise((resolve) => {
			gAjax(
				'/api/admin/env/sheetSetting',
				{},
				{
					type: 'GET',
					success: function (res) {
						if (res?.code > 0 && res.data?.settings) {
							$modal.applySettings(res.data.settings);
						}
						resolve();
					},
					error: function (err) {
						gApiErrorHandler(err);
						resolve();
					},
				}
			);
		});
	};

	/**
	 * API 응답 settings 맵을 폼 필드에 반영한다.
	 * @param {Object} settings - { 항목코드: { cell, format }, ... }
	 */
	$modal.applySettings = (settings) => {
		Object.keys(settings).forEach(function (fieldCode) {
			const item = settings[fieldCode];

			// 셀위치 input 채우기 (항상 대문자)
			if (item.cell) {
				$('input.cell-input[data-field="' + fieldCode + '"]', $modal).val(item.cell.toUpperCase());
			}

			// 형식 radio 선택 복원 (해당 항목코드에 format 옵션이 있을 때만)
			if (item.format && FORMAT_FIELDS.includes(fieldCode)) {
				$('input[name="' + fieldCode + '_format"][value="' + item.format + '"]', $modal).prop('checked', true);
			}
		});
	};

	// ──────────────────────────────── 설정 수집 ────────────────────────────────

	/**
	 * 현재 폼 상태에서 settings 맵을 수집한다.
	 * - 셀위치가 빈 값이면 cell: null
	 * - format 항목은 선택된 radio 값 수집
	 * @returns {Object} - { 항목코드: { cell, format }, ... }
	 */
	$modal.collectSettings = () => {
		const settings = {};

		$('input.cell-input', $modal).each(function () {
			const $input = $(this);
			const fieldCode = $input.data('field');
			const cellVal = $input.val().trim().toUpperCase();

			const item = {
				cell: cellVal || null,
				format: null,
			};

			// 형식 옵션이 있는 항목만 radio 값 수집
			if (FORMAT_FIELDS.includes(fieldCode)) {
				const selectedFormat = $('input[name="' + fieldCode + '_format"]:checked', $modal).val();
				item.format = selectedFormat || null;
			}

			settings[fieldCode] = item;
		});

		return settings;
	};

	// ──────────────────────────────── 이벤트: 저장 버튼 ────────────────────────────────

	$('.saveSheetSetting', $modal).on('click', async function () {

		// 1) 셀위치 입력값 검증 — 빈 값 허용, 입력 시 엑셀 주소 형식 필수
		let isValid = true;
		let $firstInvalid = null;

		$('input.cell-input', $modal).each(function () {
			const $input = $(this);
			const val = $input.val().trim();

			if (val === '') {
				$input.removeClass('is-invalid');
				return true; // 빈 값은 허용 — $.each continue
			}

			if (!CELL_PATTERN.test(val)) {
				isValid = false;
				$input.addClass('is-invalid');
				if (!$firstInvalid) {
					$firstInvalid = $input;
				}
			} else {
				$input.removeClass('is-invalid');
			}
		});

		if (!isValid) {
			gErrorHandler('셀 주소 형식이 올바르지 않습니다. (예: A1, B12, AA3)');
			$firstInvalid.focus();
			return;
		}

		// 2) 저장 최종 확인
		const confirmResult = await gMessage('성적서시트 설정', '저장하시겠습니까?', 'question', 'confirm');
		if (!confirmResult.isConfirmed) return;

		// 3) API 저장 요청
		gAjax(
			'/api/admin/env/sheetSetting',
			JSON.stringify({ settings: $modal.collectSettings() }),
			{
				type: 'PATCH',
				contentType: 'application/json',
				success: async function (res) {
					if (res?.code > 0) {
						await gMessage('성적서시트 설정', res.message || '저장되었습니다.', 'success', 'alert');
						location.reload();
					} else {
						gApiErrorHandler(res?.message || '저장에 실패했습니다.');
					}
				},
				error: function (err) {
					gApiErrorHandler(err);
				},
			}
		);
	});

	// ──────────────────────────────── 셀위치 실시간 대문자 변환 ────────────────────────────────

	// 입력 중 알파벳 소문자 → 대문자 자동 변환 및 invalid 표시 해제
	$(document).on('input', 'input.cell-input', function () {
		const pos = this.selectionStart;
		$(this).val($(this).val().toUpperCase()).removeClass('is-invalid');
		this.setSelectionRange(pos, pos);
	});

	// ──────────────────────────────── 공통 패턴 마무리 ────────────────────────────────

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		// 모달 팝업창으로 열린 경우
		$modal_root.on('modal_ready', function (e, p) {
			$modal.init_modal(p);
			if (typeof $modal.grid == 'object') {
				$modal.grid.refreshLayout();
			}
		});
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});
