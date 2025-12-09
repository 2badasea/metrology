$(function () {
	console.log('++ cali/caliOrder.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		// 이번 memberJoin 모달의 body
		$modal = $bodyCandidate.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);
	};

	// 교정접수 리스트 가져오기
	// $modal.data_source = {
	// 	api: {
	// 		readData: {
	// 			url: '/api/basic/getAgentList',
	// 			// 'serializer'는 토스트 그리드에서 제공
	// 			serializer: (grid_param) => {
	// 				grid_param.isClose = $('form.searchForm .isClose', $modal).val();
	// 				grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? '';
	// 				grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? '';
	// 				return $.param(grid_param);
	// 			},
	// 			method: 'GET',
	// 		},
	// 	},
	// };

	// 그리드 정의
	$modal.grid = new Grid({
		el: document.querySelector('.orderList'),
		columns: [
			{
				header: '접수일',
				name: 'groupName',
				className: 'cursor_pointer',
				width: '100',
				align: 'center',
			},
			{
				header: '신청업체',
				name: 'name',
				className: 'cursor_pointer',
				align: 'center',
				sortable: true,
			},
			{
				header: '신청업체 주소',
				name: 'addr',
				className: 'cursor_pointer',
				width: '300',
				align: 'center',
				sortable: true,
			},
			{
				header: '성적서발행처',
				name: 'agentNum',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '성적서발행처 주소',
				name: 'ceo',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
			},

			{
				header: '접수내역',
				className: 'cursor_pointer',
				align: 'center',
                formatter: function (data) {
                    return '<button type="button" class="btn btn-info">접수내역</button>';
                }
			}
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 20,
		},
		rowHeaders: ['checkbox'],
		minBodyHeight: 663,
		bodyHeight: 663,
		// data: $modal.data_source,
	});

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1);
		})
		// 등록
		.on('click', '.addOrder', async function (e) {
			e.preventDefault();

			try {
				const resModal = await g_modal(
					'/cali/caliOrderModify',
					{},
					{
						title: '교정접수 등록',
						size: 'xxxl',
						show_close_button: true,
						show_confirm_button: true,
						confirm_button_text: '저장',
					}
				);

				// 모달이 성공적으로 종료되었을 때만 그리드 갱신
				if (resModal) {
					$modal.grid.reloadData();
				}
			} catch (err) {
				console.error('g_modal 실행 중 에러', err);
			}

		})
		// 삭제
		.on('click', '.deleteOrder', async function (e) {
			e.preventDefault();

			// 1. 그리드 내 체크된 업체 확인
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 접수를 선택해주세요.', 'warning');
				return false;
			} else {
				// 각 접수의 id를 담는다.
				let delOrderIds = $.map(checkedRows, function (row, index) {
					return row.id;
				});

				// 2. 삭제유무 confirm 확인
				if (confirm('정말 삭제하시겠습니까?\n')) {
					g_loading_message('삭제 처리 중입니다...');

					try {
						const sendData = {
							ids: delOrderIds,
						};

						const resDelete = await g_ajax(
							'/api/basic/deleteOrder',
							JSON.stringify(sendData),

							{
								contentType: 'application/json; charset=utf-8',
							}
						);
						if (resDelete?.code === 1) {
							const delNames = resDelete.data || [];
							Swal.fire({
								icon: 'success',
								title: '삭제 완료'
							});
							// 그리드 갱신
							$modal.grid.reloadData();
						}
					} catch (err) {
						custom_ajax_handler(err);
					} finally {
					}
				} else {
					return false;
				}
			}

			return false;
		})
		;

	// 그리드 이벤트 정의
	$modal.grid.on('click', async function (e) {
		const row = $modal.grid.getRow(e.rowKey);

		if (row && e.columnName != '_checked') {
			// 업체수정 모달 띄우기
			try {
				const resModal = await g_modal(
					'/cali/caliOrderModify',
					{
						id: row.id,
					},
					{
						size: 'xxxl',
						title: '교정접수 수정',
						show_close_button: true,
						show_confirm_button: true,
						confirm_button_text: '저장',
					}
				);

				// 모달이 성공적으로 종료되었을 때만 그리드 갱신
				if (resModal) {
					$modal.grid.reloadData();
				}				

			} catch (err) {
				console.error('g_modal 실행 중 에러', err);
			}
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
