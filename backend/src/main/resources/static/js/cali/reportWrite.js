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

	// 그리드 행 클릭 → 필수항목 검증 → 성적서작성 배치 생성
	// SampleReportWriteRow: getId() = file_info.id, getSampleId() = sample.id
	// 배치 생성 API에는 sample.id(sampleId)를 전달해야 하므로 row.sampleId 사용
	$modal.grid.on('click', async function (ev) {
		const { rowKey } = ev;
		const row = $modal.grid.getRow(rowKey);
		if (!row) return;

		const reportIds  = $modal.param?.reportIds ?? [];
		const sampleId   = row.sampleId;   // sample.id
		const sampleName = row.itemName ?? '';
		const fileName   = row.fileName ?? '';

		// 대상 성적서 목록 미전달 시 경고 (reportWrite 단독 접근 등)
		if (!reportIds || reportIds.length === 0) {
			gToast('대상 성적서 정보가 없습니다.', 'warning');
			return;
		}

		// ── Step 1. 필수값 검증 진행 여부 확인 ─────────────────────────────
		const confirmResult = await gMessage(
			'필수항목 검증',
			`선택한 샘플 <b>${sampleName}</b>(${fileName})로 성적서를 작성하기 전,<br>` +
			`대상 성적서 ${reportIds.length}건의 필수값 검증을 진행하겠습니다.`,
			'question',
			'confirm',
			{ confirmButtonText: '확인' }
		);
		if (!confirmResult.isConfirmed) return;

		// ── Step 2. 필수항목 검증 API 호출 ────────────────────────────────
		let validateData;
		try {
			gLoadingMessage('필수항목 데이터를 검증합니다.');
			const res = await fetch('/api/report/validateWrite', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json; charset=utf-8' },
				body: JSON.stringify({ reportIds }),
			});
			swal.close();
			if (!res.ok) throw res;
			const resData = await res.json();
			if (resData?.code > 0) {
				validateData = resData.data;
			} else {
				await gMessage('검증 오류', resData.msg ?? '검증 중 오류가 발생했습니다.', 'error', 'alert');
				return;
			}
		} catch (err) {
			swal.close();
			customAjaxHandler(err);
			return;
		}

		// ── Step 3. 검증 결과에 따라 분기 ─────────────────────────────────
		let targetIds;

		// ③-0 진행중 성적서가 1건이라도 있으면 완전 차단 (전체 진행 포함 모든 옵션 불가)
		if (validateData.hasInProgress) {
			const numList = validateData.inProgressReportNums.join(', ');
			await gMessage(
				'작업 불가',
				`이미 작업 중인 성적서가 존재합니다.<br>${numList}`,
				'error',
				'alert'
			);
			return;
		}

		if (validateData.allPassed) {
			// 모두 통과 → 성적서작성 최종 확인
			const writeResult = await gMessage(
				'성적서 작성',
				`성적서작성을 진행하시겠습니까?<br>대상: ${reportIds.length}건`,
				'question',
				'confirm',
				{ confirmButtonText: '진행' }
			);
			if (!writeResult.isConfirmed) return;
			targetIds = reportIds;

		} else {
			// 일부 실패 → 누락 항목 안내 + 3버튼 선택
			// 필드별 누락 성적서번호를 HTML로 포맷팅
			const failureHtml = validateData.failures
				.map(f => `<b>${f.field}</b><br>${f.reportNums.join(', ')}`)
				.join('<br><br>');

			const failResult = await Swal.fire({
				title: '필수값 누락 안내',
				html:
					`<div style="text-align:left; max-height:280px; overflow-y:auto; padding-right:6px; font-size:0.92em;">` +
					`다음 성적서들의 경우 필수값이 없습니다.<br><br>${failureHtml}` +
					`</div>`,
				icon: 'warning',
				showConfirmButton: true,
				showDenyButton: true,
				showCancelButton: true,
				confirmButtonText: '전체 진행',   // isConfirmed — 누락 성적서 포함 전체 진행
				denyButtonText:    '통과만 진행', // isDenied   — 검증 통과 성적서만 진행
				cancelButtonText:  '취소',
			});

			if (failResult.isConfirmed) {
				// 전체 진행 (누락 성적서 포함)
				targetIds = reportIds;

			} else if (failResult.isDenied) {
				// 통과 성적서만 진행
				if (!validateData.passedIds || validateData.passedIds.length === 0) {
					gToast('검증을 통과한 성적서가 없습니다.', 'warning');
					return;
				}
				targetIds = validateData.passedIds;

			} else {
				// 취소
				await gMessage('취소', '취소했습니다.', 'info', 'alert');
				return;
			}
		}

		// ── Step 4. 배치 생성 + Polling 진행상황 표시 ──────────────────────

		// 4-1. 배치 생성 API 호출
		let batchId;
		try {
			gLoadingMessage('성적서작성 작업을 준비합니다.');
			const res = await fetch('/api/report/jobs/batches', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json; charset=utf-8' },
				body: JSON.stringify({ reportIds: targetIds, sampleId }),
			});
			swal.close();
			if (!res.ok) throw res;
			const resData = await res.json();
			if (resData?.code > 0) {
				batchId = resData.data.batchId;
			} else {
				await gMessage('오류', resData.msg ?? '배치 생성 중 오류가 발생했습니다.', 'error', 'alert');
				return;
			}
		} catch (err) {
			swal.close();
			customAjaxHandler(err);
			return;
		}

		// 4-2. reportWrite 모달 닫기 — Swal 진행상황 창으로 전환
		$modal_root.modal('hide');

		// item 상태별 표시 레이블
		const STEP_LABEL = {
			DOWNLOADING_TEMPLATE: '샘플 다운로드',
			FILLING_DATA:         '데이터 삽입',
			UPLOADING_ORIGIN:     '파일 업로드',
			DONE:                 '완료',
		};

		// 진행상황 HTML 빌더 — Polling 결과를 받을 때마다 Swal HTML을 갱신하기 위해 사용
		const buildProgressHtml = (batch) => {
			const total   = batch.totalCount   ?? 0;
			const success = batch.successCount ?? 0;
			const fail    = batch.failCount    ?? 0;
			const done    = success + fail;
			const pct     = total > 0 ? Math.round((done / total) * 100) : 0;

			const itemRows = (batch.items ?? []).map(item => {
				const badge =
					item.status === 'SUCCESS'  ? '<span class="badge bg-success">완료</span>' :
					item.status === 'FAIL'     ? '<span class="badge bg-danger">실패</span>'  :
					item.status === 'PROGRESS' ? `<span class="badge bg-primary">${STEP_LABEL[item.step] ?? item.step ?? '처리중'}</span>` :
					                             '<span class="badge bg-secondary">대기</span>';
				const failMsg = item.message
					? `<br><small class="text-danger">${item.message}</small>`
					: '';
				return `<tr>
					<td class="text-start" style="font-size:0.85em;">성적서 #${item.reportId}</td>
					<td>${badge}${failMsg}</td>
				</tr>`;
			}).join('');

			return `
				<div class="mb-2">
					<div class="d-flex justify-content-between mb-1" style="font-size:0.85em;">
						<span>${done} / ${total}건 처리됨</span>
						<span>${pct}%</span>
					</div>
					<div class="progress" style="height:12px;">
						<div class="progress-bar progress-bar-striped progress-bar-animated"
							role="progressbar" style="width:${pct}%;"></div>
					</div>
				</div>
				<div style="max-height:200px; overflow-y:auto;">
					<table class="table table-sm table-bordered mb-0" style="font-size:0.85em;">
						<tbody>${itemRows}</tbody>
					</table>
				</div>`;
		};

		// 4-3. 진행상황 Swal 오픈
		Swal.fire({
			title: '성적서 작성 중...',
			html: '<div id="batchProgressBody">준비 중...</div>',
			allowOutsideClick: false,
			allowEscapeKey: false,
			showConfirmButton: false,
			didOpen: () => { Swal.showLoading(); },
		});

		// 4-4. Polling 루프 (5초 간격, 최대 5분)
		const POLL_INTERVAL_MS = 5000;
		const MAX_POLL_COUNT   = 60; // 5000ms × 60 = 300초(5분)
		let pollCount = 0;

		while (pollCount < MAX_POLL_COUNT) {
			await new Promise(r => setTimeout(r, POLL_INTERVAL_MS));
			pollCount++;

			try {
				const pollRes = await fetch(`/api/report/jobs/batches/${batchId}`);
				if (!pollRes.ok) continue;
				const pollData = await pollRes.json();
				if (!pollData || pollData.code <= 0) continue;

				const batch = pollData.data;

				// Swal 내부 HTML 갱신
				const progressEl = document.getElementById('batchProgressBody');
				if (progressEl) progressEl.innerHTML = buildProgressHtml(batch);

				// 종료 조건: 배치 상태가 완료/실패/취소이면 Polling 중단
				if (['SUCCESS', 'FAIL', 'CANCELED'].includes(batch.status)) {
					Swal.hideLoading();

					if (batch.status === 'SUCCESS') {
						const icon = batch.failCount > 0 ? 'warning' : 'success';
						await gMessage(
							'성적서 작성 완료',
							`성공 ${batch.successCount}건 / 실패 ${batch.failCount}건`,
							icon,
							'alert'
						);
					} else if (batch.status === 'FAIL') {
						await gMessage('성적서 작성 실패', `${batch.failCount}건 처리 실패`, 'error', 'alert');
					} else {
						await gMessage('작업 취소', '작업이 취소되었습니다.', 'info', 'alert');
					}
					// 확인 버튼 클릭 후 workApproval 그리드 리로드 요청
					$(document).trigger('reportWriteCompleted');
					break;
				}

			} catch (pollErr) {
				// Polling 중 일시적 네트워크 오류는 무시하고 다음 주기에 재시도
				console.warn('[reportWrite] Polling 오류:', pollErr);
			}
		}

		// 최대 횟수 초과 시 (배치가 정상 종료되지 않은 경우)
		if (pollCount >= MAX_POLL_COUNT) {
			Swal.close();
			await gMessage('시간 초과', '작업 진행상황을 확인할 수 없습니다.<br>잠시 후 다시 확인해 주세요.', 'warning', 'alert');
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