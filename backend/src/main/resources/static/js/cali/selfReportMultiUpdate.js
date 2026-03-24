$(function () {
	console.log('++ cali/selfReportMultiUpdate.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	// gModal은 .modal-body에 param을 저장하므로 .modal-body를 우선 선택
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	let reportIds = [];          // 대상 성적서 id 목록
	let smallItemCodeSet = {};   // 소분류 데이터 (middleItemCodeId → 배열)
	let memberLoadTimer = null;  // 실무자/기술책임자 debounce 타이머

	// =====================================================================
	// init_modal: 모달 파라미터 수신 후 초기화
	// param: { reportIds: [...] }
	// =====================================================================
	$modal.init_modal = async (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		reportIds = param.reportIds ?? [];

		// 1. 중/소분류 코드 초기화
		await $modal.initItemCodeSet();

		// 2. 전체 실무자/기술책임자 로드 (중분류 미선택 상태)
		await $modal.loadAllRoleMembers();

		// 3. 표준장비 그리드 초기화 (빈 상태로 시작)
		$modal.grid = gGrid('.equipageList', {
			columns: [
				{ name: 'equipmentId', hidden: true },
				{ header: '관리번호', name: 'manageNo',   className: 'cursor_pointer', align: 'center' },
				{ header: '장비명',   name: 'name',       className: 'cursor_pointer', align: 'center' },
				{ header: '제작회사', name: 'makeAgent',  className: 'cursor_pointer', align: 'center' },
				{ header: '모델명',   name: 'modelName',  className: 'cursor_pointer', align: 'center' },
				{ header: '기기번호', name: 'serialNo',   className: 'cursor_pointer', align: 'center' },
			],
			pageOptions: { perPage: 10 },
			minBodyHeight: 150,
			bodyHeight: 150,
			draggable: true,
		});

		if (typeof $modal.grid == 'object') {
			$modal.grid.refreshLayout();
		}
	};

	// =====================================================================
	// 중/소분류 코드 데이터 로드 및 중분류 select 초기화
	// =====================================================================
	$modal.initItemCodeSet = async () => {
		try {
			const res = await gAjax('/api/basic/getItemCodeInfos', {}, { type: 'GET' });
			if (res?.code > 0) {
				const itemCodeSet = res.data;
				if (itemCodeSet.middleCodeInfos) {
					const $middleSelect = $('.middleCodeSelect', $modal);
					$.each(itemCodeSet.middleCodeInfos, function (_, row) {
						$middleSelect.append(new Option(`${row.codeNum} ${row.codeName}`, row.id));
					});
				}
				if (itemCodeSet.smallCodeInfos) {
					smallItemCodeSet = itemCodeSet.smallCodeInfos;
				}
			} else {
				throw new Error('/api/basic/getItemCodeInfos 호출 실패');
			}
		} catch (xhr) {
			await gApiErrorHandler(xhr);
		}
	};

	// =====================================================================
	// 전체 실무자/기술책임자 로드 (중분류 미선택 시 호출)
	// /api/basic/getAllRoleMembers: 중분류 제한 없이 권한 보유 전체 직원 반환
	// =====================================================================
	$modal.loadAllRoleMembers = async () => {
		const $workSelect     = $('.workMemberId',     $modal);
		const $approvalSelect = $('.approvalMemberId', $modal);
		$workSelect.find('option:not(:first)').remove().end().val('0');
		$approvalSelect.find('option:not(:first)').remove().end().val('0');

		try {
			const res = await gAjax('/api/basic/getAllRoleMembers', {}, { type: 'GET' });
			if (res?.code > 0) {
				const { workers, approvers } = res.data;
				workers.forEach(m  => $workSelect.append(new Option(m.name, m.id)));
				approvers.forEach(m => $approvalSelect.append(new Option(m.name, m.id)));
			}
		} catch (xhr) {
			await gApiErrorHandler(xhr);
		}
	};

	// =====================================================================
	// 중분류 기준 실무자/기술책임자 로드
	// middleItemCodeId가 0/null이면 전체 목록으로 복원
	// =====================================================================
	$modal.loadMemberOptions = async (middleItemCodeId) => {
		const $workSelect     = $('.workMemberId',     $modal);
		const $approvalSelect = $('.approvalMemberId', $modal);
		$workSelect.find('option:not(:first)').remove().end().val('0');
		$approvalSelect.find('option:not(:first)').remove().end().val('0');

		if (!middleItemCodeId || middleItemCodeId == '0') {
			// 중분류 해제 → 전체 목록으로 복원
			await $modal.loadAllRoleMembers();
			return;
		}

		try {
			const res = await gAjax('/api/basic/getMembersByMiddleCode', { middleItemCodeId }, { type: 'GET' });
			if (res?.code > 0) {
				const { workers, approvers } = res.data;
				workers.forEach(m  => $workSelect.append(new Option(m.name, m.id)));
				approvers.forEach(m => $approvalSelect.append(new Option(m.name, m.id)));
			}
		} catch (xhr) {
			await gApiErrorHandler(xhr);
		}
	};

	// =====================================================================
	// 중분류 변경 시 실무자/기술책임자 debounce 재로드 (reportModify.js 패턴 동일)
	// - option 즉시 초기화 → 로딩 표시 → 700ms 후 API 호출
	// =====================================================================
	$modal.reloadMemberOptionsWithDelay = (middleItemCodeId) => {
		if (memberLoadTimer) {
			clearTimeout(memberLoadTimer);
			memberLoadTimer = null;
		}

		const $workSelect     = $('.workMemberId',     $modal);
		const $approvalSelect = $('.approvalMemberId', $modal);
		// 즉시 초기화 (사용자 피드백)
		$workSelect.find('option:not(:first)').remove().end().val('0');
		$approvalSelect.find('option:not(:first)').remove().end().val('0');

		gLoadingMessage('실무자/기술책임자 항목이 초기화됩니다.');

		memberLoadTimer = setTimeout(async () => {
			try {
				await $modal.loadMemberOptions(middleItemCodeId);
			} finally {
				swal.close();
				gToast('실무자/기술책임자 항목이 업데이트되었습니다.', 'info');
			}
		}, 700);
	};

	// =====================================================================
	// 이벤트 바인딩
	// =====================================================================
	$modal
		// 중분류 변경 → 소분류 옵션 갱신 + 실무자/기술책임자 재로드
		.on('change', '.middleCodeSelect', function () {
			const middleCodeId    = $(this).val();
			const $smallSelect    = $('.smallCodeSelect', $modal);

			// 소분류 옵션 갱신
			$smallSelect.find('option:not(:first)').remove().end().val('0');
			if (middleCodeId && middleCodeId != '0') {
				const codes = smallItemCodeSet[middleCodeId];
				if (codes && codes.length > 0) {
					codes.forEach(row => $smallSelect.append(new Option(row.codeNum, row.id)));
				}
			}

			// 실무자/기술책임자 초기화 + debounce 재로드
			$modal.reloadMemberOptionsWithDelay(middleCodeId);
		})
		// 실무자/기술책임자 체크박스 토글 → memberRow 표시/숨김
		.on('change', '#updateMemberInfoCheck', function () {
			const checked = $(this).is(':checked');
			$('.memberRow', $modal).toggle(checked);
		})
		// 표준장비 조회 모달 호출
		.on('click', '.searchEquipage', async function () {
			// 현재 그리드 데이터를 이미 선택된 장비로 전달 (중복 필터링)
			const equipDatas = $modal.grid.getData();
			equipDatas.forEach(row => { row.id = row.equipmentId; });

			const resModal = await gModal(
				'/equipment/searchEquipmentList',
				{ equipDatas },
				{
					title: '표준장비 조회',
					size: 'xxxl',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '선택',
				}
			);

			if (resModal && resModal.jsonData && resModal.jsonData.length > 0) {
				$modal.grid.clear();
				resModal.jsonData.forEach(row => {
					$modal.grid.appendRow({
						equipmentId: row.id,
						name:        row.name,
						manageNo:    row.manageNo,
						serialNo:    row.serialNo,
						makeAgent:   row.makeAgent,
						modelName:   row.modelName,
					});
				});
			}
		});

	// =====================================================================
	// confirm_modal: 저장 버튼 클릭 시 호출 (gModal 규약)
	// =====================================================================
	$modal.confirm_modal = async function () {
		const $btn = $('button.btn_save', $modal_root);

		// 대상 성적서 확인
		if (!reportIds || reportIds.length === 0) {
			gToast('대상 성적서 정보가 없습니다.', 'warning');
			return false;
		}

		// 환경정보 수집: 값이 입력된 항목만 포함 (빈 항목 제외)
		// → 서버에서 기존 JSON과 merge하여 입력된 항목만 덮어씀
		const environmentInfo = {};
		let hasEnvData = false;
		$('.environmentInfo', $modal).each((_, input) => {
			const key   = $(input).attr('name');
			const value = $(input).val();
			if (value) {
				// 값이 있는 항목만 포함하여 미입력 항목이 null/빈 값으로 초기화되는 것을 방지
				environmentInfo[key] = value;
				hasEnvData = true;
			}
		});

		// 실무자/기술책임자 업데이트 여부
		const updateMemberInfo = $('#updateMemberInfoCheck', $modal).is(':checked');

		// 표준장비 id 목록 (값이 있을 때만 서버 전달)
		const equipmentIds = $modal.grid.getData().map(row => row.equipmentId);

		const saveData = {
			reportIds,
			expectCompleteDate: $('input[name=expectCompleteDate]', $modal).val() || null,
			caliDate:           $('input[name=caliDate]',           $modal).val() || null,
			environmentInfo:    hasEnvData ? JSON.stringify(environmentInfo) : null,
			middleItemCodeId:   Number($('.middleCodeSelect',  $modal).val()) || null,
			smallItemCodeId:    Number($('.smallCodeSelect',   $modal).val()) || null,
			updateMemberInfo,
			// 체크박스 미선택 시 null로 전달하여 서버에서 무시하도록 함
			workMemberId:     updateMemberInfo ? (Number($('.workMemberId',     $modal).val()) || null) : null,
			approvalMemberId: updateMemberInfo ? (Number($('.approvalMemberId', $modal).val()) || null) : null,
			equipmentIds:     equipmentIds.length > 0 ? equipmentIds : null,
		};

		const confirmResult = await gMessage(
			'통합수정',
			`선택된 성적서 <b>${reportIds.length}건</b>을 수정하시겠습니까?`,
			'question',
			'confirm'
		);
		if (!confirmResult.isConfirmed) return false;

		try {
			$btn.prop('disabled', true);
			gLoadingMessage();

			const res = await fetch('/api/report/selfReportMultiUpdate', {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json; charset=utf-8' },
				body: JSON.stringify(saveData),
			});
			swal.close();

			if (!res.ok) throw res;
			const resData = await res.json();

			if (resData?.code > 0) {
				await gMessage('통합수정', resData.msg ?? '수정되었습니다.', 'success', 'alert');
				$modal_root.modal('hide');
				return true;
			} else {
				await gMessage('통합수정', resData.msg ?? '수정에 실패했습니다.', 'error', 'alert');
			}
		} catch (err) {
			swal.close();
			await gApiErrorHandler(err);
		} finally {
			$btn.prop('disabled', false);
		}

		return false;
	};

	// =====================================================================
	// 페이지 마운트 처리 (common.js 규약)
	// =====================================================================
	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		// gModal이 .modal-body.data('param')에 저장한 param을 읽어 init_modal 호출
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
			initPage($modal);
		}
	}
});
