$(function () {
	console.log('++ member/memberManage.js');

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

	// 직원관리 리스트
	$modal.init_modal = (param) => {
		$modal.param = param;

		// 업체관리 리스트 가져오기
		$modal.data_source = {
			api: {
				readData: {
					url: '/api/member/getMemberList',
					// 'serializer'는 토스트 그리드에서 제공
					serializer: (grid_param) => {
						grid_param.workType = $modal.find('select[name=workType]').val(); // 재직여부
						grid_param.searchType = $modal.find('select[name=searchType]').val(); // 검색 타입
						grid_param.keyword = $modal.find('input[name=keyword]').val(); // 검색 키워드
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 직원관리 그리드
		$modal.grid = new Grid({
			el: document.querySelector('.memberList'),
			columns: [
				// 사번, 아이디, 이메일, 이름, 영문이름, 휴대번호, 부서, 직급, 상태(재직여부)
				{
					header: '사번',
					name: 'compayNo',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '아이디',
					name: 'loginId',
					className: 'cursor_pointer',
					align: 'center',
					sortable: true,
				},
				{
					header: '이메일',
					name: 'email',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
					sortable: true,
				},
				{
					header: '이름',
					name: 'name',
					width: '100',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '휴대번호',
					name: 'tel',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				// {
				// 	header: '부서',
				// 	name: 'departmentName',
				// 	width: '100',
				// 	className: 'cursor_pointer',
				// 	align: 'center',
				// },
				// {
				// 	header: '직급',
				// 	name: 'levelName',
				// 	className: 'cursor_pointer',
				// 	align: 'center',
				// },
				{
					header: '상태(재직유무)',
					name: 'workType',
					className: 'cursor_pointer',
					width: '130',
					align: 'center',
					formatter: function ({ row, value }) {
						return value == 0 ? '재직' : value == 1 ? '휴직' : '퇴직';
					},
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징
				perPage: 20,
			},
			rowHeaders: ['checkbox'],
			minBodyHeight: 663,
			bodyHeight: 663,
			data: $modal.data_source,
		});

		// 그리드 이벤트 정의
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);

			if (row && e.columnName != '_checked') {
				const id = row.id;
				// 직원 등록/수정 페이지 이동
				location.href = `/member/memberModify?id=${id}`;
			}
		});

	};	// End init_modal

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1);
		})
		// 등록
		.on('click', '.addMember', async function (e) {
			e.preventDefault();
			location.href = `/member/memberModify`;
		})
		// 삭제
		.on('click', '.deleteAgentBtn', async function (e) {
			e.preventDefault();
			const gUserAuth = $('.gLoginAuth').val();
			if (gUserAuth !== 'admin') {
				g_toast('권한이 없습니다', 'warning');
				return false;
			}

			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 업체를 선택해주세요.', 'warning');
				return false;
			}
			// 각 업체의 id를 담는다. (새로운 배열에 담기 위해 map 사용)
			let delAgentIds = $.map(checkedRows, function (row, index) {
				return row.id;
			});

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
							type: 'DELETE',
							contentType: 'application/json; charset=utf-8',
						}
					);
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
		// 모달이 아닌 일반 페이지인 경우엔 아래 init_page가 동작한다.
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
