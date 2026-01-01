$(function () {
	console.log('++ basic/bigItemCodeModify.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	// const $bodyCandidate = $candidates.filter('.modal-body');
	// if ($bodyCandidate.length) {
	// 	$modal = $bodyCandidate.first();
	// } else {
	// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
	$modal = $candidates.first();
	// }
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = async (param) => {
		$modal.param = param;

		$modal.data_source = {
			api: {
				readData: {
					url: '/api/basic/getItemCodeList',
					// 'serializer'는 토스트 그리드에서 제공
					serializer: (grid_param) => {
						grid_param.codeLevel = 'LARGE';
						grid_param.parentId = null;
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.bigGrid'),
			columns: [
				{
					name: 'id',
					hidden: true,
				},
				{
					header: '분류코드',
					name: 'codeNum',
					width: '80',
					editor: 'text',
					editor: {
						type: readOnlyEditorByCondition,
						conditions: {
							isKolasStandard: 'y',
						},
					},
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '분류코드명',
					name: 'codeName',
					editor: {
						type: readOnlyEditorByCondition,
						conditions: {
							isKolasStandard: 'y',
						},
					},
					className: 'cursor_pointer ',
					align: 'center',
				},
				{
					header: '분류코드명(영문)',
					name: 'codeNameEn',
					editor: {
						type: readOnlyEditorByCondition,
						conditions: {
							isKolasStandard: 'y',
						},
					},
					className: 'cursor_pointer',
					align: 'center',
				},
			],
			rowHeaders: ['checkbox'],
			rowHeight: 'auto',
			scrollY: true,
			data: $modal.data_source,
			editingEvent: 'click',
		});

		// 페이지 내 이벤트
		$modal
			// 행 추가
			.on('click', '.addBigCode', function (e) {
				const emptyRow = {
					id: null,
					codeNum: '',
					codeName: '',
					codeNameEn: '',
					isKolasStandard: 'n'
				};
				$modal.grid.appendRow(emptyRow);
			})
			// 행 삭제
			.on('click', '.delBigCode', function (e) {
				const checkedRows = $modal.grid.getCheckedRows();
				if (checkedRows.length === 0) {
					g_toast('삭제할 행을 선택해주세요.<br>KOLAS 표준 분류코드의 경우 수정/삭제가 불가능합니다.', 'warning');
					return false;
				}
			});

		// 그리드 객체에 대한 이벤트 추가
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			if (row) {
				if (row.isKolasStandard === 'y') {
					g_toast('KOLAS 표준 분류코드는 수정/삭제가 불가능합니다.', 'warning');
					return false;
				}
			}
		});

		// 그리드에 데이터가 렌더링(세팅) 직후의 이벤트
		$modal.grid.on('onGridUpdated', function (e) {
			const datas = $modal.grid.getData();
			datas.forEach((row) => {
				$modal.grid.store.column.allColumns.forEach((col) => {
					// kolas 공인 표준의 경우, 체크박스를 통해 선택이 안 되도록 한다.
					if (row.isKolasStandard == 'y' && col.name == '_checked') {
						$modal.grid.disableCell(row.rowKey, col.name);
						$modal.grid.addCellClassName(row.rowKey, col.name, 'read_only');
					}
				});
			});
		});

		// $modal.grid.on('response', function (e) {
		// 	console.log('response 이벤트');
		// });
	}; // End of init_modal

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장클릭');

		// $modal_root.modal('hide');
		// return $modal;
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
