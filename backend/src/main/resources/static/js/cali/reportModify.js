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
			method: "GET"
			// header, body 모두 생략
		}
		try {
			const resReportInfo = await fetch(`/api/report/getReportInfo?id=${id}`, feOptions);
			if (resReportInfo.ok) {
				const reportInfoJson = await resReportInfo.json();
				console.log("🚀 ~ reportInfoJson:", reportInfoJson);
				if (reportInfoJson?.code > 0) {
					const reportInfo = reportInfoJson.data;
					console.log("🚀 ~ reportInfo:", reportInfo)
					const parentInfo = reportInfo.reportInfo;
					console.log("🚀 ~ parentInfo:", parentInfo)
					const childInfos = reportInfo.childReportInfos ?? {};	// 없을 수도 있음
					console.log("🚀 ~ childInfos:", childInfos)

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
	};
	// 모달 내 이벤트 정의
	// $modal;

	// 저장
	$modal.confirm_modal = async function (e) {};

	// 리턴 모달 이벤트
	$modal.return_modal = async function (e) {
		$modal.param.res = true;
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
			init_page($modal);
		}
	}
});
