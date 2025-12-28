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
	// TODO 어드민페이지에서 본사정보를 수정할 수 있는 경우, 고정표준실<->현자교정 변경 시 소재지 주소도 변겨되도록하기

	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

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
						$('form.reportModifyForm', $modal).find('input[name], textarea[name], select[name]').setupValues(parentInfo);

						// 접수구분 비활성화 처리
						$('input[name=orderType]', $modal).prop('disabled', true);

						// 교정유형, 교정상세유형 세팅
						const caliType = parentInfo.caliType;
						const caliTakeType = parentInfo.caliTakeType;
						$modal.setCaliType(caliType, caliTakeType);

						// 환경정보 세팅
						// NOTE 서버에서 record 클래스 내 환경정보를 String으로 받고 있기 때문에 문자열 형태로 매핑된 상태로 브라우저에 응답한 것
						if (parentInfo.environmentInfo != undefined && parentInfo.environmentInfo) {
							const environmentInfo = JSON.parse(parentInfo.environmentInfo);

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

						// 자식성적서가 존재하는 경우, 세팅
						if (childInfos.length > 0) {
							await $modal.setChildInfo(childInfos);
						}
					}
				}

				// 데이터세팅 이후, 접수구분 수정이 안 되도록 disabled 처리할 것
			}
		} catch (xhr) {
			console.error('에러발생');
			custom_ajax_handler(xhr);
		} finally {
		}

		// 자식성적서 세팅
		// 표준장비 데이터 세팅 TODO 추가와 삭제된 장비에 대해서 데이터를 어떻게 관리할 것인지 고민할 것 => is_visible이 아닌 레코드 자체를 delete 시키고 insert시키는 방향으로 생각할 것
		// 변경전과 변경후가 같은지 판단할 것

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
			// data: $modal.dataSource,
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
		});
	}; // End of init_modal

	// 모달 내 이벤트 정의
	$modal
		// 교정유형 선택
		.on('change', 'input[name=caliType]', function () {
			console.log('변동확인');
			const caliType = $(this).val();
			// 함수를 통해서 값 세팅
			$modal.setCaliType(caliType);
		})
		// 자식성적서 삭제
		.on('click', '.deleteChild', async function () {
			// 삭제는 저장이 아닌 실시간으로 반영되며, 삭제 이후엔 numbering이 변동된다.
			console.log('자식성적서 삭제');
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
						console.log('🚀 ~ resDelete:', resDelete);

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
		console.log(saveObj);
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
			let isValid = true;
			$.each($childTables, function (index, table) {
				const childObj = {};
				if (!isValid) {
					return false;
				}
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
			if (!isValid) {
				return false;
			}
		}

		// 자식성적서의 데이터 유효성 검사 결과
		saveObj.childReportInfos = childReportData;
		console.log('🚀 ~ saveObj:', saveObj);

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
					console.log('🚀 ~ resData:', resData);
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

	// 자식성적서 넘버링 세팅
	$modal.setChildNumbering = () => {
		const childReportTitle = $('.childTitle', $modal); // span
		$.each(childReportTitle, (index, ele) => {
			$(ele).text(`기기정보 (${index + 2})`);
		});
	};

	// 자식성적서 세팅
	$modal.setChildInfo = (rows) => {
		console.log('🚀 ~ rows:', rows);
		// 부모성적서 table 요소
		const $parentItemTable = $('.itemTable', $modal).eq(0);
		const $itemTd = $('.itemList', $modal);

		// 반복문만큼 세팅한다.
		$.each(rows, function (index, row) {
			const childTable = $parentItemTable.clone(); // 부모table 복사
			childTable.find('tbody tr').eq(0).remove(); // 첫 번째 tr 삭제 -> 반복문으로 새롭게 세팅
			const newEleTr = `<tr>
								<input type='hidden' name="id">
								<th colspan="3" class="border-0 text-left"><span
										class="pl-3 childTitle"></span> </th>
								<th class="border-0 "><button class="btn btn-danger deleteChild float-right"
										type="button">삭제</button></th>
                                </tr>`;
			$(childTable).find('tbody').prepend(newEleTr);
			$(childTable).find('input[name]').setupValues(row);
			$(childTable).addClass('childTable');
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
