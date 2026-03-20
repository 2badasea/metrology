$(function () {
	console.log('++ cali/reportWrite.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	// 모달 컨텍스트에서 gModal은 .modal-body에 param을 저장하므로 .modal-body를 우선 선택
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	// =====================================================================
	// loadGridData: API 호출 후 그리드 데이터 갱신
	// 페이지네이션 없이 전체 목록을 받아 resetData()로 세팅
	// =====================================================================
	$modal.loadGridData = async function () {
		const smallItemCodeId = $modal.param?.smallItemCodeId ?? 0;
		const searchType      = $('form.searchForm .searchType', $modal).val() ?? 'all';
		const keyword         = $('form.searchForm #keyword', $modal).val() ?? '';

		try {
			const res = await gAjax(
				'/api/sample/reportWriteList',
				{ smallItemCodeId, searchType, keyword },
				{ type: 'GET' }
			);
			if (res?.code > 0) {
				$modal.grid.resetData(res.data ?? []);
			} else {
				gToast('목록을 불러오지 못했습니다.', 'warning');
			}
		} catch (xhr) {
			customAjaxHandler(xhr);
		}
	};

	// =====================================================================
	// init_modal: 모달 파라미터 수신 후 그리드 초기 로딩
	// param: { smallItemCodeId, smallCodeNum }
	// =====================================================================
	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		// 그리드 레이아웃 갱신 후 초기 데이터 로딩
		if (typeof $modal.grid == 'object') {
			$modal.grid.refreshLayout();
		}
		await $modal.loadGridData();
	};

	// =====================================================================
	// 그리드 정의 (페이지네이션 없음 — 스크롤 방식)
	// bodyHeight 고정 + rowHeight auto → 데이터 초과 시 scrollY 활성화
	// =====================================================================
	$modal.grid = gGrid('.reportWriteGrid', {
		pageOptions: false, // 페이지네이션 비활성화 (스크롤 방식)
		columns: [
			{
				header: '기기명',
				name: 'itemName',
				align: 'center',
				whiteSpace: 'pre-line',
				className: 'cursor_pointer',
			},
			{
				header: '파일명',
				name: 'fileName',
				align: 'center',
				whiteSpace: 'pre-line',
				className: 'cursor_pointer',
			},
			{
				header: '등록자',
				name: 'createMemberName',
				width: 90,
				align: 'center',
				className: 'cursor_pointer',
			},
			{
				header: '등록일시',
				name: 'createDatetime',
				width: 150,
				align: 'center',
				className: 'cursor_pointer',
				formatter: function (data) {
					// "2026-03-20T14:30:00" → "2026-03-20 14:30"
					if (!data.value) return '';
					return String(data.value).replace('T', ' ').slice(0, 16);
				},
			},
		],
		minBodyHeight: 500,
		bodyHeight: 500,   // 약 10~12행 수준, 초과 시 scrollY 활성화
		rowHeight: 'auto',
	});

	// =====================================================================
	// 이벤트 바인딩
	// =====================================================================
	$modal
		// 검색 폼 submit → 데이터 재조회
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.loadGridData();
		});

	// =====================================================================
	// 페이지 마운트 처리 (common.js 규약)
	// =====================================================================
	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		// 모달 컨텍스트: gModal이 .modal-body.data('param')에 저장한 param을 읽어 init_modal 호출
		// modal_ready 이벤트 대신 reportModify.js 패턴과 동일하게 setTimeout으로 처리
		setTimeout(() => {
			const p = $modal.data('param') || {};
			$modal.init_modal(p);
			if (typeof $modal.grid == 'object') {
				$modal.grid.refreshLayout();
			}
		}, 200);
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});