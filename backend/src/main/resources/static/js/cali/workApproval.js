$(function () {
	console.log('++ cali/workApproval.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	$modal = $candidates.first();
	let $modal_root = $modal.closest('.modal');

	// 중/소분류 코드 세트 (init_modal에서 채워짐)
	let smallItemCodeSet = {};
	let middleItemCodeSet = [];

	// =====================================================================
	// 진행상태 label 변환
	// =====================================================================
	function reportStatusLabel(value) {
		const map = {
			WAIT: '대기',
			WORK_RETURN: '실무자반려',
			APPROV_RETURN: '기술책임자반려',
			REUPLOAD: '재업로드',
			REPAIR: '수리',
			IMPOSSIBLE: '불가',
			CANCEL: '취소',
			COMPLETE: '완료',
		};
		return map[value] ?? value ?? '';
	}

	// =====================================================================
	// 파일 검증 → 결재 확인창 → 처리 (현재: 구현 준비중)
	// gridClass.js의 UploadCellRenderer에서 호출
	// =====================================================================
	window.handleWorkApprovalUploadFile = async function (file, rowKey) {
		if (!file) return;

		// 엑셀 파일(.xlsx, .xls)만 허용
		const allowedExtensions = ['.xlsx', '.xls'];
		const fileName = file.name.toLowerCase();
		const isValidExt = allowedExtensions.some((ext) => fileName.endsWith(ext));
		if (!isValidExt) {
			gToast('엑셀 파일(.xlsx, .xls)만 업로드 가능합니다.', 'warning');
			return;
		}

		// 결재 확인창 (확인 버튼 텍스트: '결재')
		const result = await gMessage('결재', '선택한 성적서를 결재 처리하시겠습니까?', 'question', 'confirm', {
			confirmButtonText: '결재',
		});
		if (result.isConfirmed) {
			gToast('구현 준비중입니다.', 'info');
		}
	};

	// =====================================================================
	// 파일 선택 창 열기 (hidden input 활용)
	// gridClass.js의 UploadCellRenderer에서 호출
	// =====================================================================
	window.triggerWorkApprovalFileSelect = function (rowKey) {
		const $input = $('#uploadFileInput');
		// 동일 파일 재선택 시에도 change 이벤트가 발생하도록 value 초기화
		$input.val('');
		$input.off('change').on('change', function () {
			handleWorkApprovalUploadFile(this.files[0], rowKey);
		});
		$input.trigger('click');
	};

	// =====================================================================
	// init_modal: 중/소분류 코드 비동기 초기화 (initPage 또는 modal_ready에서 호출됨)
	// =====================================================================
	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		try {
			const resGetItemCodeSet = await gAjax(
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
					const $middleCodeSelect = $('.middleCodeSelect', $modal);
					$.each(itemCodeSet.middleCodeInfos, function (index, row) {
						const option = new Option(row.codeNum, row.id);
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
			customAjaxHandler(xhr);
		}
	};

	// =====================================================================
	// 그리드 데이터 소스 정의
	// =====================================================================
	$modal.dataSource = {
		api: {
			readData: {
				url: '/api/report/workApprovalList',
				method: 'GET',
				serializer: (grid_param) => {
					grid_param.reportStatus    = $('form.searchForm .reportStatusFilter', $modal).val() ?? '';
					grid_param.workStatus      = $('form.searchForm .workStatusFilter', $modal).val() ?? '';
					grid_param.orderType       = $('form.searchForm .orderTypeFilter', $modal).val() ?? '';
					grid_param.middleItemCodeId = Number($('form.searchForm .middleCodeSelect', $modal).val() ?? 0);
					grid_param.smallItemCodeId  = Number($('form.searchForm .smallCodeSelect', $modal).val() ?? 0);
					grid_param.searchType      = $('form.searchForm .searchType', $modal).val() ?? '';
					grid_param.keyword         = $('form.searchForm #keyword', $modal).val() ?? '';
					return $.param(grid_param);
				},
			},
		},
	};

	// =====================================================================
	// 그리드 정의
	// =====================================================================
	$modal.grid = gGrid('.workApprovalList', {
		columns: [
			{
				// 접수구분: ACCREDDIT=공인, UNACCREDDIT=비공인, TESTING=시험
				header: '구분',
				name: 'orderType',
				width: 60,
				align: 'center',
				formatter: function (data) {
					const map = { ACCREDDIT: '공인', UNACCREDDIT: '비공인', TESTING: '시험' };
					return map[data.value] ?? data.value ?? '';
				},
			},
			{
				header: '관리번호',
				name: 'manageNo',
				width: 100,
				align: 'center',
			},
			{
				header: '소분류',
				name: 'smallCodeNum',
				width: 65,
				align: 'center',
			},
			{
				header: '접수일',
				name: 'orderDate',
				width: 85,
				align: 'center',
			},
			{
				header: '완료예정일',
				name: 'expectCompleteDate',
				width: 85,
				align: 'center',
			},
			{
				header: '성적서번호',
				name: 'reportNum',
				width: 120,
				align: 'center',
			},
			{
				header: '신청업체',
				name: 'custAgent',
				width: 130,
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '성적서발행처',
				name: 'reportAgent',
				width: 130,
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '기기명',
				name: 'itemName',
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '기기번호',
				name: 'itemNum',
				width: 120,
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '제작회사',
				name: 'itemMakeAgent',
				width: 120,
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '형식',
				name: 'itemFormat',
				width: 120,
				align: 'center',
				whiteSpace: 'pre-line',
			},
			{
				header: '진행상태',
				name: 'reportStatus',
				width: 90,
				align: 'center',
				formatter: function (data) {
					return reportStatusLabel(data.value);
				},
			},
			{
				header: '작성자',
				name: 'writeMemberName',
				width: 80,
				align: 'center',
			},
			{
				header: '실무자',
				name: 'workMemberName',
				width: 80,
				align: 'center',
			},
			{
				header: '기술책임자',
				name: 'approvalMemberName',
				width: 90,
				align: 'center',
			},
			{
				// 업로드 컬럼: gridClass.js의 UploadCellRenderer 사용
				// 버튼 클릭 및 드래그앤드롭 이벤트는 렌더러 내부에서 window 함수로 위임
				header: '업로드',
				name: 'uploadBtn',
				width: 80,
				align: 'center',
				sortable: false,
				renderer: {
					type: UploadCellRenderer,
				},
			},
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 25,      // 기본 25건
		},
		rowHeaders: ['checkbox'],
		minBodyHeight: 600,
		bodyHeight: 600,
		rowHeight: 'auto',
		data: $modal.dataSource,
	});

	// =====================================================================
	// 페이지 이벤트 바인딩
	// =====================================================================
	$modal
		// 검색 폼 submit
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1);
		})
		// 행 수 변경
		.on('change', '.rowLeng', function () {
			const rowLeng = $(this).val();
			if (rowLeng > 0) {
				$modal.grid.setPerPage(rowLeng);
			}
		})
		// 중분류 변경 → 소분류 옵션 갱신
		.on('change', '.middleCodeSelect', function () {
			const middleCodeId = $(this).val();
			const $smallCodeSelect = $('.smallCodeSelect', $modal);
			$($smallCodeSelect).find('option').remove();
			$smallCodeSelect.append(new Option('소분류전체', ''));
			if (!middleCodeId) {
				$smallCodeSelect.val('');
			} else {
				if (smallItemCodeSet[middleCodeId] != undefined && smallItemCodeSet[middleCodeId].length > 0) {
					smallItemCodeSet[middleCodeId].forEach((row) => {
						$smallCodeSelect.append(new Option(row.codeNum, row.id));
					});
				}
			}
		})
		// 버튼: 성적서작성
		// 1) 체크된 항목이 없으면 warning
		// 2) 체크된 항목의 소분류가 모두 동일해야 함
		// 3) 모두 통과 시 준비중 안내 (향후 실제 기능으로 교체)
		.on('click', '.btnWriteReport', function () {
			const checkedRows = $modal.grid.getCheckedRows();
			if (!checkedRows || checkedRows.length === 0) {
				gToast('리스트에서 항목을 선택해 주세요.', 'warning');
				return;
			}
			// 소분류 동일성 체크
			const smallCodes = [...new Set(checkedRows.map((row) => row.smallCodeNum))];
			if (smallCodes.length > 1) {
				gToast('동일한 소분류 항목만 선택해 주세요.', 'warning');
				return;
			}
			gToast('구현 준비중입니다.', 'info');
		})
		// 버튼: 성적서대기변경 (준비중)
		.on('click', '.btnWaitChange', function () {
			gToast('구현 준비중입니다.', 'info');
		})
		// 버튼: 통합수정 (준비중)
		.on('click', '.btnBulkEdit', function () {
			gToast('구현 준비중입니다.', 'info');
		})
		// 버튼: 성적서 다중결재 (준비중)
		.on('click', '.btnMultiApproval', function () {
			gToast('구현 준비중입니다.', 'info');
		});

	// =====================================================================
	// 페이지 마운트 처리 (common.js 규약)
	// =====================================================================
	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		// 모달 팝업창인 경우
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
			initPage($modal);
		}
	}
});