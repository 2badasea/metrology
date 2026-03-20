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

	// 그리드 행 클릭 → 성적서작성 배치 생성
	// SampleReportWriteRow: getId() = file_info.id, getSampleId() = sample.id
	// 배치 생성 API에는 sample.id(sampleId)를 전달해야 하므로 row.sampleId 사용
	$modal.grid.on('click', async function (ev) {
		const { rowKey } = ev;
		const row = $modal.grid.getRow(rowKey);
		if (!row) return;

		const reportIds  = $modal.param?.reportIds ?? [];
		const sampleId   = row.sampleId;   // sample.id (SampleReportWriteRow.getSampleId())
		const sampleName = row.itemName ?? '';
		const fileName   = row.fileName ?? '';

		// 대상 성적서 목록 미전달 시 경고 (reportWrite 단독 접근 등)
		if (!reportIds || reportIds.length === 0) {
			gToast('대상 성적서 정보가 없습니다.', 'warning');
			return;
		}

		// 샘플 선택 확인
		const result = await gMessage(
			'성적서 작성',
			`선택한 샘플로 성적서를 작성하시겠습니까?<br><b>${sampleName}</b> (${fileName})<br>대상 성적서: ${reportIds.length}건`,
			'question',
			'confirm',
			{ confirmButtonText: '작성 시작' },
		);
		if (!result.isConfirmed) return;

		// 배치 생성 API 호출
		try {
			gLoadingMessage('성적서작성 작업을 준비 중입니다...');
			const res = await fetch('/api/report/jobs/batches', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json; charset=utf-8' },
				body: JSON.stringify({ reportIds, sampleId }),
			});
			if (!res.ok) throw res;
			const resData = await res.json();
			if (resData?.code > 0) {
				await gMessage('성적서 작성', resData.msg, 'success', 'alert');
				$modal_root.modal('hide');  // 모달 닫기
			} else {
				await gMessage('성적서 작성 실패', resData.msg ?? '오류가 발생했습니다.', 'error', 'alert');
			}
		} catch (err) {
			swal.close();
			customAjaxHandler(err);
		}
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