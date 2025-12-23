$(function () {
	console.log('++ cali/registerMultiReport.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	let caliOrderId = null; // 접수id

	// 임의 데이터1
	const ORDER_TYPE_ITEMS = [
		{ text: '공인', value: 'ACCREDDIT' },
		{ text: '비공인', value: 'UNACCREDDIT' },
		{ text: '시험', value: 'TESTING' },
	];
	// TODO 품목코드 관리 테이블 및 메뉴 생성 이후에 하드코딩이아닌 db에서 가져오도록 변경할 것
	// NOTE 토스트 그리드에는 relation을 이용하여 동적으로 데이터를 변경이 가능하므로, 다음엔 relation 활용
	// 중분류/소분류 코드에 대한 부분도 우선 하드코딩으로 넣어준다.
	const tmpMiddleCode = [
		{ text: '101', value: '10' },
		{ text: '102', value: '11' },
	];
	// 임시 소분류
	const tmpSmallCode = [
		{ text: '10101', value: '12', middleCodeId: '10' },
		{ text: '10102', value: '13', middleCodeId: '10' },
		{ text: '10103', value: '14', middleCodeId: '10' },
		{ text: '10104', value: '15', middleCodeId: '10' },
		{ text: '10105', value: '16', middleCodeId: '10' },

		{ text: '10201', value: '17', middleCodeId: '11' },
		{ text: '10202', value: '18', middleCodeId: '11' },
		{ text: '10203', value: '19', middleCodeId: '11' },
		{ text: '10204', value: '20', middleCodeId: '11' },
		{ text: '10205', value: '21', middleCodeId: '11' },
	];

	$modal.init_modal = async (param) => {
		$modal.param = param;
		caliOrderId = $modal.param.caliOrderId;

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.addReportList'),
			columns: [
				// 추후에 부모/자식 구분해서 표시하는 행과 자식행을 추가하는 버튼 구분할 것
				{
					header: '하위구분',
					name: 'hierarchyType',
					width: 100,
					align: 'center',
					escapeHTML: false,
					formatter: ({ value }) => {
						if (value === 'parent') {
							return `<span class="badge badge-primary" style="font-size: 100%;"></i>부모</span>`;
						}
						return `<span class="badge badge-secondary" style="font-size: 100%;"></i>자식</span>`;
					},
				},
				{
					// select 박스로 진행
					header: '접수구분',
					name: 'orderType',
					width: 90,
					align: 'center',
					className: 'cursor_pointer',
					editor: { type: 'select', options: { listItems: ORDER_TYPE_ITEMS } },
					formatter: 'listItemText',
					align: 'center',
				},
				// {
				// 	header: '하위',
				// 	name: 'grid_btn_add_hierarchy',
				// 	width: 40,
				// 	align: 'center',
				// 	className: 'cursor_pointer',
				// 	// 자식행(Depth=2)은 버튼 숨김
				// 	formatter: ({ rowKey }) => {
				// 		const depth = $modal.grid.getDepth(rowKey); // 1=부모, 2=자식
				// 		if (depth >= 2) return '';
				// 		return `<button type="button" class="btn btn-secondary w-100 h-100 rounded-0 js-add-child"><i class="bi bi-plus"></i></button>`;
				// 	},
				// },
				{
					header: '중분류',
					name: 'middleItemCodeId',
					className: 'cursor_pointer',
					width: 120,
					align: 'center',
					editor: {
						type: middle_code_selectbox_renderer,
						options: { listItems: tmpMiddleCode },
					},
					formatter: 'listItemText',
				},
				{
					header: '소분류',
					name: 'smallItemCodeId',
					className: 'cursor_pointer',
					width: 120,
					align: 'center',
					editor: {
						type: small_code_selectbox_renderer,
						options: { listItems: tmpSmallCode }, // 기본은 비워둠
					},
					formatter: function (data) {
						return $modal.fmtSmallCodeNum(data);
					},
				},
				{
					name: 'itemId',
					hidden: true,
				},
				{
					header: '기기명',
					name: 'itemName',
					className: 'cursor_pointer',
                    editor: 'text',
					width: '200',
					align: 'center',
				},
				{
					header: '제작회사',
					name: 'itemMakeAgent',
					className: 'cursor_pointer',
                    editor: 'text',
					width: '200',
					align: 'center',
				},
				{
					header: '형식',
					name: 'itemFormat',
					className: 'cursor_pointer',
                    editor: 'text',
					width: '200',
					align: 'center',
				},
				{
					header: '기기번호',
					name: 'itemNum',
					className: 'cursor_pointer',
                    editor: 'text',
					width: '200',
					align: 'center',
				},
				{
					header: '교정주기',
					name: 'caliCycle',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '비고',
					name: 'remark',
					className: 'cursor_pointer',
                    editor: 'text',
					width: '200',
					align: 'center',
				},
			],
			rowHeight: 'auto',
			editingEvent: 'click', // 원클릭으로 수정할 수 있도록 변경
			rowHeaders: ['checkbox'],
			minBodyHeight: 650,
			useClientSort: false,
			bodyHeight: 650,
			// draggable: true,
			treeColumnOptions: {
				name: 'hierarchyType',
				useIcon: false,
				useCascadingCheckbox: true, // true: 부모 체크 시, 자식도 같이 체크됨(연쇄), false: 부모/자식 서로 독립적
			},
		});

		// 그리드 초기화 후 기본행 1개 표시
		$modal.grid.on('onGridMounted', function () {
			const rowCnt = $modal.grid.getRowCount();
			if (rowCnt === 0) {
				$modal.grid.appendRow($modal.grid.makeEmptyRow('ACCREDDIT', 'parent'), { focus: true });
			}
		});

		$modal.grid.on('afterChange', (ev) => {
			ev.changes.forEach(({ rowKey, columnName }) => {
				if (columnName === 'middleItemCodeId') {
					$modal.grid.setValue(rowKey, 'smallItemCodeId', '');
				}
			});
		});

		// drag and drop에 대한 이벤트
		// let dragCtx = null;

		// $modal.grid.on('dragStart', (ev) => {
		// 	const rowKey = ev.rowKey;
		// 	const parent = $modal.grid.getParentRow(rowKey);
		// 	dragCtx = {
		// 		rowKey,
		// 		fromParentKey: parent ? parent.rowKey : null,
		// 		// fromIndex 등도 같이 저장해두면 “원복”이 쉬움
		// 	};
		// });

		// $modal.grid.on('drop', (ev) => {
		// 	if (!dragCtx) return;

		// 	// 트리에서 “자식으로 붙이기” 시도를 의미하는 힌트
		// 	if (ev.appended) {
		// 		alert('부모/자식 레벨은 변경할 수 없습니다.');
		// 		// 여기서 원복 로직 수행(또는 드랍 무효 처리 가능한 방식이면 무효 처리)
		// 		return;
		// 	}

		// 	// 드랍 후 parent가 바뀌었는지 확인(같은 레벨 제약)
		// 	const parent = $modal.grid.getParentRow(dragCtx.rowKey);
		// 	const toParentKey = parent ? parent.rowKey : null;

		// 	if (toParentKey !== dragCtx.fromParentKey) {
		// 		alert('같은 레벨 내에서만 이동할 수 있습니다.');
		// 		// 원복 로직 수행
		// 	}
		// });

		// 빈 row 데이터를 생성한다.
		$modal.grid.makeEmptyRow = (orderType = 'ACCREDDIT', hierarchyType = 'parent') => {
			return {
				hierarchyType, // parent: 부모, child: 자식
				orderType,
				item_id: null,
				middleItemCodeId: null,
				smallItemCodeId: null,
				itemId: null,
				itemName: '',
				itemMakeAgent: '',
				itemFormat: '',
				itemNum: '',
				caliCycle: '',
				remark: '',
			};
		};

		// 자식 row 추가 ('하위' 버튼 클릭 시)
		$modal.grid.addChildRow = (parentRowKey) => {
			const depth = $modal.grid.getDepth(parentRowKey);
			if (depth >= 2) {
				g_toast('하위 성적서는 그 하위 성적서를<br>가질 수 없습니다.', 'warning');
				return false;
			}

			// 부모 row 바로 아래 생성되도록 한다.
			const parentRow = $modal.grid.getRow(parentRowKey);
			$modal.grid.appendTreeRow($modal.grid.makeEmptyRow(parentRow.orderType, 'child'), { parentRowKey, offset: 0, focus: true });

			$modal.grid.expand(parentRowKey);
		};

		// 그리드 행 추가 이벤트
		$modal.grid.addGridRow = (mode = '') => {
			const focusedCell = $modal.grid.getFocusedCell();
			const hasFocus = focusedCell && focusedCell.rowKey != null; // true or fasle

			// 포커스가 없는 경우, 마지막 행에 추가
			if (!hasFocus) {
				$modal.grid.appendRow($modal.grid.makeEmptyRow(), { focus: true });
				return false;
			}

			// 포커스가 존재하는 경우
			const rowKey = focusedCell.rowKey;
			const depth = $modal.grid.getDepth(rowKey);

			// 자식에 포커스면 차단
			if (depth >= 2) {
				g_toast('하위 성적서는 그 하위 성적서를<br>가질 수 없습니다.', 'warning');
				return false;
			}

			// 부모에 자식이 있으면, 부모의 마지막 자식 다음에 형제행을 넣는 게 일반적
			const childs = $modal.grid.getDescendantRows(rowKey) || [];
			const baseKey = childs.length > 0 ? childs[childs.length - 1].rowKey : rowKey;

			const at = $modal.grid.getIndexOfRow(baseKey) + 1;

			// 형제 행 추가
			const referRow = $modal.grid.getRow(rowKey);
			$modal.grid.appendRow($modal.grid.makeEmptyRow(referRow.orderType), { at, focus: true });

			// let option = {};
			// // 포커스가 존재할 경우, 포커스된 행 바로 아래 추가
			// if (focusedCell.rowKey != null) {
			// 	let rowIndex = $modal.grid.getIndexOfRow(focusedCell.rowKey);
			// 	if (mode == 'add' || mode == 'hierarchy') {
			// 		rowIndex = parseInt(rowIndex) + 1;
			// 	}
			// 	option.at = rowIndex;
			// }
			// $modal.grid.appendRow({ 'orderType': 'ACCREDDIT' }, option);
		};

		// 모달 내 그리드에 대한 이벤트
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			if (row) {
				// 자식 행 추가
				if (e.columnName == 'hierarchyType') {
					$modal.grid.addChildRow(e.rowKey);
				}
				// 그외 클릭 시
				else {
				}
			}
			// 그리드가 아닌 영역 클릭 시, blur처리 -> 포커스 없애기
			else {
				$modal.grid.blur();
			}
		});

		// 페이지 내 이벤트
		$modal.on('click', '.insertRows', function () {
			// 단순 행 추가
			$modal.grid.addGridRow();
		});

		// 엑세 다중 등록
		$modal_root
			// 엑셀 다중 업로드
			.on('click', '.addReportExcel', async function (e) {
				console.log('엑셀 다중 등록 진행');
			});
	}; // End of init_modal

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('등록진행');
	};

	// 소분류코드 반환
	$modal.fmtSmallCodeNum = (data) => {
		const middleId = data.row.middleItemCodeId;
		const found = tmpSmallCode.find((obj) => String(obj.middleCodeId) === String(middleId) && String(obj.value) === String(data.value)); // boolean
		return found ? found.text : '선택';
	};

	// 담당자 그리드 초기화

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인 경우 바로 init_modal() 호출
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
			init_page($modal);
		}
	}
});
