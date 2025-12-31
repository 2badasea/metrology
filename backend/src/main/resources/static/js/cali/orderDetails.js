$(function () {
	console.log('++ cali/orderDetails.js');

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

	let caliOrderId = null;
	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		caliOrderId = document.getElementById('caliOrderId').value; // 타임리프를 통해 값 초기화 (쿼리스트링 활용도 가능)

		// 성적서 리스트 가져오기
		$modal.data_source = {
			api: {
				readData: {
					url: '/api/report/getOrderDetailsList',
					serializer: (grid_param) => {
						// TODO item, item_code 테이블 생성 이후에 중분류/소분류 필터링도 검색조건 추가 필요
						grid_param.orderType = $('form.searchForm .orderType', $modal).val() ?? ''; // 전체선택은 빈 값으로 넘어옴
						grid_param.statusType = $('form.searchForm .statusType', $modal).val() ?? ''; // 진행상태
						grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? 'all'; // 검색타입
						grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? ''; // 검색키워드
						grid_param.caliOrderId = caliOrderId; // 접수 id

						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.reportList'),
			columns: [
				{
					header: '구분',
					name: 'reportType',
					className: 'cursor_pointer',
					width: '60',
					align: 'center',
					formatter: function (data) {
						return data.value == 'SELF' ? '자체' : '대행';
					},
				},
				{
					header: '접수구분',
					name: 'orderType',
					className: 'cursor_pointer',
					width: '80',
					align: 'center',
					formatter: function (data) {
						return data.value == 'ACCREDDIT' ? '공인' : data.value == 'UNACCREDDIT' ? '비공인' : '시험';
					},
				},
				{
					header: '중분류코드',
					name: 'middleItemCodeNum',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '소분류코드',
					name: 'smallItemCodeNum',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '성적서번호',
					name: 'reportNum',
					className: 'cursor_pointer',
					width: '120',
					align: 'center',
				},
				{
					header: '기기명',
					name: 'itemName',
					className: 'cursor_pointer',
					// width: '120',
					align: 'center',
				},
				{
					header: '제작회사',
					name: 'itemMakeAgent',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '형식',
					name: 'itemFormat',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '기기번호',
					name: 'itemNum',
					className: 'cursor_pointer',
					width: '150',
					align: 'center',
				},
				{
					header: '관리번호',
					name: 'manageNo',
					className: 'cursor_pointer',
					width: '120',
					align: 'center',
				},
				{
					// 값이 아닌 formatter로 보여줄 것
					header: '진행상태',
					name: 'reportStatus',
					className: 'cursor_pointer',
					width: '70',
					align: 'center',
					formatter: function (data) {
						// TODO 별도로 상태값에 맞는 formatter 생성해서 이용할 것
						return '';
					},
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징
				perPage: 20, // 기본 20. 선택한 '행 수'에 따라 유동적으로 변경	=> change 이벤트를 통해 setPerPage() 함수 호출
			},
			rowHeaders: ['checkbox'],
			minBodyHeight: 663,
			bodyHeight: 663,
			data: $modal.data_source, // 그리드의 데이터를 초기화하는 과정에서 api 호출
			rowHeight: 'auto',
		});

		// 그리드 이벤트 정의
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			// 성적서 수정 모달을 호출한다.
			if (row && e.columnName != '_checked') {
				// 자체와 대행을 구분한다.
				const id = row.id;
				const reportNum = row.reportNum; // 성적서 번호
				const reportType = row.reportType; // 자체/대행 구분 -> 수정 모달 UI 구분위함
				// 자체
				if (reportType === 'SELF') {
					// 기술책임자 완료 및 결재 진행중인 건에 건은 '저장' 버튼 비활성화 (접수상세, 접수, 실무자, 기책 페이지별 구분)
					const isModifiable = row.approvalDateTime || row.reportStatus === 'SUCCESS' || row.approvalStatus !== 'IDLE' ? false : true;
					const resModal = await g_modal(
						'/cali/reportModify',
						{
							id: id,
						},
						{
							title: `성적서 수정 [성적서번호 - ${reportNum}]`,
							size: 'xxxl',
							show_close_button: true,
							show_confirm_button: isModifiable,
							confirm_button_text: '저장',
						}
					);

					// 모달이 정상적으로 닫히면 갱신이 일어나도록 한다.
					if (resModal) {
						$modal.grid.reloadData();
					}
				}
				// 대행
				else {
					g_toast('대행성적서 수정은 아직 제공되지 않습니다.', 'warning');
					return false;
				}
			}
		});
	};

	// 페이지 내 이벤트 정의
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1); // 변경된 페이지 옵션에 맞춰 페이지 렌더링
		})
		// 성적서 등록 모달 호출
		.on('click', '.addReport', async function () {
			const resModal = await g_modal(
				'/cali/registerMultiReport',
				{
					caliOrderId: caliOrderId,
				},
				{
					title: '성적서 등록',
					size: 'xxxl',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '저장',
					custom_btn_html_arr: [
						`<button type="button" class="btn btn-success addReportExcel btn-sm"><i class="bi bi-file-excel"></i>EXCEL 등록</button>`,
					],
				}
			);
			if (resModal) {
				$modal.grid.reloadData();
			}
		})
		// 행 수 변경
		.on('change', '.rowLeng', function () {
			const rowLeng = $(this).val(); // 행 수

			if (rowLeng > 0) {
				$modal.grid.setPerPage(rowLeng); // perPage옵션이 변경된 상태로 다시 재렌더링이 일어남
				// $modal.grid.readPage(1);	// setPerPage() 호출 후, 굳이 readPage() 호출할 필요없음.
				// setPerPage()와 아래 getPagination().movePageTo()는 잘 사용되지 않는 옵션이라 함(내용확인!)
				// $modal.grid.getPagination().movePageTo(1);	// 변경된 페이지 옵션에 맞춰 페이지 렌더링
			}
		})
		// 성적서 삭제
		.on('click', '.deleteReport', async function () {
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 성적서를 선택해주세요.', 'warning');
				return false;
			}

			// TODO 추후에 비밀번호 또는 권한이 생긴다면 사전에 체크할 것

			const $btn = $(this);
			let isFlag = true;
			const validateInfo = {};
			try {
				$btn.prop('disabled', true);

				// TODO 추후에 대행성적서가 추가되는 경우 조건 추가할 것
				$.each(checkedRows, (index, row) => {
					const orderType = row.orderType;
					const reportType = row.reportType; // 자체(self)/대행(agcy)

					// 자체와 대행을 분리한다.
					if (reportType === 'SELF') {
						if (row.workDatetime || row.approvalDateTime) {
							// FIX 결재상태가 'IDLE'인지도 체크 필요
							isFlag = false;
							g_toast('결재가 진행중인 건이 존재합니다.', 'warning');
							return false;
						} else {
							if (validateInfo[orderType] != undefined && Array.isArray(validateInfo[orderType])) {
								validateInfo[orderType].push(row.id);
							}
							// 접수구분(key)에 맞는 배열이 없는 경우, 배열을 초기화해주고 id를 넣는다.
							else {
								validateInfo[orderType] = [];
								validateInfo[orderType].push(row.id);
							}
						}
					}
					// 대행
					else {
						if (row.reportStatus === 'COMPLETE') {
							isFlag = false;
							g_toast('이미 완료된 대행 건이 존재합니다.', 'warning');
							return false;
						} else {
							// if (validateInfo['AGCY'] != undefined && Array.isArray(validateInfo['AGCY'])) {
							// 	validateInfo['AGCY'].push(row.id);
							// } else {
							// 	validateInfo['AGCY'] = [];
							// 	validateInfo['AGCY'].push(row.id);
							// }
							// NOTE 위의 코드를 개선한 방식
							if (!Array.isArray(validateInfo['AGCY'])) {
								validateInfo['AGCY'] = [];
							}
							validateInfo['AGCY'].push(row.id);
						}
					}
				});
			} catch (err) {
				isFlag = false;
				g_toast(`삭제처리 중 오류가 있습니다.<br>${err}`, 'error');
			}

			if (!isFlag) {
				$btn.prop('disabled', false);
				return false;
			}

			// g_ajax와 fetch api 혼용해서 사용해볼 것
			try {
				g_loading_message();
				const sendData = {
					caliOrderId: caliOrderId,
					validateInfo: validateInfo,
				};
				const resValiDate = await g_ajax('/api/report/isValidDelete', JSON.stringify(sendData), {
					contentType: 'application/json; charset=utf-8',
				});
				// 문제 없는 경우
				if (resValiDate?.code > 0) {
					const confirmDelete = await g_message('성적서 삭제', '성적서를 삭제하시겠습니까?', 'question', 'confirm');

					// 삭제 OK인 경우
					if (confirmDelete.isConfirmed === true) {
						// 삭제대상  id 합치기
						const deleteIds = [];
						$.each(validateInfo, function (orderType, array) {
							// 전개연산자 ...를 활용한다.
							deleteIds.push(...array);
						});

						const options = {
							method: 'DELETE',
							headers: {
								'Content-Type': 'application/json',
							},
							body: JSON.stringify({ deleteIds: deleteIds }),
						};
						const resDelete = await fetch('/api/report/deleteReport', options);
						const resJson = await resDelete.json();
						// 삭제 성공
						if (resJson?.code > 0) {
							await g_message('성적서 삭제', resJson.msg ?? '삭제되었습니다', 'success');
							// 그리드 갱신
							$modal.grid.reloadData();
						}
					} else {
						return false;
					}
				} else {
					// 유효하지 않은 경우, 해당 알림 안내
					g_toast(resValiDate.msg ?? '삭제 검증에 실패했습니다', 'warning');
					Swal.close();
					return false;
				}
			} catch (err) {
				custom_ajax_handler(err);
				Swal.close();
			} finally {
				$btn.prop('disabled', false);
			}

			// 선택된 성적서들이 해당 페이지에서 접수구분별 가장 마지막에 속하는지, 결재가 진행중인 건이 있는지 확인
			// 1. 브라우저 단에서 1차적으로 결재가 진행중인 건이 있는지만 판단
			// 2. api를 두 번 탈 것(서버차원에서 검증)
			// 3. 검증이 완료되었다면, 대상 id들만 삭제api로 보낼 것 (deletemapping 활용?)
		})
		;

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
