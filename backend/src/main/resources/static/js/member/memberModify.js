$(function () {
	console.log('++ member/memberModify.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	urlSearch = new URLSearchParams(location.search); // 쿼리스트링 가져오기 (get으로 파라미터값을 가져올 수 있다.)
	const memberId = urlSearch.get('id');

	const menuPath = `직원관리 - 직원${memberId == null ? '등록' : '수정'}`;
	$('.topbar-inner .customBreadcrumb').text(menuPath);

	const $form = $('form.memberModifyForm', $modal);

	// 직원 등록/수정 페이지
	$modal.init_modal = async (param) => {
		$modal.param = param;

		// 직급관리, 부서관리 정보 세팅
		await $modal.setBasicOptions();

		// 수정인 경우, 직원정보를 세팅한다.
		if ((memberId != null) & (memberId > 0)) {
			// try {
			// 	const resGetInfo = await g_ajax(
			// 		`/api/member/getMemberInfo/${memberId}`,
			// 		{},
			// 		{
			// 			type: 'GET',
			// 		}
			// 	);
			// 	console.log('🚀 ~ resGetInfo:', resGetInfo);

			// 	if (resGetInfo?.code > 0) {
			// 		// 데이터 세팅
			// 		if (resGetInfo.data != undefined) {
			// 			console.log('데이터 조회');
			// 			$form.setupValues(resGetInfo.data);
			// 		}
			// 	}
			// } catch (xhr) {
			// 	console.error(xhr);
			// 	custom_ajax_handler(xhr);
			// } finally {
			// }
		}

		// 비트 상수
		$modal.AUTH_BIT = {
			WORKER: 1, // 실무자
			TECH_SUB: 2, // 기술책임자(부)
			TECH_MAIN: 4, // 기술책임자(정)
		};

		// authBitmask -> boolean 3개로 풀기
		$modal.applyAuthMaskToRow = (row) => {
			const mask = Number(row.authBitmask ?? 0);
			row.isWorker = (mask & $modal.AUTH_BIT.WORKER) !== 0;
			row.isTechSub = (mask & $modal.AUTH_BIT.TECH_SUB) !== 0;
			row.isTechMain = (mask & $modal.AUTH_BIT.TECH_MAIN) !== 0;
			return row;
		};

		// boolean 3개 -> authBitmask 합치기
		$modal.buildAuthMask = (row) => {
			let mask = 0;
			if (row.isWorker === true) mask |= $modal.AUTH_BIT.WORKER;
			if (row.isTechSub === true) mask |= $modal.AUTH_BIT.TECH_SUB;
			if (row.isTechMain === true) mask |= $modal.AUTH_BIT.TECH_MAIN;
			return mask;
		};

		// 헤더(텍스트 + 체크박스) DOM 생성
		$modal.createHeaderCheckbox = (title, columnName) => {
			const wrap = document.createElement('div');
			wrap.style.display = 'flex';
			wrap.style.alignItems = 'center';
			wrap.style.justifyContent = 'center';
			wrap.style.gap = '6px';

			const text = document.createElement('span');
			text.textContent = title;

			const cb = document.createElement('input');
			cb.type = 'checkbox';
			cb.className = 'hdr-auth-checkbox';
			cb.dataset.col = columnName;

			wrap.appendChild(text);
			wrap.appendChild(cb);
			return wrap;
		};

		// 헤더 체크박스 상태(전체/부분/없음) 동기화
		$modal.syncHeaderCheckboxState = (grid, columnName, headerEl) => {
			const cb = headerEl.querySelector('input.hdr-auth-checkbox');
			if (!cb) return;

			const values = grid.getColumnValues(columnName) || [];
			const total = values.length;
			const checkedCount = values.reduce((acc, v) => acc + (v ? 1 : 0), 0);

			if (total === 0) {
				cb.checked = false;
				cb.indeterminate = false;
				return;
			}

			cb.checked = checkedCount === total;
			cb.indeterminate = checkedCount > 0 && checkedCount < total;
		};

		// 헤더 체크박스 클릭 시 해당 컬럼 전체 토글
		$modal.bindHeaderCheckbox = (grid, columnName, headerEl) => {
			const cb = headerEl.querySelector('input.hdr-auth-checkbox');
			if (!cb) return;

			// 헤더 체크박스가 “보이긴 하는데 클릭이 안 먹는” 현상(헤더 셀 이벤트와 충돌) 방지
			cb.addEventListener('mousedown', (e) => e.stopPropagation());

			cb.addEventListener('click', (e) => {
				e.stopPropagation(); // 헤더 클릭 이벤트(정렬 등) 충돌 방지
				grid.finishEditing(); // 편집중이면 마무리

				const checked = cb.checked;
				grid.setColumnValues(columnName, checked);

				// 즉시 헤더 상태 반영(부분 체크는 afterChange에서도 동기화됨)
				$modal.syncHeaderCheckboxState(grid, columnName, headerEl);
			});
		};

		// 체크박스 컬럼 공통 옵션
		// const checkboxColumn = (title, name, headerEl, width) => ({
		// 	header: title,
		// 	name,
		// 	width,
		// 	align: 'center',
		// 	editor: {
		// 		type: 'checkbox',
		// 		options: {
		// 			checkedValue: true,
		// 			uncheckedValue: false,
		// 		},
		// 	},
		// 	customHeader: headerEl,
		// });

		const authColumn = (header, name, headerEl, width) => ({
			header,
			name,
			width,
			align: 'center',
			renderer: { type: AuthCheckboxRenderer }, // 이게 핵심
			customHeader: headerEl, // 헤더 체크박스(텍스트+체크박스 DOM)
			sortable: false, // 권한컬럼은 보통 정렬 불필요(원하면 제거)
		});

		// 헤더 엘리먼트 준비
		$modal.headerWorker = $modal.createHeaderCheckbox('실무자', 'isWorker');
		$modal.headerTechSub = $modal.createHeaderCheckbox('기술책임자(부)', 'isTechSub');
		$modal.headerTechMain = $modal.createHeaderCheckbox('기술책임자(정)', 'isTechMain');

		// Grid 생성
		$modal.itemAuthGrid = new Grid({
			el: document.querySelector('.itemAuthGrid'),
			columns: [
				{
					header: '중분류코드',
					name: 'middleItemCode',
					width: 120,
					align: 'center',
				},
				{
					header: '중분류명',
					name: 'middleItemCodeName',
					align: 'left',
				},
				// checkboxColumn('실무자', 'isWorker', $modal.headerWorker, 120),
				// checkboxColumn('기술책임자(부)', 'isTechSub', $modal.headerTechSub, 140),
				// checkboxColumn('기술책임자(정)', 'isTechMain', $modal.headerTechMain, 140),
				authColumn('실무자', 'isWorker', $modal.headerWorker, 120),
				authColumn('기술책임자(부)', 'isTechSub', $modal.headerTechSub, 140),
				authColumn('기술책임자(정)', 'isTechMain', $modal.headerTechMain, 140),
				{
					header: 'authBitmask',
					name: 'authBitmask',
					hidden: true,
				},
				{
					header: 'middleItemCodeId',
					name: 'middleItemCodeId',
					hidden: true,
				},
			],
			rowHeaders: [],
			bodyHeight: 420,
			minBodyHeight: 420,
			// 43개는 클라이언트에서 그냥 다 들고 가는 게 최적
			pageOptions: {
				useClient: true,
				perPage: 100,
			},
			data: [],
		});

		// 헤더 체크박스 바인딩
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isWorker', $modal.headerWorker);
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isTechSub', $modal.headerTechSub);
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isTechMain', $modal.headerTechMain);

		// 셀 변경 시 authBitmask 동기화 + 헤더 상태 동기화
		$modal.itemAuthGrid.on('afterChange', (ev) => {
			const changes = ev?.changes;
			if (!Array.isArray(changes) || changes.length === 0) return;

			const authCols = new Set(['isWorker', 'isTechSub', 'isTechMain']);
			const touchedRowKeys = new Set();

			changes.forEach((c) => {
				if (authCols.has(c.columnName)) touchedRowKeys.add(c.rowKey);
			});

			// 권한 컬럼이 바뀐 행만 bitmask 갱신
			touchedRowKeys.forEach((rowKey) => {
				const row = $modal.itemAuthGrid.getRow(rowKey);
				const newMask = $modal.buildAuthMask(row);
				$modal.itemAuthGrid.setValue(rowKey, 'authBitmask', newMask);
			});

			// const changes = ev?.changes ?? [];
			// if (changes.length === 0) return;

			// const touchedCols = new Set();
			// const touchedRowKeys = new Set();

			// changes.forEach((c) => {
			// 	touchedCols.add(c.columnName);
			// 	touchedRowKeys.add(c.rowKey);
			// });

			// // 권한 컬럼이 바뀐 row만 bitmask 업데이트
			// const authCols = new Set(['isWorker', 'isTechSub', 'isTechMain']);
			// let needMaskUpdate = false;
			// changes.forEach((c) => {
			// 	if (authCols.has(c.columnName)) needMaskUpdate = true;
			// });

			// if (needMaskUpdate) {
			// 	touchedRowKeys.forEach((rowKey) => {
			// 		const row = $modal.itemAuthGrid.getRow(rowKey);
			// 		const newMask = $modal.buildAuthMask(row);
			// 		$modal.itemAuthGrid.setValue(rowKey, 'authBitmask', newMask);
			// 	});
			// }

			// // 헤더 체크박스(전체/부분) 상태 동기화
			// if (touchedCols.has('isWorker')) $modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isWorker', $modal.headerWorker);
			// if (touchedCols.has('isTechSub')) $modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechSub', $modal.headerTechSub);
			// if (touchedCols.has('isTechMain')) $modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechMain', $modal.headerTechMain);
		});

		// 전체 헤더 상태 한 번에 동기화
		$modal.syncAllAuthHeaders = () => {
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isWorker', $modal.headerWorker);
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechSub', $modal.headerTechSub);
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechMain', $modal.headerTechMain);
		};

		// 테스트용 더미데이터 호출
		setTimeout(() => {
			// $modal.loadDummyItemAuth();
		}, 1000);

		$modal.loadDummyItemAuth = () => {
			// authBitmask 더미 패턴 (0, 1, 2, 4, 1|2, 1|4, 2|4, 1|2|4)
			const masks = [0, 1, 2, 4, 3, 5, 6, 7];

			// 중분류 43개 더미 생성
			const rows = Array.from({ length: 43 }, (_, idx) => {
				const i = idx + 1;

				// 코드 예시: 101~143 형태 (원하면 001~043 같은 문자열로 바꿔도 됨)
				const middleCode = String(100 + i);

				// 권한 마스크는 규칙적으로 섞이도록
				const authBitmask = masks[idx % masks.length];

				return {
					middleItemCodeId: i, // PK 대용 (더미)
					middleItemCode: middleCode, // 화면 표시 코드
					middleItemCodeName: `중분류명 ${i}`, // 화면 표시명
					authBitmask: authBitmask, // 핵심: bitmask
				};
			});

			// bitmask -> boolean 컬럼 세팅 (체크박스 표시용)
			rows.forEach((r) => $modal.applyAuthMaskToRow(r));

			// 그리드 데이터 주입
			$modal.itemAuthGrid.resetData(rows);

			// 헤더 체크박스(전체/부분) 상태 반영
			$modal.syncAllAuthHeaders();
		};
	}; // End init_modal

	// 부서관리, 직급관리 정보를 가져와서 세팅한다.
	$modal.setBasicOptions = async () => {
		const resGetOptions = await g_ajax(
			'/api/basic/getBasicOptions',
			{},
			{
				type: 'GET',
			}
		);

		if (resGetOptions?.code > 0) {
			const resData = resGetOptions.data;
			// 부서관리 옵션을 세팅한다.
			if (resData.departmentData != undefined && resData.departmentData.length > 0) {
				const $departmentSelect = $('.departmentSelect', $modal);
				$.each(resData.departmentData, function (index, data) {
					const option = new Option(data.name, data.id);
					$departmentSelect.append(option);
				});
			}
			// 직급관리 옵션을 세팅한다.
			if (resGetOptions.data != undefined && resGetOptions.data.memberLevelData != undefined) {
				const $memberLevelSelect = $('.memberLevelSelect', $modal);
				$.each(resData.memberLevelData, function (index, data) {
					const option = new Option(data.name, data.id);
					$memberLevelSelect.append(option);
				});
			}
		}
	};

	// 페이지 내 이벤트
	$modal;

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인경우
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
		// 모달이 아닌 일반 페이지인 경우엔 아래 init_page가 동작한다.
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
