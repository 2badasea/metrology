$(function () {
	console.log('++ cali/reportModify.js');

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

	let id = null; // 성적서 id
	let middleItemCodeSet = []; // 중분류정보
	let smallItemCodeSet = {}; // 소분류정보
	// TODO 어드민페이지에서 본사정보를 수정할 수 있는 경우, 고정표준실<->현자교정 변경 시 소재지 주소도 변겨되도록하기

	$modal.init_modal = async (param) => {
		$modal.param = param;

		// 중분류와 소분류 정보를 가져와서 초기화를 진행한다.
		await $modal.initItemCodeSet();

		id = $modal.param.id;
		// 성적서 데이터를 가져온다.(자식성적서 및 표준장비 데이터 포함)
		const feOptions = {
			method: 'GET',
			// header, body 모두 생략
		};
		try {
			const resReportInfo = await fetch(`/api/report/getReportInfo?id=${id}`, feOptions);
			if (resReportInfo.ok) {
				const reportInfoJson = await resReportInfo.json();
				if (reportInfoJson?.code > 0) {
					const reportInfo = reportInfoJson.data;
					const parentInfo = reportInfo.reportInfo ?? {};
					const childInfos = reportInfo.childReportInfos ?? {}; // 없을 경우 빈 객체로 받기

					// 데이터 세팅
					if (parentInfo) {
						$('form.reportModifyForm', $modal)
							.find('input[name], textarea[name], select[name]')
							.not('select[name=middleItemCodeId], select[name=smallItemCodeId]')
							.not('.childTable input[name]') // 자식요소도 제외
							.setupValues(parentInfo);

						// 접수구분 비활성화 처리 (성적서 수정 모달 내에선 수정 불가)
						$('input[name=orderType]', $modal).prop('disabled', true);

						// 교정유형, 교정상세유형 세팅
						const caliType = parentInfo.caliType;
						const caliTakeType = parentInfo.caliTakeType;
						$modal.setCaliType(caliType, caliTakeType);

						// 환경정보 세팅
						// NOTE 서버에서 record 클래스 내 환경정보를 String으로 받고 있기 때문에 문자열 형태로 매핑된 상태로 브라우저에 응답한 것
						if (parentInfo.environmentInfo != undefined && parentInfo.environmentInfo) {
							const environmentInfo = JSON.parse(parentInfo.environmentInfo); // JSON 형태로 파싱(역직렬화)

							// key별로 항목에 세팅한다.
							Object.entries(environmentInfo).forEach(([key, value]) => {
								$(`input[name=${key}]`, $modal).val(value);
							});
						}

						// 소급성문구 세팅
						if (parentInfo.tracestatementInfo != undefined && parentInfo.tracestatementInfo) {
							const tracestatementInfo = JSON.parse(parentInfo.tracestatementInfo);
							Object.entries(tracestatementInfo).forEach(([key, value]) => {
								$(`input[name=${key}]`, $modal).val(value);
							});
						}

						// 중소분류 세팅
						await $modal.setItemCode(parentInfo.middleItemCodeId, parentInfo.smallItemCodeId);

						// 자식성적서가 존재하는 경우, 세팅
						if (childInfos.length > 0) {
							await $modal.setChildInfo(childInfos);
						}

						// 품목관련 정보는 수정이 불가능하도록 막을 것(금액, 비고, 추가금액사유 제외)
						$modal
							.find('.itemTable input[name]')
							.not('.itemTable input[name=additionalFee], input[name=caliFee], input[name=remark], input[name=additionalFeeCause]')
							.prop('readonly', true);
					}
				}
			}
		} catch (xhr) {
			custom_ajax_handler(xhr);
		} finally {
		}

		$modal.equipmentDataSource = {
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

		// 표준장비 그리드 (더미데이터만 우선 표시)
		$modal.grid = new Grid({
			el: document.querySelector('.equipageList'),
			columns: [
				{
					header: '구분',
					name: 'reportType',
					className: 'cursor_pointer',
					width: '',
					align: 'center',
				},
				{
					header: '성적서번호',
					name: 'reportNum',
					className: 'cursor_pointer',
					width: '',
					align: 'center',
				},
			],
			// minBodyHeight: gridBodyHeight,
			// bodyHeight: gridBodyHeight,
			// data: $modal.equipmentDataSource,
			data: [
				{
					'reportType': 'self',
					'reportNum': 'BD25-0001-001',
					'itemName': '테스트 기기',
					'itemNum': '2025122101',
					'itemFormat': '25 ~ 45(kg)',
				},
			],
			pageOptions: {
				perPage: 15,
			},
			draggable: true,
		});
	}; // End of init_modal

	// 중분류와 소분류코드를 가져와서 중분류select 세팅 및 소분류코드 데이터를 초기화시킨다.
	$modal.initItemCodeSet = async () => {
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
						const option = new Option(`${row.codeNum} ${row.codeName}`, row.id);
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

	// (parentInfo.middleItemCodeId, parentInfo.smallItemCodeId)
	$modal.setItemCode = (middleItemCodeId, smallItemCodeId, layInitTime = 0) => {
		const $middleCodeSelect = $('.middleCodeSelect', $modal);
		if (middleItemCodeId) {
			$middleCodeSelect.val(middleItemCodeId);
		}
		const $smallCodeSelect = $('.smallCodeSelect', $modal);
		const basicOption = new Option('소분류전체', '');
		$($smallCodeSelect).find('option').remove();
		$smallCodeSelect.append(basicOption);

		setTimeout(() => {
			if (!middleItemCodeId) {
				$smallCodeSelect.val(''); // '소분류전체'로 세팅
			} else {
				if (smallItemCodeSet[middleItemCodeId] != undefined && smallItemCodeSet[middleItemCodeId].length > 0) {
					const smallItemCodes = smallItemCodeSet[middleItemCodeId];
					smallItemCodes.forEach((row, index) => {
						const option = new Option(`${row.codeNum} ${row.codeName}`, row.id);
						$smallCodeSelect.append(option);
					});
					if (smallItemCodeId > 0) {
						$smallCodeSelect.val(smallItemCodeId);
					}
				}
			}
		}, layInitTime);
	};

	// 모달 내 이벤트 정의
	$modal
		// 중분류 변경
		.on('change', '.middleCodeSelect', function () {
			const middleItemCodeId = $(this).val();
			$modal.setItemCode(middleItemCodeId);
		})
		// 교정유형 선택
		.on('change', 'input[name=caliType]', function () {
			const caliType = $(this).val(); // 변경된 타입
			// 함수를 통해서 값 세팅
			$modal.setCaliType(caliType);
		})
		// 자식성적서 삭제
		.on('click', '.deleteChild', async function () {
			// 삭제는 저장이 아닌 실시간으로 반영되며, 삭제 이후엔 numbering이 변동된다.
			const $btn = $(this);

			try {
				$btn.prop('disabled', true);
				const $deleteTable = $btn.closest('table');
				const deleteId = $deleteTable.find('input[name=id]').val();
				// id가 존재하는 경우
				if (deleteId && Number(deleteId) > 0) {
					const deleteConfirm = await g_message(
						'성적서 삭제',
						'성적서를 삭제하시겠습니까?<br>저장과 관계없이 바로 삭제됩니다. ',
						'warning',
						'confirm'
					);
					if (deleteConfirm.isConfirmed === true) {
						g_loading_message();
						// 삭제요청은 DELETE http method 형식으로 보낸다.
						const resDelete = await g_ajax(`/api/report/delete/${deleteId}`, {}, { type: 'DELETE' });

						// 삭제성공 시, 대상 table을 remove시키고, 넘버링을 새롭게 한다.
						if (resDelete?.code > 0) {
							await g_message('성적서 삭제', resDelete.msg, 'success', 'alert');
							// 영역을 삭제 후, numbering을 새롭게 한다.
							$deleteTable.remove();
							$modal.setChildNumbering();
						}
					} else {
						return false;
					}
				}
				// id가 존재하지 않는 경우엔 요소만 날린다.
				else {
					$deleteTable.remove();
					$modal.setChildNumbering();
				}
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
				$btn.prop('disabled', false);
			}
		})
		// 자식성적서 추가
		.on('click', '.addChild', function () {
			// 부모성적서의 기기정보를 복사해서 값을 초기화 한 다음 마지막에 붙여넣고 새롭게 넘버링
			const $btn = $(this);
			const $table = $btn.closest('table');
			const newTable = $table.clone();
			newTable.find('tbody tr').eq(0).remove();
			const newTrEle = `<tr>
								<input type='hidden' name="id">
								<input type='hidden' name="middleItemCodeId">
								<input type='hidden' name="smallItemCodeId">								
								<th colspan="3" class="border-0 text-left"><span
										class="pl-3 childTitle"></span> </th>
								<th class="border-0 "><button class="btn btn-danger deleteChild float-right"
										type="button">삭제</button></th>
                                </tr>`;
			newTable.find('tbody').prepend(newTrEle);
			newTable.addClass('childTable');
			newTable.find('input[name]').val(''); // 값 초기화
			$('.itemList', $modal).append(newTable); // td의 마지막에 요소를 붙이고,
			$modal.setChildNumbering(); // 넘버링을 한다.
		})
		// 교정일자 '오늘' 클릭
		.on('click', '.setTodayDate', function () {
			const today = new Date();

			const year = today.getFullYear();
			const month = (today.getMonth() + 1).toString().padStart(2, '0');
			const day = today.getDate().toString().padStart(2, '0');

			const dateString = year + '-' + month + '-' + day;

			$('input[name=caliDate]', $modal).val(dateString);
		})
		.on('click', '.itemSearch', async function (e) {
			const $btn = $(this);
			const $targetTable = $btn.closest('table');
			$modal.itemSearch($targetTable);
		})
		.on('keydown', 'input[name=itemName]', async function (e) {
			if (e.keyCode == 13) {
				const $input = $(this);
				const $targetTable = $input.closest('table');
				await $modal.itemSearch($targetTable);
			}
		})
		// 품목정보 수정 안 되는 것 안내
		.on('click', '.notMoidfy', function () {
			g_toast('성적서 수정 시, 품목정보 변경은 조회로만 가능합니다.', 'warning');
			return false;
		});

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장진행');
		const $btn = $('button.btn_save', $modal_root);

		// TODO 1. 표준장비 그리드 구현 시, 별도 처리 필요
		// TODO 2. 품목관리 페이지 구현 시, 품목 자동저장 로직 추가할 것
		const $form = $('.reportModifyForm', $modal);

		// form 요소중에 자식 테이블의 하위 요소를 제외한 요소들을 대상으로 값을 담는다.
		const saveData = $form.find('input[name], textarea[name], select[name]').not('.childTable input[name]');
		const saveObj = {};
		$.each(saveData, function (index, ele) {
			const type = $(ele).attr('type');
			const name = $(ele).attr('name');
			const value = $(ele).val();
			// TODO 아래와 같이 처리하는 방식 -> common.js에 별도로 만들기
			if ('checkbox' == type) {
				if ('undefined' == typeof saveObj[name]) {
					saveObj[name] = [];
				}
				if ($(ele).is(':checked')) {
					saveObj[name].push(value);
				}
			} else if ('radio' == type) {
				if ($(ele).is(':checked')) {
					saveObj[name] = value;
				}
			} else {
				saveObj[name] = value;
			}
		});

		// 담긴 데이터엔 접수관련 데이터도 존재하지만, record 클래스에서 필드로 정의하지 않음으로써 필터링하기
		saveObj.id = id;

		// 교정료나 추가금액의 경우, comma를 제거하고 삽입
		saveObj.caliFee = Number(uncomma(saveObj.caliFee) || 0);
		saveObj.additionalFee = Number(uncomma(saveObj.additionalFee) || 0);

		// 소급성 문구 데이터 담기
		const tracestatementInfo = {};
		$('.tracestatementInfo', $modal).each((index, input) => {
			const key = $(input).attr('name');
			const value = $(input).val();
			tracestatementInfo[key] = value;
		});
		// NOTE string형태로 해당 key를 받기 위해선 애초에 값 자체를 문자열로 직렬화 시킨 상태로 값을 담아야 한다.
		saveObj.tracestatementInfo = JSON.stringify(tracestatementInfo);

		// 환경정보 데이터 담기
		const environmentInfo = {};
		$('.environmentInfo', $modal).each((index, input) => {
			const key = $(input).attr('name');
			const value = $(input).val();
			environmentInfo[key] = value;
		});
		// NOTE string형태로 해당 key를 받기 위해선 애초에 값 자체를 문자열로 직렬화 시킨 상태로 값을 담아야 한다.
		saveObj.environmentInfo = JSON.stringify(environmentInfo);

		const childReportData = [];
		// 자식성적서가 존재하는 경우, 별도로 받을 것
		const $childTables = $('.childTable', $modal);
		if ($childTables.length > 0) {
			let isValid = true; // 기기명이 존재하지 않을 경우 리턴
			$.each($childTables, function (index, table) {
				const childObj = {};
				if (!isValid) {
					return false;
				}
				// <tabel> 요소 내부의 input들에 대해서도 순회로 검증 및 데이터 담기
				$(table)
					.find('input[name]')
					.each(function (idx, input) {
						const key = $(input).attr('name');
						let val = $(input).val();

						if (key == 'itemName' && !check_input(val)) {
							g_toast('기기명이 존재하지 않습니다.', 'warning');
							isValid = false;
							return false;
						}
						// 금액처리
						if (key == 'caliFee' || key == 'additionalFee') {
							val = Number(uncomma(val) || 0);
						}
						// id가 없는 경우 null을 넣어준다
						if (key === 'id' && !val) {
							val = null;
						}
						// 교정주기가 없다면 기본적으로 12를 삽입한다.
						if (key === 'itemCaliCycle' && !val) {
							val = 12;
						}
						childObj[key] = val;
					});
				childReportData.push(childObj);
			});
			// 검증에 실패한 경우 return
			if (!isValid) {
				return false;
			}
		}

		// 문제가 없었다면 데이터를 담는다.
		saveObj.childReportInfos = childReportData;
		// 저장로직 진행
		try {
			$btn.prop('disabled', true);
			const confirmSave = await g_message('성적서 수정', '성적서를 저장하시겠습니까?', 'question', 'confirm');
			if (confirmSave.isConfirmed === true) {
				g_loading_message();
				const options = {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json; charset=utf-8',
					},
					body: JSON.stringify(saveObj),
				};
				const resSave = await fetch('/api/report/updateReport', options);
				if (resSave.ok) {
					const resData = await resSave.json();
					if (resData?.code > 0) {
						await g_message('성적서 수정', resData.msg ?? '수정되었습니다', 'success', 'alert');
						$modal_root.modal('hide');
						return true;
					} else {
						await g_message('성적서 수정', '수정 실패', 'error', 'alert');
					}
				} else {
					swal.close();
				}
			} else {
				return false;
			}
		} catch (err) {
			console.error(err);
			custom_ajax_handler(err);
		} finally {
			swal.close();
			$btn.prop('disabled', false);
		}
	};

	// 리턴 모달 이벤트
	$modal.return_modal = async function (e) {
		$modal.param.res = true;
		$modal_root.modal('hide');
		return $modal.param;
	};

	// 품목조회 데이터 세팅
	$modal.itemSearch = async (table) => {
		const isParent = table.hasClass('childTable') ? false : true;

		const resModal = await g_modal(
			'/basic/searchItemList',
			{},
			{
				size: 'xxxl',
				title: '교정 품목 리스트',
				show_close_button: true,
			}
		);

		// 반환된 데이터 존재 시 삽입
		if (resModal.jsonData != undefined) {
			const d = resModal.jsonData;
			console.log(d);
			table.find('input[name=itemId]').val(d.id);
			table.find('input[name=itemCaliCycle]').val(d.caliCycle);
			table.find('input[name=itemName]').val(d.name);
			table.find('input[name=itemNameEn]').val(d.nameEn);
			table.find('input[name=itemMakeAgent]').val(d.makeAgent);
			table.find('input[name=itemMakeAgentEn]').val(d.makeAgentEn);
			table.find('input[name=itemFormat]').val(d.format);
			table.find('input[name=itemNum]').val(d.num);
			table.find('input[name=caliFee]').val(comma(d.fee ?? 0));
			// 부모와 자식의 중분류소분류는 별도로 한다.
			if (isParent) {
				$modal.setItemCode(d.middleItemCodeId, d.smallItemCodeId, 500);
			} else {
				table.find('input[name=middleItemCodeId]').val(d.middleItemCodeId);
				table.find('input[name=smallItemCodeId]').val(d.smallItemCodeId);
			}
		}
	};

	// 자식성적서 넘버링 세팅
	$modal.setChildNumbering = () => {
		const childReportTitle = $('.childTitle', $modal); // span
		$.each(childReportTitle, (index, ele) => {
			$(ele).text(`기기정보 (${index + 2})`);
		});
	};

	// 자식성적서 세팅
	$modal.setChildInfo = (rows) => {
		// 부모성적서 table 요소
		const $parentItemTable = $('.itemTable', $modal).eq(0);
		const $itemTd = $('.itemList', $modal); // 자식성적서를 붙여줄 요소

		// 반복문만큼 세팅한다.
		$.each(rows, function (index, row) {
			const childTable = $parentItemTable.clone(); // 부모table 복사
			childTable.find('tbody tr').eq(0).remove(); // 첫 번째 tr 삭제 -> 반복문으로 새롭게 세팅
			const newEleTr = `<tr>
								<input type='hidden' name="id">
								<input type='hidden' name="middleItemCodeId">
								<input type='hidden' name="smallItemCodeId">
								<th colspan="3" class="border-0 text-left"><span
										class="pl-3 childTitle"></span> </th>
								<th class="border-0 "><button class="btn btn-danger deleteChild float-right"
										type="button">삭제</button></th>
                                </tr>`;
			$(childTable).find('tbody').prepend(newEleTr); // 자식성적서는 새로운 tr로 교체
			$(childTable).find('input[name]').setupValues(row); // 자식성적서의 id도 세팅됨
			$(childTable).addClass('childTable'); // 부모테이블, 자식테이블 구분
			$itemTd.append(childTable);
		});

		// 자식성적서 numbering 세팅
		$modal.setChildNumbering();
	};

	// 교정유형, 교정상세유형 변경 이벤트
	$modal.setCaliType = (caliType, caliTakeType = '') => {
		// 현장교정인 경우
		if (caliType === 'SITE') {
			$('div.siteDiv', $modal).removeClass('d-none');
			$('div.standardDiv', $modal).addClass('d-none');
		}
		// 고정표준실인 경우
		else {
			$('div.siteDiv', $modal).addClass('d-none');
			$('div.standardDiv', $modal).removeClass('d-none');
		}

		if (caliTakeType) {
			$(`input[name=caliTakeType][value=${caliTakeType}]`, $modal).prop('checked', true);
		} else {
			if (caliType === 'SITE') {
				$('input[name=caliTakeType][value=SITE_SELF]', $modal).prop('checked', true); // '현장교정'이 기본값
			} else {
				$('input[name=caliTakeType][value=SELF]', $modal).prop('checked', true); // 방문이 기본값
			}
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
