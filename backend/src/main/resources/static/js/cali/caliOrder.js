$(function () {
	console.log('++ cali/caliOrder.js');

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

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);
	};

	// 교정접수 리스트 가져오기
	$modal.data_source = {
		api: {
			readData: {
				url: '/api/caliOrder/getOrderList',
				serializer: (grid_param) => {
					// 접수시작/종료일, 세금계산서, 접수유형, 진행상태, 검색타입, 검색키워드를 넘긴다.
					grid_param.orderStartDate = $('form.searchForm .orderStartDate', $modal).val() ?? ''; // 접수일(시작일)
					grid_param.orderEndDate = $('form.searchForm .orderEndDate', $modal).val() ?? ''; // 접수일(마지막)
					grid_param.isTax = $('form.searchForm .isTax', $modal).val() ?? ''; // 세금계산서 발행여부
					grid_param.caliType = $('form.searchForm .caliType', $modal).val() ?? ''; // 교정유형(고정표준실/현장교정)
					grid_param.statusType = $('form.searchForm .statusType', $modal).val() ?? ''; // 진행상태
					grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? ''; // 검색타입
					grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? ''; // 검색키워드
					return $.param(grid_param);
				},
				method: 'GET',
			},
		},
	};

	// 그리드 정의
	$modal.grid = new Grid({
		el: document.querySelector('.orderList'),
		columns: [
			{
				header: '긴급여부',
				name: 'priority_type',
				className: 'cursor_pointer',
				width: '60',
				align: 'center',
				formatter: function (data) {
					return data.value == 'emergency' ? '긴급' : '일반';
				},
			},
			{
				// DB상에서는 datetime이지만, 화면에는 date타입으로 표현
				header: '접수일',
				name: 'orderDate',
				className: 'cursor_pointer',
				align: 'center',
				width: '80',
				formatter: function (data) {
					return !data.value ? '' : data.value;
				},
			},
			{
				header: '접수번호',
				name: 'orderNum',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
			},
			{
				header: '신청업체',
				name: 'custAgent',
				className: 'cursor_pointer',
				with: '150',
				align: 'center',
			},
			{
				header: '성적서발행처',
				name: 'reportAgent',
				className: 'cursor_pointer',
				width: '150',
				align: 'center',
			},
			{
				header: '성적서발행처 주소',
				name: 'reportAgentAddr',
				className: 'cursor_pointer',
				align: 'center',
			},
			// {
			// 	header: '출장일시',
			// 	name: 'btripDate',
			// 	className: 'cursor_pointer',
			// 	width: '100',
			// 	align: 'center',
			// 	formatter: function (data) {
			// 		console.log("🚀 ~ data:", data)
			// 		let row = data.row;
			// 		console.log("🚀 ~ row:", row)
			// 		let html = '';
			// 		if (row.btripStartDate && row.btripEndDate) {
			// 			html = `${row.btripStartDate} / ${row.btripEndDate}`;
			// 		}
			// 		// 출장시작일 ~ 종료일 형태로 작게 보여줄 것
			// 		return html;
			// 	},
			// },
			{
				header: '요청사항',
				name: 'grid_btn_remark',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
				formatter: function (data) {
					// 모달을 통해서 볼 수 있도록 할 것
					let btnClass = data.remark ? 'btn-info' : 'btn-secondary';
					return `<button type='button' class='btn ${btnClass} w-100 h-100 rounded-0' ><i class='bi bi-chat-left-text'></i></button>
					`;
				},
			},
			{
				header: '세금계산서',
				name: 'isTax',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
				formatter: function (data) {
					// data.isCheck == 'y'라면, checked 속성값 삽입
					let checked = data.value == 'y' ? 'checked' : '';
					// FIX toggle을 활용해서 보여주도록 한다.
					return `
							<input class="bs_toggle" type="checkbox" data-toggle="toggle" data-on="발행" data-off="미발행" data-width="100%" data-size="xs" ${checked}>
					`;
				},
			},
			{
				header: '접수내역',
				name: 'grid_btn_orderDetails',
				className: 'cursor_pointer',
				align: 'center',
				width: '70',
				formatter: function (data) {
					let row = data.row;
					let cntText = '';
					let btnClass = 'btn-secondary';
					if (row.reportCnt != undefined && row.reportCnt > 0) {
						cntText = '1개 이상 존재';
						btnClass = 'default p-0';
					} else {
						cntText = `<i class="bi bi-pencil-square"></i>`;
					}
					// FIX 성적서가 존재하는 경우, 성적서의 개수를 표기한다.
					return `
							<button type='button' class='btn ${btnClass} w-100 h-100 rounded-0' >${cntText}</button>
					`;
				},
			},
			{
				header: '대행',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
				formatter: function (data) {
					// FIX 대행 작업 시 진행.   접수내역과 같이 개수를 표기하도록 한다.
					return '';
				},
			},
			{
				header: '복사',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
				formatter: function (data) {
					// FIX 자체+대행 포함하여 성적서 개수가 1개 이상이어야 표기
					return '';
				},
			},
			{
				header: '교정신청서',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
				formatter: function (data) {
					// 버튼 2개로 구성할 것
					return `
								<div class="btn-group btn-group-sm w-100 h-100" role="group" aria-label="Basic example">
									<button type="button" class="h-100 rounded-0 btn btn-info downCaliOrder" data-type="excel"><i class="bi bi-download"></i></button>
									<button type="button" class="h-100 rounded-0 btn sendCaliOrder btn-secondary" data-type="mail"><i class="bi bi-envelope"></i></button>
								</div>
							`;
				},
			},
			{
				header: '완료통보서',
				className: 'cursor_pointer',
				width: '80',
				align: 'center',
				formatter: function (data) {
					// 모달을 통해서 볼 수 있도록 할 것
					return `
								'<button type="button" class="btn w-100 h-100 rounded-0 checkCpt">
									<i class="bi bi-pencil-square"></i>
								</button>
							`;
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
		rowHeight: 'auto',
		// minRowHeight: 36,
	});

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();

			// // 1. 검색 전, 시작일/종료일 체크하도록 하기
			const orderStartDate = $('.orderStartDate', $modal).val();
			const orderEndDate = $('.orderEndDate', $modal).val();

			// 1. 날짜가 없는 경우 체크
			if (!orderStartDate || !orderEndDate) {
				g_toast('접수일 조회 기간을 선택해주세요.', 'warning');
				return false;
			}

			// 2. 앞뒤 순서 체크 (YYYY-MM-DD 문자열 비교로도 충분)
			if (orderStartDate > orderEndDate) {
				g_toast('조회 종료일이 시작일보다 빠를 수 없습니다.', 'warning');
				return false;
			}

			// 3. 조회기간 1년 이내로 제한
			const startDate = new Date(orderStartDate);
			const endDate = new Date(orderEndDate);

			// 두 날짜 차이(ms) → 일(day) 단위로 변환
			const DAY_MS = 1000 * 60 * 60 * 24;
			const diffTime = endDate.getTime() - startDate.getTime(); // ms 차이
			const diffDays = diffTime / DAY_MS;

			// 365일 초과면 막기 (<= 365일까지만 허용)
			if (diffDays > 365) {
				g_toast('조회 기간은 최대 1년까지만 설정할 수 있습니다.', 'warning');
				return false;
			}

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
						size: 'xl',
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
								title: '삭제 완료',
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
		});

	// 그리드 이벤트 정의
	$modal.grid.on('click', async function (e) {
		const row = $modal.grid.getRow(e.rowKey);

		if (row && e.columnName != '_checked') {
			// 접수내역 호출
			if (e.columnName == 'grid_btn_orderDetails') {
				// TODO 나중에 window.open 방식을 get이 아닌 from으로 변경할 수 있도록 할 것
				window.open(`/cali/orderDetails?caliOrderId=${row.id}&custAgent=${row.custAgent}&reportAgent=${row.reportAgent}`, '_blank');
			}
			// 접수수정
			else {
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
