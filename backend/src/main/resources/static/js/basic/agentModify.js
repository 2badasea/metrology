$(function () {
	console.log('++ basic/agentModify.js');

	// 1) 아직 modal-view-applied 안 된 애들 중에서
	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	// 2) 모달 안에서 뜨는 경우: .modal-body.modal-view 우선 선택
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $notModalViewAppliedEle.first();
	}
	// let $modal = $('.modal-view:not(.modal-view-applied)');
	let $modal_root = $modal.closest('.modal');

	let agentId = 0;
	let originAgentNum = '';

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		// 업체id로 초기화 하기(수정)
		if ($modal.param?.id > 0) {
			// 옵셔널체이닝으로 체크
			agentId = Number($modal.param.id);

			// g_ajax로 값 세팅
			// NOTE async, await으로도 가능한지 확인
			g_ajax(
				'/api/basic/getAgentInfo',
				{
					id: agentId,
				},
				{
					success: function (data) {
						if (data) {
							$modal.find('form.agentModifyForm input[name], textarea[name]').setupValues(data);
							// flag, type에 대해서도 세팅할 것
							// 폐업구분
							if (data.isClose == 'y') {
								$('.isClose', $modal).prop('checked', true);
							}
							// 업체형태에 대한 checkbox 설정
							if (data.agentFlag > 0) {
								// 반복문을 돌면서 세팅
								let chkBitInput = $('.agentFlagTypes', $modal).find('.chkBit');
								setCheckBit(chkBitInput, data.agentFlag);
							}
							// 사업자번호 존재 시, 기본적으로 중복체크 한 것으로 설정 (값 & 색상 부여)
							if (data.agentNum) {
								originAgentNum = data.agentNum;
								$('button.chkAgentNum', $modal).val('y').removeClass('btn-secondary').addClass('btn-success');
							}
						}
					},
					error: function (xhr) {
						custom_ajax_handler(xhr);
					},
					complete: function () {
						console.log('업체정보 데이터 세팅 complete');
					},
				}
			);
		}

		// 담당자 리스트 그리드 세팅
	};

	/**
	 * 사업자번호 키업 이벤트 핸들러
	 * debounce 자체는 처음 스크립트가 로드될 때(셋업 시점) 1번 실행 -> handler 생성
	 * handler(...) : keyup 발생할 때마다 실행 -> clearTimeout / setTimeout 동작
	 *
	 * @param   {[type]}  function  [function description]
	 *
	 * @return  {[type]}            [return description]
	 */
	$modal.agentNumKeyupHandler = debounce(function () {
		const agentNumVal = $(this).val();
		// 수정
		if (agentId > 0) {
			$modal.setCheckState(originAgentNum === agentNumVal);
		}
		// 등록
		else {
			$modal.setCheckState(false);
		}
	}, 250);

	$modal.setCheckState = (flag) => {
		const $btn = $('button.chkAgentNum', $modal);

		if (flag) {
			$btn.val('y').addClass('btn-success').removeClass('btn-secondary');
		} else {
			$btn.val('n').addClass('btn-secondary').removeClass('btn-success');
		}
	};

	// 모달 내 이벤트 정의
	$modal
		// 사업자번호 항목 입력할 때마다 keyup 이벤트 호출
		.on('keyup', 'input[name=agentNum]', function (e) {
			// 엔터키 -> 중복체크
			if (e.key === 'Enter' || e.keyCode === 13) {
				$('button.chkAgentNum', $modal).trigger('click'); // 중복확인 요청
				return false;
			} else {
				$modal.agentNumKeyupHandler.call(this, e);
			}
		})
		// 중복체크 진행
		.on('click', 'button.chkAgentNum', async function () {
			const $btn = $(this);
			const agentNumVal = $('input[name=agentNum]', $modal).val().trim();
			// 업체수정 & 수정된 부분이 없는 경우 return
			if (agentNumVal == originAgentNum) {
				g_toast('동일한 사업자번호 입니다.', 'warning');
				return false;
			}

			// 값이 없거나 하이픈(-) 포함해서 12자리가 아닌 경우 return false;
			if (!check_input(agentNumVal) || agentNumVal.length != 12) {
				g_toast('사업자번호 형식이 올바르지 않습니다', 'warning');
				return false;
			}

			$btn.prop('disabled', true); // 버튼 비활성화 처리

			try {
				g_loading_message(); // 로딩창 호출

				// api 호출
				const resChkAgentNum = await g_ajax('/api/member/chkDuplicateLoginId', {
					loginId: agentNumVal,
					refPage: 'agentModify',
				});

				Swal.close();	// sweet alert창 있을 경우 닫아버리기

				if (resChkAgentNum?.code == 1) {
					await g_message('중복체크', '등록가능한 사업자번호 입니다.', 'success');
					$btn.val('y').addClass('btn-success').removeClass('btn-secondary');
				} else {
					await g_message('중복체크', '이미 등록된 사업자번호 입니다.', 'warning');
					$btn.val('n').addClass('btn-secondary').removeClass('btn-success');
				}

			} catch (err) {
				Swal.close();	// sweet alert창 있을 경우 닫아버리기
				// 에러처리
				custom_ajax_handler(err);
			} finally {
				$btn.prop('disabled', false);
			}
		});

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장클릭!!');

		// agentflag값 확인
		const $chkBitInputs = $('.agentFlagTypes', $modal).find('.chkBit');
		let agentFlag = getCheckBit($chkBitInputs);
		console.log('값확인');
		console.log(agentFlag);

		return false;
	};

	// 담당자 그리드 초기화

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인 경우 바로 init_modal() 호출
		const p = $modal.data('param') || {};
		$modal.init_modal(p);
		if (typeof $modal.grid == 'object') {
			$modal.grid.refreshLayout();
		}
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});

// TODO 추후 아래 두 함수에 대해선 공통요소(common.js)로 분리시킬 것
// 2진수 단위로 값이 세팅되어 있는 요소들에 대해 값을 세팅하는 함수
function setCheckBit($ele, bitValue) {
	// & 대상 input의 value값을 기준으로 & 비트연산을 통해 값이 포함되면 checked 설정을 준다.
	$.each($ele, function (index, ele) {
		let originValue = $(ele).val();
		if (bitValue & originValue) {
			$(ele).prop('checked', true);
		}
	});
}

// 2진수 단위로 세팅되어 있는 요소들의 값의 합
function getCheckBit($ele) {
	let totalBitNum = 0;
	$.each($ele, function (index, ele) {
		if ($(ele).is(':checked')) {
			totalBitNum += Number($(ele).val());
		}
	});

	return totalBitNum;
}
