$(function () {
	console.log('++ basic/itemCode.js');

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

	$modal.init_modal = (param) => {
		$modal.param = param;

		g_ajax(
			'/api/basic/getItemCodeSet',
			{
				codeLevel: 'LARGE',
			},
			{
				type: 'GET',
				success: function (res) {
					if (res?.code > 0) {
						$modal.setLargeItemCodeSet(res.data);
					}
				},
			}
		);

		// 중분류 리스트 가져오기
		$modal.middleDataSource = {
			initialRequest: false, // 최초 렌더 시 자동 조회 방지 (기본 true)
			api: {
				readData: {
					url: '/api/basic/getItemCodeList',
					serializer: (grid_param) => {
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.middleGrid = new Grid({
			el: document.querySelector('.middleGrid'),
			columns: [
				{
					header: '품목코드',
					name: 'codeNum',
					className: 'cursor_pointer',
					align: 'center',
					width: '80',
				},
				{
					header: '품목코드명',
					name: 'codeName',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '품목코드명(영문)',
					name: 'codeNameEn',
					className: 'cursor_pointer',
					align: 'center',
				},
			],
			pageOptions: {
				perPage: 10,
			},
			minBodyHeight: 417,
			bodyHeight: 417,
			rowHeaders: ['checkbox'],
			data: $modal.middleDataSource,
			rowHeight: 'auto',
		});

		$modal.middleSelectedRowKey = null;
		$modal.middleGrid.on('focusChange', function (ev) {
			if ($modal.middleSelectedRowKey != null && $modal.middleSelectedRowKey >= 0) {
				$modal.middleGrid.removeRowClassName($modal.middleSelectedRowKey, 'gridFocused');
			}
			$modal.middleSelectedRowKey = ev.rowKey;
			$modal.middleGrid.addRowClassName($modal.middleSelectedRowKey, 'gridFocused');

			// 포커스된 데이터의 경우, 상단에 정보를 세팅시키도록 한다.
			const data = $modal.middleGrid.getRow(ev.rowKey);
			$('.middleCodeBody', $modal).find('input[name]').setupValues(data);
			$('.middleCodeBody', $modal)
				.find('input[name]')
				.prop('readonly', data.isKolasStandard == 'y');
		});

		// 그리드 정의
		$modal.smallGrid = new Grid({
			el: document.querySelector('.smallGrid'),
			columns: [
				{
					header: '품목코드',
					name: 'codeNum',
					className: 'cursor_pointer',
					align: 'center',
					width: '80',
				},
				{
					header: '품목코드명',
					name: 'codeName',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '품목코드명(영문)',
					name: 'codeNameEn',
					className: 'cursor_pointer',
					align: 'center',
				},
			],
			pageOptions: {
				perPage: 5,
			},
			// data: $modal.smallDataSource,
			rowHeaders: ['checkbox'],
			rowHeight: 'auto',
		});

		// 대분류 세팅
		$modal.setLargeItemCodeSet = (data) => {
			const largeSelect = $('.largeCodeSeelct', $modal);
			if (data.length > 0) {
				data.forEach((itemCode) => {
					const option = new Option(`${itemCode.codeNum} (${itemCode.codeName})`, itemCode.id);
					largeSelect.append(option);
				});
			}
		};

		// 그리드 이벤트 정의
		// $modal.grid.on('click', async function (e) {
		// 	const row = $modal.grid.getRow(e.rowKey);

		// 	if (row && e.columnName != '_checked') {
		// 	}
		// });
	}; // End init_modal

	// 페이지 내 이벤트
	$modal
		// 대분류관리 모달 호출
		.on('click', '.manageBig', async function () {
			const resModal = await g_modal(
				'/basic/bigItemCodeModify',
				{},
				{
					size: 'lg',
					title: '대분류코드 관리',
					show_close_button: true,
					show_confirm_button: true,
				}
			);
		})
		// 대분류코드 변경에 따른 중분류 그리드 리로드
		.on('change', '.largeCodeSeelct', function () {
			const value = $(this).val();
			if (!value || value == 0) {
				$modal.middleGrid.resetData([]); // 로컬 초기화
				return false;
			} else {
				const params = {
					parentId: value,
					codeLevel: 'MIDDLE',
				};
				$('.middleCodeBody input', $modal).val('').prop('readonly', false);
				$modal.middleGrid.readData(1, params, true);
			}
		});

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
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
