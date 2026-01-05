$(function () {
	console.log('++ basic/itemModify.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	$modal = $candidates.first();
	let $modal_root = $modal.closest('.modal');

	let itemId;
	$modal.init_modal = async (param) => {
		$modal.param = param;

		// 업체id로 초기화 하기(수정)
		if ($modal.param?.id > 0) {
			// 옵셔널체이닝으로 체크
			itemId = Number($modal.param.id);

			// 데이터를 가져와서 세팅한다.
			try {
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
			}
		}

		// 교정수수로 데이터 세팅
		$modal.dataSource = {
			api: {
				readData: {
					url: '/api/item/getItemFeeHistory',
					serializer: (grid_param) => {
						grid_param.itemId = itemId;
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 업체 담당자 그리드
		$modal.grid = new Grid({
			el: document.querySelector('.agentManagerGrid'),
			columns: [
				{
					header: '기준일자',
					name: 'baseDate',
					className: 'cursor_pointer',
					editor: 'text',
					width: '150',
					align: 'center',
				},
				{
					header: '교정수수료',
					name: 'baseFee',
					editor: number_format_editor,
					width: '150',
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
			// data: $modal.dataSource,
		});

	};


	// 모달 내 이벤트 정의
	$modal;

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장클릭!!');
		// 업체정보 & 담당자 정보 유효성 체크 후, formdata에 데이터 담기
		const $form = $('.itemModifyForm', $modal);
		const formData = $form.serialize_object();
		$modal.grid.blur();




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