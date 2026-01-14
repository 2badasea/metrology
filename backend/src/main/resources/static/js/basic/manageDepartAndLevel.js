$(function () {
	console.log('++ basic/manageDepartAndLevel.js');

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

	$modal.init_modal = (param) => {
		$modal.param = param;

		// 부서정보 가져오기
		$modal.departmentDataSource = {
			api: {
				readData: {
					url: '/api/basic/getDepartmentList',
					serializer: (grid_param) => {
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.middleGrid = new Grid({
			el: document.querySelector('.departmentGrid'),
			columns: [
				{
					header: '부서명',
					name: 'name',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '표시순서',
					name: 'seq',
					className: 'cursor_pointer',
					align: 'center',
                    width: '150',
				},
			],
			// pageOptions: {
			// 	perPage: 15,
			// },
			minBodyHeight: 680,
			bodyHeight: 680,
			rowHeaders: ['checkbox'],
			data: $modal.departmentDataSource,
			rowHeight: 'auto',
			draggable: true, // 드래그 허용
		});

		// 직급 정보 가져오기
		$modal.levelDataSource = {
			api: {
				readData: {
					url: '/api/basic/getMemberLevelList',
					serializer: (grid_param) => {
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.smallGrid = new Grid({
			el: document.querySelector('.levelGrid'),
			columns: [
				{
					header: '직급명',
					name: 'codeNum',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '표시순서',
					name: 'codeName',
					className: 'cursor_pointer',
					align: 'center',
                    width: '150',
				},
			],
			// pageOptions: {
			// 	perPage: 15,
			// },
			minBodyHeight: 680,
			bodyHeight: 680,
			data: $modal.levelDataSource,
			rowHeaders: ['checkbox'],
			rowHeight: 'auto',
			draggable: true, // 드래그 허용
		});
	}; // End init_modal

	// 페이지 내 이벤트
	$modal
		// 저장 클릭
		.on('click', '.saveItemCode', async function () {})
		// 삭제 
        // NOTE 삭제 시 알림주기 (기존에 해당 직급을 가지고 있는 직원들은 정보가 사라지기 때문에 새롭게 등록해야 한다고)
		.on('click', '.deleteItemCode', async function () {
			const gUserAuth = $('.gLoginAuth').val();
			if (gUserAuth !== 'admin') {
				g_toast('권한이 없습니다', 'warning');
				return false;
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
