$(function () {
	console.log('++ basic/sampleManage.js');

	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		$modal = $notModalViewAppliedEle.first();
	}
	let $modal_root = $modal.closest('.modal');

	let middleItemCodeSet = []; // 중분류정보
	let smallItemCodeSet = {}; // 소분류정보

	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log($modal.param);

		// 화면 내 중분류, 소분류 항목 초기화
		await $modal.initItemCodeSet();

		$modal.sampleListGrid = new Grid({
			el: document.querySelector('.sampleListGrid'),
			data: [], // TODO 데이터 바인딩은 추후
			columns: [
				{
					header: '중분류',
					name: 'middleInfo',
					align: 'center',
					width: 140,
					className: 'cursor_pointer',
					formatter: ({ row }) => {
						const code = row?.middleCodeNum ?? '';
						const name = row?.middleCodeName ?? '';
						return `${code}<br>${name}`.trim();
					},
				},
				{
					header: '소분류',
					name: 'smallInfo',
					align: 'center',
					width: 200,
					className: 'cursor_pointer',
					formatter: ({ row }) => {
						const code = row?.smallCodeNum ?? '';
						const name = row?.smallCodeName ?? '';
						return `${code}<br>${name}`.trim();
					},
				},
				{
					header: '기기명',
					name: 'equipName',
					align: 'center',
					className: 'cursor_pointer',
				},
			],
			pageOptions: {
				// useClient: true,
				perPage: 20,
			},
			rowHeaders: ['checkbox'],
			minBodyHeight: 639,
			bodyHeight: 639,
			rowHeight: 'auto',
			scrollY: true,
		});

		$modal.sampleFileGrid = new Grid({
			el: document.querySelector('.sampleFileGrid'),
			data: [], // TODO 데이터 바인딩은 추후
			columns: [
				{
					header: '파일명',
					name: 'fileName',
					align: 'center',
					className: 'cursor_pointer',
				},
				{
					header: '등록자',
					name: 'createMember',
					align: 'center',
					width: 90,
				},
				{
					header: '등록일시',
					name: 'createDateTime',
					align: 'center',
					width: 140,
				},
				{
					header: '-',
					align: 'center',
					width: 60,
					formatter: function () {
						return `
							<button type='button' class='btn btn-danger w-100 h-100 rounded-0' ><i
                                    class="bi bi-trash"></i></button>
					`;
					},
				},
			],
			pageOptions: {
				useClient: true,
				perPage: 10,
			},
			bodyHeight: 420,
			minBodyHeight: 420,
			rowHeight: 'auto',
			scrollY: true,
		});
	};

	// 중분류, 소분류 코드 초기화
	$modal.initItemCodeSet = async () => {
		try {
			const resGetItemCodeSet = await gAjax(
				'/api/basic/getItemCodeInfos',
				{},
				{
					type: 'GET',
				},
			);
			if (resGetItemCodeSet?.code > 0) {
				const itemCodeSet = resGetItemCodeSet.data;
				if (itemCodeSet.middleCodeInfos) {
					middleItemCodeSet = itemCodeSet.middleCodeInfos;
					// 반복문으로 세팅
					const $middleCodeSelect = $('.middleCodeSelect', $modal);
					$.each(middleItemCodeSet, function (index, row) {
						const option = new Option(`${row.codeNum} ${row.codeName}`, row.id);
						$middleCodeSelect.append(option);
					});
				}
				if (itemCodeSet.smallCodeInfos) {
					smallItemCodeSet = itemCodeSet.smallCodeInfos;
				}
			} else {
				throw new Error('분류코드 정보 가져오기 실패');
			}
		} catch (error) {
			console.log('catch!!');
			console.log(error);
			errorHandler(error);
		}
	};

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
			initPage($modal);
		}
	}
});
