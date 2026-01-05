$(function () {
	console.log('++ basic/itemModify.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	$modal = $candidates.first();
	let $modal_root = $modal.closest('.modal');

	let itemId;
	let smallItemCodeSetObj = {};
	let middleItemCodeSetAry = [];
	const delHistoryIds = [];

	$modal.init_modal = async (param) => {
		$modal.param = param;

		smallItemCodeSetObj = $modal.param.smallItemCodeSetObj;
		middleItemCodeSetAry = $modal.param.middleItemCodeSetAry;

		// 중분류 세팅
		const resSetMiddleCode = await $modal.setMiddleCode();
		// await new Promise(resolve => setTimeout(resolve, 500)); // 지연시간 주기

		// 업체id로 초기화 하기(수정)
		if ($modal.param?.id > 0) {
			// 옵셔널체이닝으로 체크
			itemId = Number($modal.param.id);
			// 데이터를 가져와서 세팅한다.
			try {
				const resGetItemInfo = await g_ajax(
					`/api/item/getItemInfo/${itemId}`,
					{},
					{
						'type': 'get',
					}
				);
				if (resGetItemInfo?.code > 0) {
					const itemInfo = resGetItemInfo.data;
					console.log('🚀 ~ itemInfo:', itemInfo);
					$('form[name=itemModifyForm]', $modal).find('input[name], select[name]').setupValues(itemInfo);
					$(`input[name=isInhousePossible][value=${itemInfo.isInhousePossible}]`, $modal).trigger('click'); // 당사가능여부 세팅
					$modal.setSmallCode(itemInfo.middleItemCodeId, itemInfo.smallItemCodeId, 500); // 중소분류 세팅
				}
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
			}

			// 교정수수료 데이터 세팅
			$modal.dataSource = {
				api: {
					readData: {
						url: `/api/item/getItemFeeHistory/${itemId}`,
						serializer: (grid_param) => {
							return $.param(grid_param);
						},
						method: 'GET',
					},
				},
			};
		}

		// 업체 담당자 그리드
		$modal.grid = new Grid({
			el: document.querySelector('.feeHistoryGrid'),
			columns: [
				{
					header: '기준일자',
					name: 'baseDate',
					className: 'cursor_pointer',
					editor: {
						type: DateEditor,
						options: { required: false }, // 필요하면 true
					},
					width: '250',
					align: 'center',
				},
				{
					header: '교정수수료',
					name: 'baseFee',
					editor: number_format_editor,
					width: '200',
					className: 'cursor_pointer',
					align: 'right',
				},
				{
					header: '비고',
					name: 'remark',
					editor: 'text',
					className: 'cursor_pointer',
					align: 'left',
				},
			],
			rowHeaders: ['checkbox'],
			editingEvent: 'click', // 원클릭으로 수정할 수 있도록 변경. 기본값은 'dblclick'
			data: itemId > 0 ? $modal.dataSource : null,
		});

		// 그리드 이벤트
		$modal.grid.on('response', function (e) {
			setTimeout(() => {
				const rowCnt = $modal.grid.getRowCount();
				if (rowCnt == 0) {
					$modal.grid.appendRow(
						{
							baseFee: 0,
						},
						{ at: 0 }
					);
				}
			}, 100);
		});
	};

	// 모달 내 이벤트 정의
	$modal
		// 중분류코드 변경 시
		.on('change', '.middleItemCode', function () {
			const middleCodeId = $(this).val();
			$modal.setSmallCode(middleCodeId);
		})
		// 수수료 이력 추가
		.on('click', '.addFeeHistory', function () {
			// 무조건 최신으로 표시
			$modal.grid.appendRow(
				{
					baseFee: 0,
				},
				{ at: 0 }
			);
		})
		// 수수료 이력 삭제
		.on('click', '.delFeeHistory', function () {
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 이력을 선택해주세요.');
				return false;
			} else {
				const removeRowKeyAry = [];
				// 삭제할 아이디는 배열에 담는다(저장 시 반영)
				checkedRows.forEach((row, index) => {
					if (row.id != undefined && row.id > 0) {
						delHistoryIds.push(row.id);
					}
					removeRowKeyAry.push(row.rowKey);
				});
				if (delHistoryIds.length > 0) {
					g_toast('삭제된 이력은 저장 시 반영됩니다.', 'info');
				}
				$modal.grid.removeRows(removeRowKeyAry);

				if ($modal.grid.getRowCount() === 0) {
					$modal.grid.appendRow({ baseFee: 0 }, { at: 0 });
				}
			}
		});

	// 중분류 세팅 함수
	$modal.setMiddleCode = async () => {
		const $middleItemCodeSelect = $('.middleItemCode', $modal);
		if (middleItemCodeSetAry.length > 0) {
			$.each(middleItemCodeSetAry, function (index, mCodeInfo) {
				const mOption = new Option(`${mCodeInfo.codeNum} ${mCodeInfo.codeName}`, mCodeInfo.id);
				$middleItemCodeSelect.append(mOption);
			});
		}
		await new Promise((resolve) => setTimeout(resolve, 100)); // 지연시간 주기
	};

	// 중분류 선택에 따른 소분류 세팅 함수
	$modal.setSmallCode = (middleCodeId, smallCodeId, layInitTime = 0) => {
		const $smallCodeSelect = $('.smallItemCode', $modal);
		const basicOption = new Option('소분류전체', '');
		$($smallCodeSelect).find('option').remove();
		$smallCodeSelect.append(basicOption);

		setTimeout(() => {
			if (!middleCodeId) {
				$smallCodeSelect.val(''); // '소분류전체'로 세팅
			} else {
				if (smallItemCodeSetObj[middleCodeId] != undefined && smallItemCodeSetObj[middleCodeId].length > 0) {
					const smallItemCodes = smallItemCodeSetObj[middleCodeId];
					smallItemCodes.forEach((row, index) => {
						const option = new Option(`${row.codeNum} ${row.codeName}`, row.id);
						$smallCodeSelect.append(option);
					});
					if (smallCodeId > 0) {
						$smallCodeSelect.val(smallCodeId);
					}
				}
			}
		}, layInitTime);
	};

	// 저장
	$modal.confirm_modal = async function (e) {
		// 업체정보 & 담당자 정보 유효성 체크 후, formdata에 데이터 담기
		const $form = $('.itemModifyForm', $modal);
		const formData = $form.serialize_object();
		$modal.grid.blur();
		console.log('🚀 ~ formData:', formData);

		// 중분류, 소분류, 품목명 체크
		const name = formData.name;
		console.log('🚀 ~ name:', name);
		if (!check_input(name)) {
			g_toast('품목명을 입력해주세요.', 'warning');
			return false;
		}

		const middleItemCodeId = formData.middleItemCodeId;
		console.log('🚀 ~ middleItemCodeId:', middleItemCodeId);
		const smallItemCodeId = formData.smallItemCodeId;
		console.log('🚀 ~ smallItemCodeId:', smallItemCodeId);
		if (!middleItemCodeId || !smallItemCodeId) {
			g_toast('중분류, 소분류는 필수입니다.', 'warning');
			return false;
		}

		// 교정수수료 데이터 검증하기
		const rowDatas = $modal.grid.getData();
		if (rowDatas.length === 0) {
			g_toast('수수료 이력은 최소 1개는 존재해야 합니다.', 'warning');
			return false;
		}
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
