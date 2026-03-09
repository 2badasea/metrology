$(function () {
	console.log('++ basic/sampleManage.js');

	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		$modal = $notModalViewAppliedEle.first();
	}
	let $modal_root = $modal.closest('.modal');

	let middleItemCodeSet = [];
	let smallItemCodeSet = {};
	let selectedRowKey = null;    // 현재 선택된 그리드 rowKey
	let selectedSampleId = null;  // 현재 선택된 샘플 ID
	let allFiles = [];            // 현재 로드된 파일 캐시 (클라이언트 필터용)

	$modal.init_modal = async (param) => {
		$modal.param = param;

		await $modal.initItemCodeSet();

		// ── 왼쪽 그리드: 샘플 목록 (서버 페이징) ──
		const sampleDataSource = {
			api: {
				readData: {
					url: '/api/sample/list',
					method: 'GET',
					serializer: (gridParam) => {
						const middleId = $('.sampleListForm .middleCodeSelect', $modal).val();
						const smallId  = $('.sampleListForm .smallCodeSelect', $modal).val();
						if (middleId) gridParam.middleItemCodeId = middleId;
						if (smallId)  gridParam.smallItemCodeId  = smallId;
						gridParam.searchType = 'name';
						gridParam.keyword    = $('.sampleListForm input[name=keyword]', $modal).val().trim();
						return $.param(gridParam);
					},
				},
			},
		};

		$modal.sampleListGrid = gGrid('.sampleListGrid', {
			columns: [
				{
					header: '중분류',
					name: 'middleInfo',
					align: 'center',
					width: 150,
					className: 'cursor_pointer',
					formatter: ({ row }) => {
						const code = row.middleCodeNum ?? '';
						const name = row.middleCodeName ?? '';
						return code && name ? `${code}<br>${name}` : (code || name || '');
					},
				},
				{
					header: '소분류',
					name: 'smallInfo',
					align: 'center',
					width: 210,
					className: 'cursor_pointer',
					formatter: ({ row }) => {
						const code = row.smallCodeNum ?? '';
						const name = row.smallCodeName ?? '';
						return code && name ? `${code}<br>${name}` : (code || name || '');
					},
				},
				{
					header: '기기명',
					name: 'name',
					align: 'center',
					className: 'cursor_pointer',
				},
			],
			pageOptions: {
				useClient: false,
				perPage: 25,
			},
			rowHeaders: ['checkbox'],
			data: sampleDataSource,
			minBodyHeight: 639,
			bodyHeight: 639,
			rowHeight: 'auto',
			scrollY: true,
		});

		// ── 오른쪽 그리드: 파일 목록 ──
		$modal.sampleFileGrid = gGrid('.sampleFileGrid', {
			columns: [
				{
					header: '파일명',
					name: 'fileName',
					align: 'center',
					className: 'cursor_pointer',
				},
				{
					header: '등록자',
					name: 'writerName',
					align: 'center',
					width: 90,
				},
				{
					header: '등록일시',
					name: 'createDatetime',
					align: 'center',
					width: 140,
					formatter: ({ value }) => {
						if (!value) return '';
						if (Array.isArray(value)) {
							const [y, M, d, h, m] = value;
							return `${y}-${String(M).padStart(2, '0')}-${String(d).padStart(2, '0')} `
								+ `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
						}
						return String(value).substring(0, 16).replace('T', ' ');
					},
				},
				{
					header: '-',
					name: 'deleteBtn',
					align: 'center',
					width: 60,
					formatter: () =>
						`<button type='button' class='btn btn-danger w-100 h-100 rounded-0'>`
						+ `<i class="bi bi-trash"></i></button>`,
				},
			],
			bodyHeight: 420,
			minBodyHeight: 420,
			rowHeight: 'auto',
			scrollY: true,
		});

		// ── 이벤트 바인딩 ──

		// 중분류 변경 → 소분류 연동 (검색 폼)
		$('.sampleListForm .middleCodeSelect', $modal).on('change', function () {
			$modal.updateSmallOptions('.sampleListForm .smallCodeSelect', $(this).val());
		});

		// 중분류 변경 → 소분류 연동 (우측 입력 폼)
		$('.sampleFileForm .middleCodeSelect', $modal).on('change', function () {
			$modal.updateSmallOptions('.sampleFileForm .smallCodeSelect', $(this).val());
		});

		// 검색 폼 submit
		$('.sampleListForm', $modal).on('submit', (e) => {
			e.preventDefault();
			$modal.sampleListGrid.reloadData();
		});

		// 왼쪽 그리드 행 클릭 (선택 / 토글 해제)
		$modal.sampleListGrid.on('click', (ev) => {
			if (ev.rowKey == null || ev.columnName === '_checked') return;
			if (selectedRowKey !== null && selectedRowKey === ev.rowKey) {
				// 같은 행 재클릭 → 선택 해제
				$modal.sampleListGrid.removeRowClassName(ev.rowKey, 'gridFocused');
				selectedRowKey = null;
				selectedSampleId = null;
				$modal.resetForm();
			} else {
				if (selectedRowKey !== null) {
					$modal.sampleListGrid.removeRowClassName(selectedRowKey, 'gridFocused');
				}
				selectedRowKey = ev.rowKey;
				const row = $modal.sampleListGrid.getRow(ev.rowKey);
				selectedSampleId = row?.id ?? null;
				$modal.sampleListGrid.addRowClassName(ev.rowKey, 'gridFocused');
				$modal.loadDetail(row);
			}
		});

		// 신규 버튼 (선택 초기화 + 폼 리셋)
		$('.reset', $modal).on('click', () => {
			if (selectedRowKey !== null) {
				$modal.sampleListGrid.removeRowClassName(selectedRowKey, 'gridFocused');
				selectedRowKey = null;
				selectedSampleId = null;
			}
			$modal.resetForm();
		});

		// 저장 버튼
		$('.saveBtn', $modal).on('click', () => $modal.saveSample());

		// 샘플 삭제 버튼
		$('.deleteSample', $modal).on('click', () => $modal.deleteSamples());

		// 파일 그리드 클릭 (파일명 → 다운로드 / 삭제 버튼 → 삭제)
		$modal.sampleFileGrid.on('click', (ev) => {
			if (ev.rowKey == null) return;
			const row = $modal.sampleFileGrid.getRow(ev.rowKey);
			if (!row) return;
			const target = ev.nativeEvent?.target;
			if (target && target.closest('.btn-danger')) {
				$modal.deleteFile(row.id);
			} else if (ev.columnName === 'fileName') {
				window.location.href = `/api/file/fileDownload/${row.id}`;
			}
		});

		// 파일 검색 버튼 (클라이언트 필터)
		$('.searchFileBtn', $modal).on('click', () => {
			const keyword = $('.fileKeyword', $modal).val().trim().toLowerCase();
			const filtered = keyword
				? allFiles.filter(f => (f.fileName ?? '').toLowerCase().includes(keyword))
				: allFiles;
			$modal.sampleFileGrid.resetData(filtered);
		});
	};

	// ── 초기화: 중분류/소분류 코드셋 로드 ──
	$modal.initItemCodeSet = async () => {
		try {
			const res = await gAjax('/api/basic/getItemCodeInfos', {}, { type: 'GET' });
			if (res?.code > 0) {
				const itemCodeSet = res.data;
				if (itemCodeSet.middleCodeInfos) {
					middleItemCodeSet = itemCodeSet.middleCodeInfos;
					// 검색폼 + 입력폼 양쪽에 중분류 옵션 추가
					const $allMiddleSel = $('.middleCodeSelect', $modal);
					$.each(middleItemCodeSet, (_, row) => {
						$allMiddleSel.append(new Option(`${row.codeNum} ${row.codeName}`, row.id));
					});
				}
				if (itemCodeSet.smallCodeInfos) {
					smallItemCodeSet = itemCodeSet.smallCodeInfos;
				}
			} else {
				throw new Error('분류코드 정보를 불러오지 못했습니다.');
			}
		} catch (e) {
			gApiErrorHandler(e);
		}
	};

	// 소분류 select 옵션 업데이트 (middleId 기반)
	$modal.updateSmallOptions = (selector, middleId) => {
		const $select = $(selector, $modal);
		const firstOpt = $select.find('option:first').clone();
		$select.empty().append(firstOpt);
		const smallList = smallItemCodeSet[middleId] || [];
		smallList.forEach(item => {
			$select.append(new Option(`${item.codeNum} ${item.codeName}`, item.id));
		});
	};

	// 행 선택 시 우측 폼 및 파일 그리드 채우기
	$modal.loadDetail = async (row) => {
		const $form = $('.sampleFileForm', $modal);

		// 중분류 설정
		$('.middleCodeSelect', $form).val(row.middleItemCodeId);

		// 소분류 옵션 갱신 후 선택
		$modal.updateSmallOptions('.sampleFileForm .smallCodeSelect', row.middleItemCodeId);
		$('.smallCodeSelect', $form).val(row.smallItemCodeId);

		// 기기명
		$('input[name=name]', $form).val(row.name);

		// 파일 input 초기화
		$('.sampleFiles', $modal).val('');

		// 파일 그리드 로드
		allFiles = [];
		$modal.sampleFileGrid.resetData([]);
		$('.fileKeyword', $modal).val('');
		await $modal.loadFiles(row.id);
	};

	// 파일 목록 API 조회
	$modal.loadFiles = async (sampleId) => {
		try {
			const res = await gAjax(`/api/sample/${sampleId}/files`, {}, { type: 'GET' });
			if (res?.code > 0) {
				allFiles = res.data || [];
				$modal.sampleFileGrid.resetData(allFiles);
			}
		} catch (e) {
			gApiErrorHandler(e);
		}
	};

	// 우측 폼 초기화 (신규 상태)
	$modal.resetForm = () => {
		const $form = $('.sampleFileForm', $modal);
		$form[0].reset();
		// 소분류 select: 첫 번째 옵션만 남기기
		const $smallSel = $('.smallCodeSelect', $form);
		$smallSel.empty().append(new Option('선택하세요', '0'));
		// 파일 그리드·검색 초기화
		allFiles = [];
		$modal.sampleFileGrid.resetData([]);
		$('.fileKeyword', $modal).val('');
	};

	// ── 저장 ──
	$modal.saveSample = async () => {
		const $form = $('.sampleFileForm', $modal);
		const middleItemCodeId = parseInt($('.middleCodeSelect', $form).val()) || 0;
		const smallItemCodeId  = parseInt($('.smallCodeSelect', $form).val()) || 0;
		const name = $('input[name=name]', $form).val().trim();
		const file = $('.sampleFiles', $modal)[0]?.files[0] ?? null;

		// 입력 검증
		if (!name)                   return gErrorHandler('기기명을 입력해주세요.');
		if (!middleItemCodeId)        return gErrorHandler('중분류를 선택해주세요.');
		if (!smallItemCodeId)         return gErrorHandler('소분류를 선택해주세요.');

		const saveConfirm = await gMessage('샘플 저장', '저장하시겠습니까?', 'question', 'confirm');
		if (!saveConfirm.isConfirmed) return;

		try {
			if (selectedSampleId !== null) {
				// 수정
				await $modal.callSaveApi('PATCH', `/api/sample/${selectedSampleId}`,
					middleItemCodeId, smallItemCodeId, name, file);
				gToast('수정되었습니다.');
			} else {
				// 신규 — 중복 체크
				const dupRes = await gAjax('/api/sample/checkDuplicate',
					{ middleItemCodeId, smallItemCodeId, name }, { type: 'GET' });
				if (dupRes?.code > 0 && dupRes.data?.exists) {
					const existingId = dupRes.data.sampleId;
					if (file) {
						// 중복 + 파일 있음 → 기존 샘플에 파일 추가 여부 확인
						const addConfirm = await gMessage(
							'중복 샘플 확인',
							'동일한 샘플이 이미 존재합니다.<br>파일을 기존 샘플에 추가하시겠습니까?',
							'warning',
							'confirm'
						);
						if (!addConfirm.isConfirmed) return;
						await $modal.callSaveApi('PATCH', `/api/sample/${existingId}`,
							middleItemCodeId, smallItemCodeId, name, file);
						gToast('기존 샘플에 파일이 추가되었습니다.');
					} else {
						// 중복 + 파일 없음 → 차단
						return gErrorHandler('동일한 샘플이 이미 존재합니다.');
					}
				} else {
					// 신규 등록
					await $modal.callSaveApi('POST', '/api/sample',
						middleItemCodeId, smallItemCodeId, name, file);
					gToast('등록되었습니다.');
				}
			}

			// 저장 후 처리: 목록 갱신 + 선택 초기화
			$modal.sampleListGrid.reloadData();
			if (selectedRowKey !== null) {
				$modal.sampleListGrid.removeRowClassName(selectedRowKey, 'gridFocused');
				selectedRowKey = null;
				selectedSampleId = null;
			}
			$modal.resetForm();
		} catch (e) {
			gApiErrorHandler(e);
		}
	};

	// multipart 저장 API 호출 공통 함수 (fetch 사용)
	$modal.callSaveApi = async (method, url, middleItemCodeId, smallItemCodeId, name, file) => {
		const formData = new FormData();
		formData.append('data',
			new Blob([JSON.stringify({ name, middleItemCodeId, smallItemCodeId })],
				{ type: 'application/json' }));
		if (file) formData.append('file', file);

		const res = await fetch(url, { method, body: formData });
		if (!res.ok) throw res;
		const json = await res.json();
		if (!json || json.code <= 0) throw new Error(json?.msg || '저장 실패');
		return json;
	};

	// ── 샘플 삭제 ──
	$modal.deleteSamples = async () => {
		const checkedRows = $modal.sampleListGrid.getCheckedRows();
		if (!checkedRows.length) return gErrorHandler('삭제할 항목을 선택해주세요.');

		const delConfirm = await gMessage(
			'샘플 삭제',
			`선택한 ${checkedRows.length}건을 삭제하시겠습니까?`
			+ `<br><small class="text-danger">연관 파일도 함께 삭제됩니다.</small>`,
			'warning',
			'confirm'
		);
		if (!delConfirm.isConfirmed) return;

		try {
			const ids = checkedRows.map(row => row.id);
			const res = await fetch('/api/sample', {
				method: 'DELETE',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ ids }),
			});
			if (!res.ok) throw res;
			const json = await res.json();
			if (!json || json.code <= 0) throw new Error(json?.msg || '삭제 실패');

			gToast('삭제되었습니다.');
			$modal.sampleListGrid.reloadData();
			selectedRowKey = null;
			selectedSampleId = null;
			$modal.resetForm();
		} catch (e) {
			gApiErrorHandler(e);
		}
	};

	// ── 파일 삭제 ──
	$modal.deleteFile = async (fileId) => {
		const delConfirm = await gMessage('파일 삭제', '해당 파일을 삭제하시겠습니까?', 'warning', 'confirm');
		if (!delConfirm.isConfirmed) return;

		try {
			const res = await fetch(`/api/sample/files/${fileId}`, { method: 'DELETE' });
			if (!res.ok) throw res;
			const json = await res.json();
			if (!json || json.code <= 0) throw new Error(json?.msg || '삭제 실패');

			gToast('파일이 삭제되었습니다.');
			allFiles = allFiles.filter(f => f.id !== fileId);
			$modal.sampleFileGrid.resetData(allFiles);
		} catch (e) {
			gApiErrorHandler(e);
		}
	};

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		const p = $modal.data('param') || {};
		$modal.init_modal(p);
		if (typeof $modal.grid == 'object') {
			$modal.grid.refreshLayout();
		}
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});
