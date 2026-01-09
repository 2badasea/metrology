$(function () {
	console.log('++ equipment/equipmentModify.js');

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

	let equipmentId;
	let fieldOptions = [];

	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);
		fieldOptions = $modal.param.fieldOptions ?? [];

		// 분야 세팅
		await $modal.setFieldCode();

		// 수정인 경우 데이터 세팅
		if ($modal.param?.id > 0) {
			equipmentId = Number($modal.param.id);
		}
	};

	// 모달 내 이벤트 정의
	$modal
		// 이미지 변경
		.on('change', 'input[name=equipmentImgFile]', function (e) {
			console.log('이미지 변경 감지');
			const files = e.target.files;
			if (files.length > 0) {
				let file = files[0]; // 첫 번째 파일 정보
				let fileType = file.type; // 파일 타입 (확장자)

				if (!fileType.startsWith('image/')) {
					g_toast('이미지 파일만 업로드 가능합니다.', 'warning');
					files.val('');
					return false;
				}
				const $imgBox = $('.equipmentImg', $modal);
				console.log('🚀 ~ $imgBox:', $imgBox);
				let objectUrl = URL.createObjectURL(file);
				$imgBox.attr('src', objectUrl).style('display', 'block');
				//
			} else {
				return false;
			}
		})
		// 이미지 삭제
		.on('click', '.deleteImgFile', async function () {});

	// 분야 세팅
	$modal.setFieldCode = async () => {
		const $fieldSelect = $('.equipmentFieldSelect', $modal);
		if (fieldOptions.length > 0) {
			fieldOptions.forEach((obj) => {
				const option = new Option(obj.name, obj.id);
				$fieldSelect.append(option);
			});
		}
	};

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장진행');
		// const $form = $('.caliOrderModifyForm', $modal);
		// const orderData = $form.serialize_object();
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
