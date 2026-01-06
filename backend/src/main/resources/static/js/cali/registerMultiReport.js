$(function () {
	console.log('++ cali/registerMultiReport.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	// const $bodyCandidate = $candidates.filter('.modal-body');
	// if ($bodyCandidate.length) {
	// 	$modal = $bodyCandidate.first();
	// } else {
	// 	// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
	$modal = $candidates.first();
	// }
	let $modal_root = $modal.closest('.modal');

	let caliOrderId = null; // 접수id
	let middleItemCodeSetAry = [];
	let smallItemCodeSetObj = {};

	// 임의 데이터1
	const ORDER_TYPE_ITEMS = [
		{ text: '공인', value: 'ACCREDDIT' },
		{ text: '비공인', value: 'UNACCREDDIT' },
		{ text: '시험', value: 'TESTING' },
	];
	const CALI_CYCLE_TYPES = [
		{ text: '자동(기본 12개월)', value: '0' },
		{ text: '3개월', value: '3' },
		{ text: '6개월', value: '6' },
		{ text: '12개월', value: '12' },
		{ text: '18개월', value: '18' },
		{ text: '24개월', value: '24' },
		{ text: '36개월', value: '36' },
	];
	// TODO 1)	품목코드 관리 테이블 및 메뉴 생성 이후에 하드코딩이아닌 db에서 가져오도록 변경할 것
	// TODO 2) 토스트 그리드에는 relation을 이용하여 동적으로 데이터를 변경이 가능하므로, 다음엔 relation 활용
	// https://nhn.github.io/tui.grid/latest/tutorial-example05-relation-columns 페이지 참조
	// 중분류/소분류 코드에 대한 부분도 우선 하드코딩으로 넣어준다.

	$modal.init_modal = async (param) => {
		$modal.param = param;
		caliOrderId = $modal.param.caliOrderId;
		middleItemCodeSetAry = $modal.param.middleItemCodeSetAry; // 중분류 데이터
		smallItemCodeSetObj = $modal.param.smallItemCodeSetObj; // 소분류 데이터

		const middleListItems = await $modal.buildMiddleListItems(middleItemCodeSetAry); // 중분류 데이터 가공
		const smallMapListItems = await $modal.buildSmallMapListItems(smallItemCodeSetObj); // 소분류 데이터 가공

		console.log('확인');
		console.log('🚀 ~ middleItemCodeSetAry:', middleListItems);
		console.log('🚀 ~ smallItemCodeSetObj:', smallMapListItems);

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.addReportList'),
			columns: [
				// 추후에 부모/자식 구분해서 표시하는 행과 자식행을 추가하는 버튼 구분할 것
				{
					header: '대표/하위',
					name: 'hierarchyType',
					width: 100,
					align: 'center',
					escapeHTML: false,
					formatter: ({ value }) => {
						if (value === 'parent') {
							return `<span class="badge badge-primary" style="font-size: 100%;"></i>대표</span>`;
						}
						return `<span class="badge badge-secondary" style="font-size: 100%;"></i>하위</span>`;
					},
				},
				{
					header: '하위추가',
					width: 90,
					name: 'addChild',
					align: 'center',
				},
				{
					// select 박스로 진행
					header: '접수구분',
					name: 'orderType',
					width: 80,
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
					width: 90,
					align: 'center',
					editor: {
						type: 'select',
						options: { listItems: middleListItems },
					},
					relations: [
						{
							targetNames: ['smallItemCodeId'],
							listItems({ value }) {
								// value === middleId
								return smallMapListItems[String(value)] || [{ text: '선택', value: '' }];
							},
							disabled({ value }) {
								// 중분류가 없으면 소분류 비활성화
								return !value;
							},
						},
					],
					formatter: 'listItemText',
				},
				{
					header: '소분류',
					name: 'smallItemCodeId',
					className: 'cursor_pointer',
					width: 90,
					align: 'center',
					formatter: 'listItemText',
					editor: {
						type: 'select',
						options: { listItems: [] }, // relations가 채워줌
					},
				},
				{
					header: '기기명',
					name: 'itemName',
					className: 'cursor_pointer',
					editor: 'text',
					// width: '200',
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
					header: '교정수수료',
					name: 'caliFee',
					className: 'cursor_pointer',
					editor: number_format_editor,
					width: '80',
					align: 'right',
					formatter: ({ value }) => {
						return number_format(value);
					},
				},
				{
					header: '교정주기',
					name: 'itemCaliCycle',
					className: 'cursor_pointer',
					editor: {
						type: 'select',
						options: { listItems: CALI_CYCLE_TYPES },
					},
					formatter: 'listItemText',
					width: 110,
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
				// 아래는 화면에 표시되진 않지만 넘기는 값들
				{
					name: 'itemId',
					hidden: true,
				},
			],
			editingEvent: 'click', // 원클릭으로 수정할 수 있도록 변경
			rowHeaders: ['checkbox'],
			minBodyHeight: 650,
			useClientSort: false,
			bodyHeight: 650,
			// draggable: true,
			treeColumnOptions: {
				name: 'addChild', // 해당 열 클릭 시 트리구조가 생성된다.
				useIcon: true,
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
				// 중분류코드 변경 시, 소분류코드 초기화
				if (columnName === 'middleItemCodeId') {
					$modal.grid.setValue(rowKey, 'smallItemCodeId', '');
				}
			});
		});

		// 빈 row 객체 반환. (기본 접수타입은 '공인')
		$modal.grid.makeEmptyRow = (orderType = 'ACCREDDIT', hierarchyType = 'parent') => {
			return {
				hierarchyType, // parent: 부모, child: 자식
				orderType,
				middleItemCodeId: null, // id관련 컬럼은 기본적으로 NULL을 준다.
				smallItemCodeId: null,
				itemId: null,
				itemName: '',
				itemMakeAgent: '',
				itemFormat: '',
				itemNum: '',
				itemCaliCycle: 0, // TODO 교정주기 품목테이블에서 교정주기가 없거나, 시간단위, 또는 '수시'인 경우 고민 필요)
				remark: '',
			};
		};

		// 자식 row 추가 ('하위' 버튼 클릭 시)
		$modal.grid.addChildRow = (parentRowKey) => {
			const depth = $modal.grid.getDepth(parentRowKey); // 클릭이 발생한 row의 깊이 (부모는 1임)
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
		};

		// 모달 내 그리드에 대한 이벤트
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			if (row) {
				// 자식 행 추가
				if (e.columnName == 'addChild') {
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
		$modal
			// 행추가
			.on('click', '.insertRows', function () {
				// 단순 행 추가
				$modal.grid.addGridRow();
			})
			// 행삭제
			.on('click', '.deleteRows', function () {
				$modal.grid.blur();

				// rowKey는 고유키이므로, index와는 다른 개념이다. reset을 하기 전에는 그리드 내  rowKey는 독립적이다.

				const checkedRowKeys = $modal.grid.getCheckedRowKeys();
				if (checkedRowKeys.length === 0) {
					g_toast('삭제할 행을 선택해주세요.', 'warning');
					return false;
				}

				// 깊이를 기준으로 내림차순 (자식부터 정렬되도록)
				let descendRowKeys = checkedRowKeys.sort((a, b) => {
					// 자식 행에 속하는 행부터 먼저 정렬된다.
					$modal.grid.getDepth(b) - $modal.grid.getDepth(a);
				});

				for (const key of descendRowKeys) {
					if ($modal.grid.getRow(key)) {
						$modal.grid.removeRow(key);
					}
				}

				// 삭제처리 후, 행이 존재하지 않으면 기본 1개 초기화
				if ($modal.grid.getRowCount() === 0) {
					$modal.grid.appendRow($modal.grid.makeEmptyRow());
				}
			});

		$modal_root
			// 그리드가 아닌 영역 클릭 시, 그리드에 대한 blur() 처리를 해준다.
			.on('click', '.modal-dialog', function (e) {
				if ($(e.target).closest('.addReportList').length === 0 && !$(e.target).hasClass('insertRows')) {
					$modal.grid.blur();
				}
			})
			// TODO 엑셀 다중 업로드
			.on('click', '.addReportExcel', async function (e) {
				console.log('엑셀 다중 등록 진행');
			});
	}; // End of init_modal

	// 저장
	$modal.confirm_modal = async function (e) {
		$modal.grid.blur();
		const rows = $modal.grid.getData();
		console.log('🚀 ~ rows:', rows);

		// 저장버튼 비활성화
		const $saveBtn = $('button.btn_save', $modal_root);
		$saveBtn.prop('disabled', true);
		let isValid = true; // 유효성 검증
		let notSearchItemList = []; // 품목조회를 하지 않은 경우 별도로 넣기

		const sendData = [];
		try {
			// 순회하면서 값 확인 => 일단 전부 보낼 것 dto에 hierarchyType 필드를 통해서 부모 id 찾기
			let currentIndex = -1;
			$.each(rows, function (index, item) {
				const itemName = item.itemName;
				const caliFee = item.caliFee;
				if (!check_input(itemName.trim())) {
					g_toast('품목명은 필수입니다', 'warning');
					$modal.grid.focus(item.rowKey, 'itemName'); // 해당 cell 포커스
					isValid = false;
					return false;
				}

				if (!caliFee || caliFee == '') {
					item.caliFee = 0;
				}

				// 품목을 조회하지 않았던 row에 대해선 alert를 위해 별도 구분
				if (!item.itemId || Number(item.itemId) == 0) {
					notSearchItemList.push(item);
				}

				// 문서타입 구분
				if (item.orderType == 'ACCREDDIT' || item.orderType == 'TESTING') {
					item.docType = 'ISO';
				} else {
					item.docType = 'B';
				}

				item.reportType = 'SELF'; // 성적서타입(self)

				//
				if (item.hierarchyType === 'parent') {
					item.child = [];
					sendData.push(item);
					currentIndex = sendData.length - 1;
				} else {
					sendData[currentIndex].child.push(item);
				}
			});
		} catch (err) {
			g_toast('오류가 있습니다.', 'error');
			isValid = false;
			console.log(err);
		} finally {
			$saveBtn.prop('disabled', false);
		}

		// 유효성 검증 못한 경우 return
		if (!isValid) {
			$saveBtn.prop('disabled', false);
			return false;
		}

		let confirmMsg = `저장하시겠습니까?`;
		if (notSearchItemList.length > 0) {
			confirmMsg += '<br>품목을 조회하지 않은 아래 데이터의 경우, 품목에 자동으로 등록됩니다.<br><br>';
			confirmMsg += `<div class='text-left'>`;
			// 최대 10건까지만 보여주고, 넘어갈 경우 ...로 표시
			$.each(notSearchItemList, function (idx, item) {
				if (idx === 10) {
					return false;
				} else {
					const itemName = item.itemName ?? '';
					const itemFormat = item.itemFormat ?? '';
					const itemMakeAgent = item.itemMakeAgent ?? '';
					confirmMsg += `${idx + 1}. 품목명: '${itemName}', 형식: '${itemFormat}', 제작회사: '${itemMakeAgent}' <br>`;
				}
			});
			if (notSearchItemList.length > 10) {
				confirmMsg += `.....<br>그외 ${notSearchItemList.length - 10}건`;
			}
			confirmMsg += `</div>`;
		}

		// g_ajax로 처리하기
		const confirmSave = await g_message('성적서 등록', confirmMsg, 'info', 'confirm');
		if (confirmSave.isConfirmed == true) {
			g_loading_message();

			try {
				const resSave = await g_ajax(`/api/report/addReport?caliOrderId=${caliOrderId}`, JSON.stringify(sendData), {
					contentType: 'application/json; charset=utf-8',
				});
				console.log('🚀 ~ resSave:', resSave);
				if (resSave?.code > 0) {
					await g_message('성적서 등록', '', 'success');
					$modal_root.modal('hide');
					return true;
				} else {
					await g_message('성적서 저장 실패', '', 'warning');
				}
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
				$saveBtn.prop('disabled', false);
			}
		} else {
			$saveBtn.prop('disabled', false);
			return false;
		}
	};

	$modal.buildMiddleListItems = (middleItemCodeSetAry) => {
		return [{ text: '선택', value: '' }, ...middleItemCodeSetAry.map((x) => ({ text: x.codeNum, value: String(x.id) }))];
	};

	$modal.buildSmallMapListItems = (smallItemCodeSetObj) => {
		const map = {};
		for (const [middleId, list] of Object.entries(smallItemCodeSetObj || {})) {
			map[String(middleId)] = [{ text: '선택', value: '' }, ...(list || []).map((x) => ({ text: x.codeNum, value: String(x.id) }))];
		}
		return map;
	};

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
