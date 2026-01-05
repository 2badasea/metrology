$(function () {
	console.log('++ basic/item.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	$modal = $candidates.first();
	let $modal_root = $modal.closest('.modal');

	let smallItemCodeSet = {};
    let middleItemCodeSet = [];

	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		try {
			const resGetItemCodeSet = await g_ajax(
				'/api/basic/getItemCodeInfos',
				{},
				{
					type: 'GET',
				}
			);

			if (resGetItemCodeSet?.code > 0) {
				const itemCodeSet = resGetItemCodeSet.data;
				if (itemCodeSet.middleCodeInfos) {
                    middleItemCodeSet = itemCodeSet.middleCodeInfos; 
					// 반복문으로 세팅
					const $middleCodeSelect = $('.middleCodeSelect', $modal);
					$.each(itemCodeSet.middleCodeInfos, function (index, row) {
						const option = new Option(row.codeNum, row.id);
						$middleCodeSelect.append(option);
					});
				}
				if (itemCodeSet.smallCodeInfos) {
					smallItemCodeSet = itemCodeSet.smallCodeInfos;
				}
			} else {
				console.log('호출실패');
				throw new Error('/api/basic/getItemCodeInfos 호출 실패');
			}
		} catch (xhr) {
			console.error('통신에러');
			custom_ajax_handler(xhr);
		}
	};

	// 품목 리스트 가져오기
	$modal.dataSource = {
		api: {
			readData: {
				url: '/api/item/getItemList',
				serializer: (grid_param) => {
					grid_param.isInhousePossible = $('form.searchForm', $modal).find('.isInhousePossible').val() ?? ''; // 전체선택 또는 값이 없을 땐 빈 값
					grid_param.middleItemCodeId = Number($('form.searchForm .middleCodeSelect', $modal).val() ?? 0); // 전체선택(''), null, undefined 모두 커버
					grid_param.smallItemCodeId = Number($('form.searchForm .smallCodeSelect', $modal).val() ?? 0);
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
		el: document.querySelector('.itemList'),
		columns: [
			{
				header: '생성타입',
				name: 'createType',
				className: 'cursor_pointer',
				width: '60',
				align: 'center',
				formatter: function (data) {
					return data.value == 'BASIC' ? '기본' : '자동등록';
				},
			},
			{
				header: '기기명',
				name: 'name',
				className: 'cursor_pointer',
				align: 'center',
				whiteSpace: 'pre-line',
				// width: '80',
			},
			{
				header: '기기명(영문)',
				name: 'nameEn',
				className: 'cursor_pointer',
				// width: '120',
				whiteSpace: 'pre-line',
				align: 'center',
			},
			{
				header: '제작회사',
				name: 'makeAgent',
				className: 'cursor_pointer',
				with: '120',
				whiteSpace: 'pre-line',
				align: 'center',
			},
			{
				header: '제작회사(영문)',
				name: 'makeAgentEn',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
			},
			{
				header: '형식',
				name: 'format',
				width: '120',
				whiteSpace: 'pre-line',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '기기번호',
				className: 'cursor_pointer',
				name: 'num',
				width: '150',
				align: 'center',
			},
			{
				header: '교정주기',
				name: 'caliCycle',
				width: '70',
				className: 'cursor_pointer',
				align: 'center',
				formatter: function ({value}) {
                    if (!value || value == 0) {
                        return '-';
                    } else {
                        return `${Number(value)}개월`;
                    }
				},
			},
			{
				header: '수수료',
				name: 'fee',
				width: '70',
				className: 'cursor_pointer',
				align: 'right',
				formatter: function (data) {
					return `${number_format(Number(data.value ?? 0))}`;
				},
			},
			{
				header: '비고',
				name: 'remark',
				width: '100',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '당사가능여부',
				name: 'isInhousePossible',
				width: '100',
				className: 'cursor_pointer',
				align: 'center',
				formatter: function ({ row, value }) {
					return value == 'y' ? '가능' : '불가';
				},
			},
			{
				header: '복사',
				name: 'grid_btn_copy',
				width: '60',
				className: 'cursor_pointer',
				align: 'center',
			},
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 20,
		},
		rowHeaders: ['checkbox'],
		data: $modal.dataSource,
		minBodyHeight: 641,
		bodyHeight: 641,
		rowHeight: 'auto',
		scrollX: false,
		summary: {
			height: 22,
			position: 'bottom',
			columnContent: {
				name: {
					template: function () {
						return ``;
					},
				},
			},
		},
	});

	// 그리드 이벤트 정의
	$modal.grid.on('click', async function (e) {
		const row = $modal.grid.getRow(e.rowKey);

		if (row && e.columnName != '_checked') {
			// 복사
			if (e.columnName == 'grid_btn_copy') {
				console.log('품목복사 클릭');
			}
			// 접수수정
			else {
				try {
					const resModal = await g_modal(
						'/basic/itemModify',
						{
							id: row.id,
						},
						{
							size: 'xl',
							title: '품목 수정',
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
    
    // 그리드 렌더링 시, 검색결과 갯수를 표시한다.
    $modal.grid.on('response', function (e) {
        let jsonRow = JSON.parse(e.xhr.response);
        const totalCnt = jsonRow.data.pagination.totalCount ?? 0;
        const rowCnt = jsonRow.data.contents.length ?? 0;
        $modal.grid.setSummaryColumnContent('name', {
            template: () => `총 ${number_format(totalCnt)} 건 중 ${number_format(rowCnt)} 건 조회`
        });        
    })

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();

			$modal.grid.getPagination().movePageTo(1);
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
		// 등록
		.on('click', '.addItem', async function (e) {
			e.preventDefault();

			try {
				const resModal = await g_modal(
					'/basic/itemModify',
					{},
					{
						title: '품목 등록',
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
		.on('click', '.deleteItem', async function (e) {
			e.preventDefault();

			// 1. 그리드 내 체크된 항목 확인
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 품목을 선택해주세요.', 'warning');
				return false;
			} else {
				// 각 접수의 id를 담는다.
				let delItemIds = $.map(checkedRows, function (row, index) {
					return row.id;
				});

				// 2. 삭제유무 confirm 확인
				if (confirm('정말 삭제하시겠습니까?\n')) {
					g_loading_message('삭제 처리 중입니다...');

					try {
						const sendData = {
							ids: delItemIds,
						};

						const resDelete = await g_ajax(
							'/api/basic/deleteItem',
							JSON.stringify(sendData),

							{
								contentType: 'application/json; charset=utf-8',
							}
						);
						if (resDelete?.code === 1) {
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
		})
		// 중분류코드 변경 시
		.on('change', '.middleCodeSelect', function () {
			const middleCodeId = $(this).val();
			const $smallCodeSelect = $('.smallCodeSelect', $modal);
			const basicOption = new Option('소분류전체', '');
			$($smallCodeSelect).find('option').remove();
			$smallCodeSelect.append(basicOption);
			if (!middleCodeId) {
				$smallCodeSelect.val(''); // '소분류전체'로 세팅
			} else {
				if (smallItemCodeSet[middleCodeId] != undefined && smallItemCodeSet[middleCodeId].length > 0) {
					const smallItemCodes = smallItemCodeSet[middleCodeId];
					// NOTE 아래 형태는 비권장. DOM요소로 오해할 수 있다.
					// $(smallItemCodes).each((index, row) => {})
					smallItemCodes.forEach((row, index) => {
						const option = new Option(`${row.codeNum}`, row.id);
						$smallCodeSelect.append(option);
					});
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
