$(function () {
	console.log('++ cali/businessTripModify.js');

	// ── 표준 $modal / $modal_root 설정 ──────────────────────────
	const $candidates    = $('.modal-view:not(.modal-view-applied)');
	let   $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	// 모듈 레벨 변수 (init_modal 이후 할당, 이벤트 핸들러에서도 참조)
	let $form;
	let btripId;
	let isModify;
	const MAX_FILES     = 5;
	const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

	// ──────────────────────────────────────────
	// 그리드 삭제 버튼 formatter (공통)
	// columnName === 'deleteBtn' 일 때 ev.columnName으로 판단
	// ──────────────────────────────────────────
	function deleteButtonFormatter() {
		return '<button type="button" class="btn btn-danger btnDeleteGridRow" ' +
		       'style="width:100%;height:100%;min-height:26px;border-radius:0;border:0;padding:0;display:block;font-weight:bold;">×</button>';
	}

	// 출장일시 유효성 검사 (시작 >= 종료 방지)
	function validateDateRange() {
		const start = $('input[name=startDatetime]', $form).val();
		const end   = $('input[name=endDatetime]',   $form).val();
		if (!start || !end) return true;
		if (new Date(start) >= new Date(end)) {
			gToast('출장 종료일시는 시작일시보다 이후여야 합니다.', 'warning');
			return false;
		}
		return true;
	}

	// ──────────────────────────────────────────
	// $modal.updateEquipCnt — 표준장비 건수 표시 갱신
	// ──────────────────────────────────────────
	$modal.updateEquipCnt = function () {
		const cnt = $modal.equipGrid.getData().length;
		$('.equipCnt').text(cnt > 0 ? `(${cnt})` : '');
	};

	// ──────────────────────────────────────────
	// $modal.syncGridHeights — 좌측 그리드 높이 동기화
	// 표준장비 그리드 bodyHeight를 조정하여 출장차량 그리드 하단이
	// 우측 '비고' 행 하단과 평행이 되도록 맞춤
	// ──────────────────────────────────────────
	$modal.syncGridHeights = function () {
		requestAnimationFrame(() => {
			const rightH       = $('.col-6.pr-0.pl-1').outerHeight(true);
			const carCardH     = $('.carList').closest('.card').outerHeight(true);
			const equipHeaderH = $('.equipageList').closest('.card').find('.card-header').outerHeight(true);
			const cardBodyPad  = 8;   // card-body.p-1 상하 패딩 합 (4px × 2)
			const gridHeaderH  = 29;  // TUI Grid 헤더 행 기본 높이
			const mb2          = 8;   // 두 카드 사이 mb-2 여백

			const newBodyH = rightH - carCardH - mb2 - equipHeaderH - cardBodyPad - gridHeaderH;
			if (newBodyH > 80) {
				$modal.equipGrid.setBodyHeight(newBodyH);
			}
		});
	};

	// ──────────────────────────────────────────
	// $modal.initTravelerSelect — 교정실무자 selectpicker 초기화
	// @param {string[]} selectedIds — 선택값 복원 (수정 시)
	// ──────────────────────────────────────────
	$modal.initTravelerSelect = async function (selectedIds) {
		try {
			const res = await fetch('/api/admin/businessTrip/memberOptions');
			if (!res.ok) throw res;
			const json    = await res.json();
			const members = json.data || [];

			const $sel = $('select.travelerSelect', $form);
			$sel.empty();
			members.forEach(m => {
				$sel.append(`<option value="${m.id}">${m.name}</option>`);
			});

			$sel.selectpicker({
				liveSearch:          true,
				selectedTextFormat:  'count > 2',
				countSelectedText:   '{0}명 선택',
				noneSelectedText:    '실무자를 선택하세요.',
			});

			if (selectedIds && selectedIds.length > 0) {
				$sel.selectpicker('val', selectedIds.map(String));
			}
		} catch (e) {
			console.error('실무자 목록 로드 실패', e);
			gToast('실무자 목록을 불러오지 못했습니다.', 'error');
		}
	};

	// ──────────────────────────────────────────
	// $modal.loadDetail — 수정 모달 기존 데이터 로드
	// @param {string|number} id — 출장일정 id
	// ──────────────────────────────────────────
	// ──────────────────────────────────────────
	// $modal.loadDetail — 수정 모달 기존 데이터 로드
	// @param {string|number} id — 출장일정 id
	// @return {Array} 기존 표준장비 목록 (그리드 초기화 후 populate에 사용)
	//                 그리드는 init_modal에서 초기화되므로 여기서 직접 appendRow 불가
	// ──────────────────────────────────────────
	$modal.loadDetail = async function (id) {
		try {
			const res = await fetch(`/api/admin/businessTrip/${id}`);
			if (!res.ok) throw res;
			const d = (await res.json()).data;

			// datetime 필드는 datetime-local 형식으로 변환 후 setupValues로 일괄 세팅
			// btripId, travelerIds는 별도 처리 → setupValues 대상 제외
			const formData = {
				...d,
				startDatetime: toDatetimeLocal(d.startDatetime),
				endDatetime:   toDatetimeLocal(d.endDatetime),
			};
			$form.find('input[name], textarea[name], select[name]')
				.not('[name=btripId]').not('[name=travelerIds]')
				.setupValues(formData);

			// updateMemberName은 name 속성 없는 display 전용 input → 별도 세팅
			$('input.updateMemberNameDisplay', $form).val(d.updateMemberName ?? '');

			// 출장자 selectpicker 복원
			const selectedIds = d.travelerIds
				? d.travelerIds.split(',').map(s => s.trim()).filter(Boolean)
				: [];
			await $modal.initTravelerSelect(selectedIds);

			// 기존 표준장비 목록 조회 — 반환 후 그리드 초기화 시점에 populate
			// perPage=9999로 전체 로드 (출장 1건에 수십 건 이상 없음)
			try {
				const eqRes  = await fetch(`/api/equipment/getUsedEquipment?refTable=business_trip&refTableId=${id}&page=1&perPage=9999`);
				if (!eqRes.ok) throw eqRes;
				const eqJson = await eqRes.json();
				return eqJson.data?.contents ?? [];
			} catch (e) {
				console.error('기존 표준장비 로드 실패', e);
				return [];
			}

		} catch (e) {
			console.error('출장일정 상세 조회 실패', e);
			gApiErrorHandler(e);
			return [];
		}
	};

	// ──────────────────────────────────────────
	// $modal.openCustAgentSearch — 신청업체 조회 모달
	// ──────────────────────────────────────────
	$modal.openCustAgentSearch = async function () {
		const agentName = $('input[name=custAgent]', $form).val();
		const resModal  = await gModal(
			'/agent/searchAgentModify',
			{ agentFlag: 1, agentName },
			{ title: '업체 조회', size: 'xxl', show_close_button: true, show_confirm_button: false }
		);
		if (resModal?.returnData) {
			const d = resModal.returnData;
			$('input[name=custAgentId]',      $form).val(d.id          ?? '');
			$('input[name=custAgent]',        $form).val(d.name        ?? '');
			$('input[name=custAgentAddr]',    $form).val(d.addr        ?? '');
			$('input[name=custManager]',      $form).val(d.managerName ?? '');
			$('input[name=custManagerTel]',   $form).val(d.managerTel  ?? '');
			$('input[name=custManagerEmail]', $form).val(d.managerEmail ?? '');
		}
	};

	// ──────────────────────────────────────────
	// $modal.openReportAgentSearch — 성적서발행처 조회 모달
	// ──────────────────────────────────────────
	$modal.openReportAgentSearch = async function () {
		const agentName = $('input[name=reportAgent]', $form).val();
		const resModal  = await gModal(
			'/agent/searchAgentModify',
			{ agentFlag: 4, agentName },
			{ title: '업체 조회', size: 'xxl', show_close_button: true, show_confirm_button: false }
		);
		if (resModal?.returnData) {
			const d = resModal.returnData;
			$('input[name=reportAgentId]',    $form).val(d.id          ?? '');
			$('input[name=reportAgent]',      $form).val(d.name        ?? '');
			$('input[name=reportAgentAddr]',  $form).val(d.addr        ?? '');
			$('input[name=reportManager]',    $form).val(d.managerName ?? '');
			$('input[name=reportManagerTel]', $form).val(d.managerTel  ?? '');
		}
	};

	// ──────────────────────────────────────────
	// $modal.init_modal — 모달 초기화
	// setTimeout 200ms 후 호출됨 (DOM 렌더링 완료 보장)
	// ──────────────────────────────────────────
	$modal.init_modal = async (param) => {
		$modal.param = param;

		// $form은 모달 컨텍스트 내에서 탐색 (모듈 레벨 변수에 할당)
		$form    = $('form.btripModifyForm', $modal);
		btripId  = $('input[name=btripId]', $form).val();
		isModify = btripId && btripId !== 'null' && btripId !== '';

		// ── 데이터 로드 먼저 (그리드 초기화 전 폼 세팅) ────────────
		// 수정 시: API로 상세 데이터를 가져와 setupValues로 폼 채움
		//          기존 표준장비 목록도 반환받아 그리드 초기화 후 populate
		// 등록 시: 교정실무자 selectpicker만 초기화, 기본 날짜 세팅
		let savedEquipList = [];
		if (isModify) {
			savedEquipList = await $modal.loadDetail(btripId) ?? [];
		} else {
			await $modal.initTravelerSelect([]);

			// 시작/종료일 기본값 세팅
			// - 더블클릭 진입: $modal.param.defaultDate = 클릭한 날짜 (yyyy-MM-dd)
			// - 일정등록 버튼 진입: defaultDate 없음 → 오늘 날짜 사용
			const defaultDate = $modal.param?.defaultDate
				|| new Date().toISOString().substring(0, 10);
			$('input[name=startDatetime]', $form).val(defaultDate + 'T08:00');
			$('input[name=endDatetime]',   $form).val(defaultDate + 'T17:00');
		}

		// ── 표준장비 그리드 ────────────────────────────
		$modal.equipGrid = gGrid('.equipageList', {
			columns: [
				{ name: 'equipmentId', hidden: true },
				{ header: '관리번호',   name: 'manageNo',     width: 90 },
				{ header: '기기명',     name: 'name'                    }, // 자동 채움
				{ header: '제작회사',   name: 'makeAgent',    width: 90 },
				{ header: '형식',       name: 'modelName',    width: 80 },
				{ header: '기기번호',   name: 'serialNo',     width: 90 },
				{ header: '차기교정일', name: 'nextCaliDate', width: 90 },
				{ header: '교정기관',   name: 'caliAgent',    width: 90 },
				{
					header:    '-',
					name:      'deleteBtn',
					width:     36,
					align:     'center',
					sortable:  false,
					className: 'delete-col',
					formatter: deleteButtonFormatter,
				},
			],
			draggable:   true,
			pageOptions: false,  // 로컬 데이터 — 페이지네이션 불필요
			minBodyHeight: 150,
			bodyHeight:    180,
			data: [],
		});

		// 표준장비 삭제 버튼 (TUI Grid click 이벤트는 ev.columnName으로 판단)
		$modal.equipGrid.on('click', function (ev) {
			if (ev.columnName === 'deleteBtn') {
				$modal.equipGrid.removeRow(ev.rowKey);
				$modal.updateEquipCnt();
			}
		});

		// 수정 시: 기존 표준장비 목록 populate (loadDetail 반환값)
		// UsedEquipmentListPr 기준 — equipmentId, manageNo, name, makeAgent, serialNo 제공
		// modelName, nextCaliDate, caliAgent는 ref 테이블에 없어 빈값 표시
		if (savedEquipList.length > 0) {
			savedEquipList.forEach(row => {
				$modal.equipGrid.appendRow({
					equipmentId:  row.equipmentId  ?? '',
					manageNo:     row.manageNo      ?? '',
					name:         row.name          ?? '',
					makeAgent:    row.makeAgent     ?? '',
					modelName:    '',
					serialNo:     row.serialNo      ?? '',
					nextCaliDate: '',
					caliAgent:    '',
				});
			});
			$modal.updateEquipCnt();
		}

		// ── 출장차량 그리드 ────────────────────────────
		$modal.carGrid = gGrid('.carList', {
			columns: [
				{ header: '차명',     name: 'carName' }, // 자동 채움
				{ header: '차량번호', name: 'carNum'  },
				{ header: '년식',     name: 'carYear' },
				{
					header:    '-',
					name:      'deleteBtn',
					width:     36,
					align:     'center',
					sortable:  false,
					className: 'delete-col',
					formatter: deleteButtonFormatter,
				},
			],
			pageOptions:   false,
			minBodyHeight: 120,
			bodyHeight:    150,
			data: [],
		});

		// 출장차량 삭제 버튼
		$modal.carGrid.on('click', function (ev) {
			if (ev.columnName === 'deleteBtn') {
				$modal.carGrid.removeRow(ev.rowKey);
			}
		});

		// ── 높이 동기화 ────────────────────────────────
		$modal.syncGridHeights();
	};

	// ──────────────────────────────────────────
	// 이벤트 핸들러 (모달 루트에 위임, init_modal 이전에 바인딩)
	// ──────────────────────────────────────────
	$modal
		// 출장일시 유효성 검사
		.on('change', 'input[name=startDatetime]', function () {
			if ($('input[name=endDatetime]', $form).val()) {
				if (!validateDateRange()) $(this).val('');
			}
		})
		.on('change', 'input[name=endDatetime]', function () {
			if ($('input[name=startDatetime]', $form).val()) {
				if (!validateDateRange()) $(this).val('');
			}
		})
		// 표준장비 조회 버튼
		// reportModify 패턴: row.id = row.equipmentId 변환 후 전달 (중복 선택 방지)
		.on('click', '.btnSearchEquip', async function () {
			const equipDatas = $modal.equipGrid.getData();
			equipDatas.forEach(row => { row.id = row.equipmentId; });

			const resModal = await gModal(
				'/equipment/searchEquipmentList',
				{ equipDatas },
				{
					title:               '표준장비 조회',
					size:                'xxxl',
					show_close_button:   true,
					show_confirm_button: true,
					confirm_button_text: '선택',
				}
			);
			if (resModal?.jsonData?.length > 0) {
				$modal.equipGrid.clear();
				resModal.jsonData.forEach(row => {
					$modal.equipGrid.appendRow({
						equipmentId:  row.id,
						manageNo:     row.manageNo     ?? '',
						name:         row.name         ?? '',
						makeAgent:    row.makeAgent    ?? '',
						modelName:    row.modelName    ?? '',
						serialNo:     row.serialNo     ?? '',
						nextCaliDate: row.nextCaliDate ?? '',
						caliAgent:    row.caliAgent    ?? '',
					});
				});
				$modal.updateEquipCnt();
			}
		})
		// 출장차량 조회 버튼 (미구현)
		.on('click', '.btnSearchCar', function () {
			gToast('구현 준비중입니다.', 'info');
		})
		// 신청업체 조회 (버튼 or readonly input 클릭)
		.on('click', '.btnSearchCustAgent',   () => $modal.openCustAgentSearch())
		.on('click', 'input[name=custAgent]', () => $modal.openCustAgentSearch())
		// 성적서발행처 조회 (버튼 or readonly input 클릭)
		.on('click', '.btnSearchReportAgent',   () => $modal.openReportAgentSearch())
		.on('click', 'input[name=reportAgent]', () => $modal.openReportAgentSearch())
		// 주소 검색 (Daum 우편번호)
		// data-addr-target 속성에 지정된 input name 필드에 결과 세팅
		.on('click', '.btnSearchAddr', async function () {
			const addrField = $(this).data('addr-target');
			await sample4ExecDaumPostcode('', addrField, '');
		})
		// 첨부파일 선택 — 크기/개수 검증 (실제 업로드는 저장 시 처리)
		.on('change', '.uploadFiles', function () {
			const $input    = $(this);
			const $newInput = $input.clone().val('');
			const files     = this.files;
			if (!files || files.length === 0) return;

			if (files.length > MAX_FILES) {
				gToast(`파일은 한 번에 최대 ${MAX_FILES}개까지만 업로드 가능합니다.`, 'warning');
				$input.replaceWith($newInput);
				return;
			}
			for (let i = 0; i < files.length; i++) {
				if (files[i].size > MAX_FILE_SIZE) {
					const sizeMB = (files[i].size / (1024 * 1024)).toFixed(2);
					gToast(`'${files[i].name}' 파일(${sizeMB}MB)은 허용 용량(최대 10MB)을 초과합니다.`, 'warning');
					$input.replaceWith($newInput);
					return;
				}
			}
		})
		// 기존 첨부파일 조회 버튼
		// - val > 0: 파란색 버튼 → fileList 모달 오픈
		// - val = 0: 회색 버튼 → 토스트
		.on('click', '.searchFile', async function () {
			const $btn    = $(this);
			const fileCnt = parseInt($btn.val(), 10) || 0;

			if (fileCnt > 0) {
				const fileResult = await gModal(
					'/basic/fileList',
					{ refTableName: 'business_trip', refTableId: btripId },
					{ size: 'lg', title: '첨부파일 확인', show_close_button: true, show_confirm_button: false }
				);
				// 모달에서 파일을 모두 삭제한 경우 버튼 회색 전환
				if (fileResult?.fileCnt === 0) {
					$btn.val(0).removeClass('btn-primary').addClass('btn-secondary');
				}
			} else {
				gToast('등록된 첨부파일이 없습니다.', 'warning');
			}
		});

	// ──────────────────────────────────────────
	// $modal.confirm_modal — 저장 버튼 콜백
	// 필수값 검증 → gMessage confirm → FormData(JSON Blob + 파일) → POST/PATCH
	// true 반환 시 모달 닫힘, false 반환 시 유지
	// ──────────────────────────────────────────
	$modal.confirm_modal = async function () {
		// ── 필수값 검증 ──────────────────────────
		const title         = $('input[name=title]',         $form).val().trim();
		const startDatetime = $('input[name=startDatetime]', $form).val();
		const endDatetime   = $('input[name=endDatetime]',   $form).val();

		if (!title) {
			gToast('일정 제목을 입력하세요.', 'warning');
			return false;
		}
		if (!startDatetime) {
			gToast('출장 시작일시를 입력하세요.', 'warning');
			return false;
		}
		if (!endDatetime) {
			gToast('출장 종료일시를 입력하세요.', 'warning');
			return false;
		}
		if (!validateDateRange()) return false;

		// ── 저장 확인 ──────────────────────────
		const confirmResult = await gMessage(
			isModify ? '출장일정을 수정하시겠습니까?' : '출장일정을 등록하시겠습니까?',
			'', 'question', 'confirm'
		);
		if (confirmResult.isConfirmed !== true) return false;

		// ── 요청 데이터 구성 ────────────────────
		// 빈 문자열은 null로 전송 (백엔드 @Valid 통과 + DB nullable 처리)
		const v = (name) => $(`input[name=${name}]`, $form).val().trim() || null;
		const reqData = {
			title,
			type:             $('select[name=type]',          $form).val()  || null,
			startDatetime,
			endDatetime,
			custAgentId:      $('input[name=custAgentId]',    $form).val()  || null,
			custAgent:        v('custAgent'),
			custAgentAddr:    v('custAgentAddr'),
			custManager:      v('custManager'),
			custManagerTel:   v('custManagerTel'),
			custManagerEmail: v('custManagerEmail'),
			reportAgentId:    $('input[name=reportAgentId]',  $form).val()  || null,
			reportAgent:      v('reportAgent'),
			reportAgentAddr:  v('reportAgentAddr'),
			reportManager:    v('reportManager'),
			reportManagerTel: v('reportManagerTel'),
			siteAddr:         v('siteAddr'),
			siteManager:      v('siteManager'),
			siteManagerTel:   v('siteManagerTel'),
			siteManagerEmail: v('siteManagerEmail'),
			// 교정실무자: selectpicker val()은 배열 → 콤마 구분 문자열로 변환
			travelerIds:      $('select.travelerSelect', $form).val()?.join(',') || null,
			remark:           $('textarea[name=remark]', $form).val().trim()     || null,
			// 표준장비: 그리드 행 → {refTable, refTableId, equipmentId, seq} 배열
			// refTable/refTableId는 서비스에서 덮어쓰므로 더미값 전송
			equipmentDatas:   $modal.equipGrid.getData().map((row, idx) => ({
				refTable:    'business_trip',
				refTableId:  isModify ? Number(btripId) : null,
				equipmentId: row.equipmentId,
				seq:         idx,
			})),
		};

		// ── FormData 구성 (multipart) ──────────
		// Content-Type 헤더 미지정 → 브라우저가 boundary 자동 설정
		const fd        = new FormData();
		const partName  = isModify ? 'updateReq' : 'createReq';
		fd.append(partName, new Blob([JSON.stringify(reqData)], { type: 'application/json' }));

		const $fileInput = $('input.uploadFiles', $form);
		if ($fileInput[0]?.files?.length > 0) {
			Array.from($fileInput[0].files).forEach(f => fd.append('files', f));
		}

		// ── API 요청 ────────────────────────────
		try {
			const url    = isModify
				? `/api/admin/businessTrip/${btripId}`
				: '/api/admin/businessTrip';
			const method = isModify ? 'PATCH' : 'POST';
			const res    = await fetch(url, { method, body: fd });
			if (!res.ok) throw res;

			await gMessage(
				isModify ? '출장일정 수정 완료' : '출장일정 등록 완료',
				isModify ? '출장일정이 수정되었습니다.' : '출장일정이 등록되었습니다.',
				'success'
			);
			return true; // true 반환 → 모달 닫힘
		} catch (e) {
			console.error('출장일정 저장 실패', e);
			gApiErrorHandler(e);
			return false;
		}
	};

	// ── 표준 모달 초기화 footer ──────────────────────────────────
	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		// 모달 팝업창인 경우 DOM 렌더링 완료 후 init_modal 호출 (200ms 대기)
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
