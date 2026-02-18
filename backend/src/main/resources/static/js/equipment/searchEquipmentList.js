$(function () {
	console.log('++ equipment/searchEquipmentList.js');

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

	const $selectedContainer = $('.selectedContainer', $modal);

	$modal.init_modal = async (param) => {
		$modal.param = param;

		const equipDatas = $modal.param.equipDatas ?? [];
		if (equipDatas.length > 0) {
			equipDatas.forEach((obj) => {
				$modal.setSelectedItem(obj);
			});
		}

		// 검색필터 분야 세팅
		await $modal.setEquipmentField();

		$modal.dataSource = {
			api: {
				readData: {
					url: '/api/equipment/getEquipmentList',
					serializer: (grid_param) => {
						const exceptIds = $selectedContainer
							.find('button')
							.map(function () {
								return $(this).attr('data-equipmentId');
							})
							.get();

						grid_param.equipmentFieldId = Number($('form.searchForm', $modal).find('.equipmentFieldSelect').val()); // 분야(전체는 0으로 받음)
						grid_param.isUse = $('form.searchForm', $modal).find('.isUse').val() ?? '';
						grid_param.isDispose = $('form.searchForm', $modal).find('.isDispose').val() ?? '';
						grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? ''; // 검색타입 (전체는 ''로 넘김)
						grid_param.keyword = $('form.searchForm', $modal).find('input[name=keyword]').val().trim(); // 검색키워드
						grid_param.exceptIds = exceptIds ?? []; // 검색제외 id
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.grid = gGrid('.searchEquipGrid', {
			columns: [
				{
					header: '분야',
					className: 'cursor_pointer',
					name: 'fieldName',
					width: '80',
					align: 'center',
				},
				{
					header: '관리번호',
					className: 'cursor_pointer',
					name: 'manageNo',
					width: '170',
					align: 'center',
				},
				{
					header: '장비명',
					className: 'cursor_pointer',
					name: 'name',
					// width: '200',
					align: 'center',
				},
				{
					header: '기기번호',
					className: 'cursor_pointer',
					name: 'serialNo',
					width: '200',
					align: 'center',
				},
				{
					header: '제작회사',
					className: 'cursor_pointer',
					name: 'makeAgent',
					width: '200',
					align: 'center',
				},
				{
					header: '모델명',
					className: 'cursor_pointer',
					name: 'modelName',
					width: '180',
					align: 'center',
				},
				{
					header: '관리담당(정)',
					className: 'cursor_pointer',
					name: 'primaryManager',
					width: '90',
					align: 'center',
				},
				{
					header: '관리담당(부)',
					className: 'cursor_pointer',
					name: 'secondaryManager',
					width: '90',
					align: 'center',
				},
				{
					header: '유휴여부',
					className: 'cursor_pointer',
					name: 'isUse',
					width: '70',
					align: 'center',
					formatter: ({ value }) => {
						return value == 'y' ? '' : '유휴';
					},
				},
				{
					header: '폐기여부',
					className: 'cursor_pointer',
					name: 'isDispose',
					width: '70',
					align: 'center',
					formatter: ({ value }) => {
						return value == 'y' ? '폐기' : '-';
					},
				},
				{
					header: '설치위치',
					className: 'cursor_pointer',
					name: 'install_location',
					width: '100',
					align: 'center',
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징
				perPage: 15,
			},
			data: $modal.dataSource,
			minBodyHeight: 600,
			bodyHeight: 600,
			rowHeight: 'auto',
			scrollX: false,
			// summary: {
			// 	height: 22,
			// 	position: 'bottom',
			// 	columnContent: {
			// 		manageNo: {
			// 			template: function () {
			// 				return ``;
			// 			},
			// 		},
			// 	},
			// },
		});

		// 그리드 렌더링 시, 검색결과 갯수를 표시한다.
		$modal.grid.on('response', function (e) {
			let selectedIds = $selectedContainer
				.find('button')
				.map(function () {
					return $(this).attr('data-equipmentId');
				})
				.get();
			setTimeout(() => {
				$modal.grid.getData().forEach((row) => {
					if (selectedIds.includes(String(row.id))) {
						$modal.grid.addRowClassName(row.rowKey, 'selectedRow');
					}
				});
			}, 300);
		});

		// 모달 내 그리드에 대한 이벤트
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			if (row) {
				// 상단에 선택된 정보를 표시한다.
				const $isSelectedBtn = $selectedContainer.find(`button[data-equipmentId="${row.id}"]`);
				if ($isSelectedBtn.length) {
					$isSelectedBtn.remove();
				} else {
					$modal.setSelectedItem(row);
				}
				$modal.grid.reloadData();
			}
		});

		// 페이지 내 이벤트
		$modal
			// 검색
			.on('submit', '.searchForm', function (e) {
				e.preventDefault();
				$modal.grid.getPagination().movePageTo(1);
			})
			// 모달 내 중분류 변경
			.on('change', '.middleCodeSelect', function () {
				const middleItemCodeId = $(this).val();
				$modal.setItemCode(middleItemCodeId);
			})
			// 입력값 초기화
			.on('click', 'button.resetBtn', function () {
				$('form.searchForm select, input', $modal).val('');
				$modal.grid.getPagination().movePageTo(1);
			})
			// 선택품목 삭제
			.on('click', '.removeItem', function () {
				// 버튼을 제거하고 리로딩 한다.
				$(this).closest('button').remove();
				$modal.grid.reloadData();
			});
	}; // End of init_modal

	// 선택한 것을 화면 상단에 표시
	$modal.setSelectedItem = (row) => {
		const itemBtn = `<button type='button' class='items m-1 btn btn-sm btn-info' data-equipmentId='${row.id}'>
							${row.name}
							<input type="hidden" class="selectedData" value='${JSON.stringify(row)}' />
							<span class='badge badge-light removeItem'>X</span>
            `;
		$selectedContainer.append(itemBtn);
	};

	$modal.setEquipmentField = async () => {
		const $fieldSelect = $('.equipmentFieldSelect', $modal);

		const feOptions = {
			method: 'GET',
		};
		const resGetField = await fetch(`/api/equipment/getEquipmentField?isUse=y`, feOptions);
		if (resGetField.ok) {
			const resData = await resGetField.json();
			if (resData.data != undefined && resData.data.length > 0) {
				fieldOptions = resData.data;
				fieldOptions.forEach((row) => {
					const codeOption = new Option(row.name, row.id);
					$fieldSelect.append(codeOption);
				});
			}
		} else {
		}
	};

	// 저장
	$modal.confirm_modal = async function (e) {
		// 선택된 데이터를 모두 담는다.
		const $selectedBtns = $selectedContainer.find('button');
		if ($selectedBtns.length > 0) {
			$modal.param.jsonData = $selectedBtns
				.map(function () {
					return JSON.parse($(this).find('.selectedData').val());
				})
				.get();
		}
		$modal_root.modal('hide');
		return $modal.param;
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
			initPage($modal);
		}
	}
});
