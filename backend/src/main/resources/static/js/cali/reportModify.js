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
					console.log('🚀 ~ parentInfo:', parentInfo);
					const childInfos = reportInfo.childReportInfos ?? {}; // 없을 경우 빈 객체로 받기
					console.log('🚀 ~ childInfos:', childInfos);

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
							console.log('🚀 ~ parentInfo:', parentInfo);

							// key별로 항목에 세팅한다.
							Object.entries(environmentInfo).forEach(([key, value]) => {
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
		});

	// 저장
	$modal.confirm_modal = async function (e) {};

	// 리턴 모달 이벤트
	$modal.return_modal = async function (e) {
		$modal.param.res = true;
		$modal_root.modal('hide');
		return $modal.param;
	};

	// 자식성적서 세팅
	$modal.setChildInfo = (rows) => {
		console.log('🚀 ~ rows:', rows);
		// 부모성적서 table 요소
		const $parentItemTable = $('.itemTable', $modal);
		const $itemTd = $('.itemList', $modal);

		// 반복문만큼 세팅한다.
		$.each(rows, function (index, row) {
			const childTable = $parentItemTable.clone(); // 부모table 복사
			childTable.find('tbody tr').eq(0).remove(); // 첫 번째 tr 삭제 -> 반복문으로 새롭게 세팅
			const orderNo = (index + 1);
			const newEleTr = `<tr>
								<input type='hidden' name="id">
								<th colspan="3" class="border-0 text-left"><span
										class="pl-3">기기정보 (${orderNo})</span> </th>
								<th class="border-0 "><button class="btn btn-danger deleteChild float-right"
										type="button">삭제</button></th>
                                </tr>`;
			$(childTable).find('tbody').prepend(newEleTr);
			$(childTable).find('input[name]').setupValues(row);
			$(childTable).find('table').addClass('childTable');
			$itemTd.append(childTable);
		});
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
