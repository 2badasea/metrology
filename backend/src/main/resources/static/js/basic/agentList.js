$(function () {
	console.log('++ basic/agentList.js');

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

	// 업체관리 리스트 가져오기
	$modal.data_source = {
		api: {
			readData: {
				url: '/api/basic/getAgentList',
				// 'serializer'는 토스트 그리드에서 제공
				serializer: (grid_param) => {
					// grid_param = $.extend(grid_param, $('form.searchForm', $modal).serializeObject());
					// let search_types = $modal
					// 	.find('form.searchForm .searchType')
					// 	.find('option')
					// 	.map(function () {
					// 		if ($(this).val() != 'all') return $(this).val();
					// 	})
					// 	.get();
					grid_param.isClose = $('form.searchForm .isClose', $modal).val();
					grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? '';
					grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? '';
					return $.param(grid_param);
				},
				method: 'GET',
			},
		},
	};

	console.log('확인');

	// 그리드 정의
	$modal.grid = new Grid({
		el: document.querySelector('.agentList'),
		columns: [
			{
				header: '가입방식',
				name: 'createType',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
				formatter: function (data) {
					let html = '';
					if (data.value == 'join') {
						html = '가입';
					} else if (data.value == 'basic') {
						html = '등록';
					} else if (data.value == 'auto') {
						html = '접수';
					}
					return html;
				},
			},
			{
				header: '그룹명',
				name: 'groupName',
				className: 'cursor_pointer',
				width: '100',
				align: 'center',
			},
			{
				header: '업체명',
				name: 'name',
				className: 'cursor_pointer',
				align: 'center',
				sortable: true,
			},
			{
				header: '주소',
				name: 'addr',
				className: 'cursor_pointer',
				width: '300',
				align: 'center',
				sortable: true,
			},
			{
				header: '사업자번호',
				name: 'agentNum',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '대표',
				name: 'ceo',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
			},

			{
				header: '전화번호',
				name: 'agnetTel',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '이메일',
				name: 'email',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '담당자',
				name: 'manager',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
			},
			{
				header: '담당자 연락처',
				name: 'managerTel',
				className: 'cursor_pointer',
				align: 'center',
			},
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 15,
		},
		rowHeaders: ['checkbox'],
		// data: [
		// 	{
		// 		name: 'Beautiful Lies',
		// 		artist: 'Birdy',
		// 		release: '2016.03.26',
		// 		genre: 'Pop',
		// 	},
		// ],
		data: $modal.data_source,
	});

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1);
		})
		// 등록
		.on('click', '.addAgentBtn', async function (e) {
			e.preventDefault();
			await g_modal(
				'/basic/agentModify',
				{},
				{
					title: '업체 등록',
					size: 'xxl',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '저장',
				}
			).then((resModal) => {
				// 모달창이 닫히면 그리드 갱신
				$modal.grid.reloadData();
			});
		})
		// 삭제
		.on('click', '.deleteAgentBtn', async function (e) {
			e.preventDefault();

			// 1. 그리드 내 체크된 업체 확인
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 업체를 선택해주세요.', 'warning');
				return false;
			} else {
				// 각 업체의 id를 담는다.
				let delAgentIds = $.map(checkedRows, function (row, index) {
					return row.id;
				});
				console.log('🚀 ~ delAgentIds:', delAgentIds);

				// 2. 삭제유무 confirm 확인
				if (confirm('정말 삭제하시겠습니까?\n업체정보, 담당자, 로그인 계정이 삭제됩니다')) {
					g_loading_message('삭제 처리 중입니다...');

					try {
						// 서버에 전송할 때, obj 형태로 보냄(DTO로 받음)
						// NOTE contentType이 application/json이 아닌 기본형태라면 DTO가 아닌 @RequestParam으로 받는 것도 가능
						const sendData = {
							ids: delAgentIds,
						};

						const resDelete = await g_ajax(
							'/api/basic/deleteAgent',
							JSON.stringify(sendData),

							{
								contentType: 'application/json; charset=utf-8',
							}
						);
						console.log('🚀 ~ resDelete:', resDelete);
						if (resDelete?.code === 1) {
							const delNames = resDelete.data || [];
							Swal.fire({
								icon: 'success',
								title: '삭제 완료',
								text: `삭제된 업체: ${delNames.join(', ')}`,
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
		// 그룹관리 모달 호출
		.on('click', '.groupManageBtn', async function (e) {
			e.preventDefault();

			// 선택된 업체 존재하는지 확인
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('관리할 업체를 선택해주세요.', 'warning');
				return false;
			} else {
				// 그룹관리 업체명?
				const updateAgentIds = $.map(checkedRows, function (item, index) {
					return item.id;
				}); // 배열([]) 리턴
				console.log('updateAgentIds: ' + updateAgentIds);

				await g_modal(
					'/basic/agentGroupModify',
					{
						ids: updateAgentIds,
					},
					{
						size: '',
						title: '그룹관리',
						show_close_button: true,
						show_confirm_button: true,
						confirm_button_text: '저장',
					}
				).then((data) => {
					console.log('🚀 ~ data:', data);
					$modal.grid.reloadData();
				});
			}

			// g_modal 호출
		});

	// 그리드 이벤트 정의
	$modal.grid.on('click', async function (e) {
		const row = $modal.grid.getRow(e.rowKey);

		if (row && e.columnName != '_checked') {
			// 업체수정 모달 띄우기
			console.log('업체수정 모달 open!!');
			await g_modal(
				'/basic/agentModify',
				{
					id: row.id,
				},
				{
					size: 'xxl',
					title: '업체 수정',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '저장',
				}
			).then(() => {
				// 모달창이 닫히면 그리드가 갱신되도록 변경
				$modal.grid.reloadData();
			});
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
