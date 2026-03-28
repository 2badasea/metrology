$(function () {
	console.log('++ dev/workModify.js');

	// ── $modal / $modal_root 설정 (표준 패턴) ──────────────────────────
	const $candidates    = $('.modal-view:not(.modal-view-applied)');
	let   $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	// ── 모듈 레벨 상태 변수 ────────────────────────────────────────────
	let editor      = null;       // Toast UI Editor 인스턴스 (작성 패널)
	let viewer      = null;       // Toast UI Viewer 인스턴스 (상세 패널)
	let attachFiles = [];         // 작성 패널에서 선택된 첨부파일 목록
	let activeTab        = 'inquiry';  // 현재 활성 탭 ('notice' | 'inquiry')
	let currentWorkId    = null;       // 현재 상세 보기 중인 문의 ID
	let workGrid         = null;       // 문의 목록 Toast UI Grid 인스턴스
	let noticeGrid       = null;       // 공지사항 목록 Toast UI Grid 인스턴스
	let pendingNoticeData = null;      // 공지 탭 최초 활성화 전까지 데이터 임시 보관

	const MAX_FILES     = 10;
	const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

	// 유형/상태/중요도 한글 레이블
	const CATEGORY_LABEL = { BUG: '버그', ERROR: '오류', NEW_FEATURE: '기능 요청', INQUIRY: '문의' };
	const STATUS_LABEL   = { READY: '접수', IN_PROGRESS: '처리중', ON_HOLD: '보류', DONE: '완료', CANCELLED: '취소' };
	const STATUS_CLASS   = {
		READY: 'badge-secondary', IN_PROGRESS: 'badge-primary',
		ON_HOLD: 'badge-warning', DONE: 'badge-success', CANCELLED: 'badge-dark',
	};
	const PRIORITY_LABEL = { NORMAL: '일반', EMERGENCY: '긴급' };
	const PRIORITY_CLASS = { NORMAL: 'badge-info', EMERGENCY: 'badge-danger' };

	// ── Toast UI Editor 동적 로드 (vendor 로컬 파일) ──────────────────
	// config.html 공통 블록에 포함하지 않고 이 모달 최초 오픈 시 1회만 로드
	function loadEditorLibrary() {
		return new Promise((resolve) => {
			if (window.toastui && window.toastui.Editor) { resolve(); return; }
			const link  = document.createElement('link');
			link.rel    = 'stylesheet';
			link.href   = '/vendor/tui-editor/toastui-editor.min.css';
			document.head.appendChild(link);
			const script  = document.createElement('script');
			script.src    = '/vendor/tui-editor/toastui-editor-all.min.js';
			script.onload = resolve;
			document.head.appendChild(script);
		});
	}

	// ── 탭 전환 ────────────────────────────────────────────────────────
	// 작성 패널 진행 중 탭 이동 시 dirty 체크 포함
	$modal.switchTab = async function (tabName) {
		if (activeTab === tabName) return;

		// 작성 패널이 열려 있고 내용이 있으면 확인
		const isWriteOpen = $('.panel-write', $modal).not('.d-none').length > 0;
		if (isWriteOpen) {
			const hasTitle   = $('input[name=title]', $modal).val().trim().length > 0;
			const hasContent = editor && editor.getMarkdown().trim().length > 0;
			if (hasTitle || hasContent) {
				const confirmed = await gMessage(
					'작성 중인 내용이 있습니다. 이동하시겠습니까?', '', 'warning', 'confirm'
				);
				if (!confirmed.isConfirmed) return;
			}
			// 문의 탭의 패널을 목록으로 되돌림
			$modal.showPanel('list');
		}

		activeTab = tabName;

		// 탭 버튼 active 상태 전환
		$('.work-tab-btn', $modal).removeClass('active');
		$(`.work-tab-btn[data-tab="${tabName}"]`, $modal).addClass('active');

		// 탭 컨텐츠 전환
		$('.work-tab-pane', $modal).addClass('d-none');
		$(`.work-tab-pane[data-pane="${tabName}"]`, $modal).removeClass('d-none');

		// 등록 버튼: 문의 탭의 작성 패널에서만 노출
		$modal_root.find('.modal-btn-confirm').hide();

		// 공지사항 탭 최초 활성화 시 그리드 초기화
		// (d-none 상태에서는 그리드가 크기를 측정할 수 없으므로 탭 전환 후 지연 실행)
		if (tabName === 'notice' && !noticeGrid) {
			setTimeout(() => { $modal.initNoticeGrid(); }, 50);
		}
	};

	// ── 문의 탭 내 패널 전환 ──────────────────────────────────────────
	// panelName: 'list' | 'write' | 'detail'
	$modal.showPanel = function (panelName) {
		$('.work-panel', $modal).addClass('d-none');
		$(`.panel-${panelName}`, $modal).removeClass('d-none');

		// 등록 버튼: 작성 패널에서만 노출
		const $confirmBtn = $modal_root.find('.modal-btn-confirm');
		if (panelName === 'write') {
			$confirmBtn.show();
		} else {
			$confirmBtn.hide();
		}
	};

	// ── 문의 목록 API 조회 및 Toast Grid 렌더링 ─────────────────────
	$modal.loadWorkList = async function () {
		try {
			const res   = await fetch('/api/dev/works');
			if (!res.ok) throw res;
			const json  = await res.json();
			const works = json.code === 1 ? (json.data ?? []) : [];

			// 그리드용 데이터 변환 (한글 레이블 파생 필드 추가)
			const gridData = works.map((w, idx) => ({
				...w,
				seq:      idx + 1,
				catLabel: CATEGORY_LABEL[w.category]        ?? w.category        ?? '',
				priLabel: PRIORITY_LABEL[w.priorityByAgent] ?? w.priorityByAgent ?? '',
				stLabel:  STATUS_LABEL[w.workStatus]        ?? w.workStatus       ?? '',
				dateStr:  w.createdAt ? w.createdAt.substring(0, 10) : '',
			}));

			if (!workGrid) {
				// 최초 초기화
				workGrid = gGrid($('.workListGrid', $modal)[0], {
					columns: [
						{ header: '번호',   name: 'seq',      width: 50,  align: 'center' },
						{ header: '유형',   name: 'catLabel', width: 80,  align: 'center' },
						{ header: '제목',   name: 'title',    minWidth: 150 },
						{ header: '중요도', name: 'priLabel', width: 70,  align: 'center' },
						{ header: '상태',   name: 'stLabel',  width: 80,  align: 'center' },
						{ header: '등록일', name: 'dateStr',  width: 90,  align: 'center' },
					],
					bodyHeight:      300,
					pageOptions:     false,
					showConfigButton: false,
					data: gridData,
				});

				// 행 클릭 → 상세 API 조회
				workGrid.on('click', async ({ rowKey }) => {
					const row = workGrid.getRow(rowKey);
					if (!row || !row.id) return;
					try {
						const detailRes  = await fetch(`/api/dev/works/${row.id}`);
						if (!detailRes.ok) throw detailRes;
						const detailJson = await detailRes.json();
						if (detailJson.code !== 1) {
							gToast(detailJson.message ?? '상세 조회에 실패했습니다.', 'error');
							return;
						}
						currentWorkId = row.id;
						await $modal.renderDetail(detailJson.data);
						$modal.showPanel('detail');
						// WORK 타입 알림 읽음 처리 (fire-and-forget)
						fetch(`/api/alarms/readByRef?refType=WORK&refId=${row.id}`, { method: 'PATCH' }).catch(() => {});
					} catch (e) {
						gApiErrorHandler(e, { defaultMessage: '문의 상세를 불러오는 중 오류가 발생했습니다.' });
					}
				});
			} else {
				// 이후 호출 시 데이터만 교체
				workGrid.resetData(gridData);
			}
		} catch (e) {
			gApiErrorHandler(e, { defaultMessage: '문의 목록을 불러오는 중 오류가 발생했습니다.' });
		}
	};

	// ── 공지사항 API 조회 및 Toast Grid 렌더링 ──────────────────────
	$modal.loadNoticeList = async function () {
		try {
			const res     = await fetch('/api/dev/notices');
			if (!res.ok) throw res;
			const json    = await res.json();
			const notices = json.code === 1 ? (json.data ?? []) : [];

			// 미확인 카운트 계산 + 그리드용 데이터 변환
			let unreadCount = 0;
			const gridData  = notices.map((n, idx) => {
				if (!n.isChecked) unreadCount++;
				return {
					...n,
					seq:          idx + 1,
					dateStr:      n.createdAt ? n.createdAt.substring(0, 10) : '',
					checkedLabel: n.isChecked ? '확인' : '미확인',
				};
			});

			// 미확인 공지 뱃지 업데이트
			const $badge = $('.notice-badge', $modal);
			if (unreadCount > 0) {
				$badge.text(unreadCount).removeClass('d-none');
			} else {
				$badge.addClass('d-none');
			}

			// 공지 탭이 아직 한 번도 활성화되지 않은 경우 데이터만 보관
			// (d-none 상태에서는 그리드 크기 측정 불가 → initNoticeGrid 시 사용)
			if (!noticeGrid) {
				pendingNoticeData = gridData;
				return;
			}
			noticeGrid.resetData(gridData);
		} catch (e) {
			gApiErrorHandler(e, { defaultMessage: '공지사항 목록을 불러오는 중 오류가 발생했습니다.' });
		}
	};

	// ── 공지사항 그리드 초기화 (탭 최초 활성화 시 1회 실행) ──────────
	$modal.initNoticeGrid = function () {
		if (noticeGrid) return;
		noticeGrid = gGrid($('.noticeListGrid', $modal)[0], {
			columns: [
				{ header: '번호',   name: 'seq',          width: 50,  align: 'center' },
				{ header: '제목',   name: 'title',         minWidth: 200 },
				{ header: '등록일', name: 'dateStr',       width: 90,  align: 'center' },
				{ header: '확인',   name: 'checkedLabel',  width: 60,  align: 'center' },
			],
			bodyHeight:      300,
			pageOptions:     false,
			showConfigButton: false,
			data: pendingNoticeData ?? [],
		});
		pendingNoticeData = null;
	};

	// ── 작성 패널 초기화 ───────────────────────────────────────────────
	$modal.initWritePanel = async function () {
		// 폼 초기화 (먼저 리셋 후 값 세팅)
		$('form.workWriteForm', $modal)[0]?.reset();
		attachFiles = [];
		$('.attach-file-list', $modal).empty();

		// 작성자/연락처 세팅 (부모 페이지 G_USER 전역변수 활용, reset() 이후에 세팅)
		if (typeof G_USER !== 'undefined') {
			$('.writerNameDisplay', $modal).val(G_USER.name || '');
			$('.writerTelDisplay',  $modal).val(G_USER.hp   || '');
		}

		// Toast UI Editor 초기화 (1회만 생성, 이후 재사용)
		if (!editor) {
			await loadEditorLibrary();
			editor = new toastui.Editor({
				el:              $modal.find('.workEditorEl')[0],
				height:          '280px',
				initialEditType: 'wysiwyg',
				previewStyle:    'vertical',
				placeholder:     '문의 내용을 입력하세요. 이미지는 붙여넣기 또는 드래그로 첨부할 수 있습니다.',
				toolbarItems: [
					['heading', 'bold', 'italic', 'strike'],
					['hr', 'quote'],
					['ul', 'ol'],
					['table', 'image', 'link'],
					['code', 'codeblock'],
				],
			});
		} else {
			editor.setMarkdown('');
		}
	};

	// ── 상세 패널 렌더링 ───────────────────────────────────────────────
	$modal.renderDetail = async function (workData) {
		$('.work-category-badge', $modal)
			.text(CATEGORY_LABEL[workData.category] ?? workData.category)
			.attr('class', 'badge badge-secondary work-category-badge');

		$('.work-priority-badge', $modal)
			.text(PRIORITY_LABEL[workData.priorityByAgent] ?? workData.priorityByAgent)
			.attr('class', `badge ${PRIORITY_CLASS[workData.priorityByAgent] ?? 'badge-info'} work-priority-badge`);

		$('.work-status-badge', $modal)
			.text(STATUS_LABEL[workData.workStatus] ?? workData.workStatus)
			.attr('class', `badge ${STATUS_CLASS[workData.workStatus] ?? 'badge-secondary'} work-status-badge`);

		$('.work-title-display',  $modal).text(workData.title ?? '');
		$('.work-date-display',   $modal).text(workData.createdAt ? workData.createdAt.substring(0, 10) : '');
		$('.work-writer-display', $modal).text(workData.createMemberName ?? '');

		// Toast UI Viewer (본문)
		if (!viewer) {
			await loadEditorLibrary();
			viewer = new toastui.Viewer({
				el:           $modal.find('.workViewerEl')[0],
				initialValue: workData.content ?? '',
			});
		} else {
			viewer.setMarkdown(workData.content ?? '');
		}

		// 첨부파일
		const attachList = workData.attachments ?? [];
		if (attachList.length > 0) {
			$('.work-attach-footer', $modal).removeClass('d-none');
			const $attachUl = $('.detail-attach-list', $modal).empty();
			attachList.forEach(f => {
				$attachUl.append(
					`<li><a href="#" class="text-primary" data-path="${f.storedPath}">
						<i class="bi bi-file-earmark"></i> ${$('<span>').text(f.originalName).html()}
					</a></li>`
				);
			});
		} else {
			$('.work-attach-footer', $modal).addClass('d-none');
		}

		$modal.renderComments(workData.comments ?? []);
	};

	// ── 댓글 목록 렌더링 ───────────────────────────────────────────────
	$modal.renderComments = function (comments) {
		const $wrap = $('.work-comments', $modal).empty();

		if (comments.length === 0) {
			$wrap.append(`<div class="text-center text-muted py-2" style="font-size:.85rem;">아직 댓글이 없습니다.</div>`);
			return;
		}

		comments.forEach(c => {
			const isDev   = c.authorType === 'DEV';
			const dateStr = c.createdAt ? c.createdAt.substring(0, 16).replace('T', ' ') : '';
			$wrap.append(`
				<div class="comment-item ${isDev ? 'comment-dev' : 'comment-agent'} mb-2">
					<div class="comment-meta d-flex align-items-center" style="gap:6px;font-size:.8rem;">
						<span class="badge ${isDev ? 'badge-dark' : 'badge-secondary'}">${isDev ? '개발팀' : '업체'}</span>
						<strong>${$('<span>').text(c.authorName).html()}</strong>
						<span class="text-muted">${dateStr}</span>
					</div>
					<div class="comment-body mt-1">${$('<span>').text(c.content).html()}</div>
				</div>`);
		});
	};

	// ── 문의 등록 처리 ────────────────────────────────────────────────
	$modal.submitWork = async function () {
		const $form    = $('form.workWriteForm', $modal);
		const category = $('select[name=category]', $form).val();
		const title    = $('input[name=title]',     $form).val().trim();
		const content  = editor ? editor.getMarkdown() : '';

		if (!category) { gToast('유형을 선택하세요.', 'warning');   return false; }
		if (!title)    { gToast('제목을 입력하세요.', 'warning');   return false; }
		if (!content.trim()) { gToast('내용을 입력하세요.', 'warning'); return false; }

		const confirmed = await gMessage('문의를 등록하시겠습니까?', '', 'question', 'confirm');
		if (!confirmed.isConfirmed) return false;

		try {
			const formData = new FormData();
			formData.append('category',        category);
			formData.append('priorityByAgent', $('select[name=priorityByAgent]', $form).val());
			formData.append('title',           title);
			formData.append('content',         content);
			attachFiles.forEach(f => formData.append('files', f));
			// Content-Type 헤더 지정 금지 — 브라우저가 boundary를 포함한 multipart/form-data 자동 설정
			const res  = await fetch('/api/dev/works', { method: 'POST', body: formData });
			if (!res.ok) throw res;
			const json = await res.json();
			if (json.code !== 1) {
				gToast(json.message ?? '등록에 실패했습니다.', 'error');
				return false;
			}
			gToast('문의가 등록되었습니다.', 'success');
			$modal.showPanel('list');
			await $modal.loadWorkList();
			return true;
		} catch (e) {
			gApiErrorHandler(e, { defaultMessage: '문의 등록 중 오류가 발생했습니다.' });
			return false;
		}
	};

	// ── 댓글 등록 처리 ────────────────────────────────────────────────
	$modal.submitComment = async function () {
		const content = $('.commentInput', $modal).val().trim();
		if (!content) { gToast('댓글 내용을 입력하세요.', 'warning'); return; }

		try {
			const res  = await fetch(`/api/dev/works/${currentWorkId}/comments`, {
				method:  'POST',
				headers: { 'Content-Type': 'application/json' },
				body:    JSON.stringify({ content }),
			});
			if (!res.ok) throw res;
			const json = await res.json();
			if (json.code !== 1) {
				gToast(json.message ?? '댓글 등록에 실패했습니다.', 'error');
				return;
			}
			// 댓글 입력창 초기화 후 상세 재조회하여 댓글 목록 갱신
			$('.commentInput', $modal).val('');
			const detailRes  = await fetch(`/api/dev/works/${currentWorkId}`);
			if (!detailRes.ok) throw detailRes;
			const detailJson = await detailRes.json();
			if (detailJson.code === 1) {
				$modal.renderComments(detailJson.data.comments ?? []);
			}
		} catch (e) {
			gApiErrorHandler(e, { defaultMessage: '댓글 등록 중 오류가 발생했습니다.' });
		}
	};

	// ── 첨부파일 선택 ────────────────────────────────────────────────
	$modal.on('change', '.attachInput', function () {
		Array.from(this.files).forEach(f => {
			if (attachFiles.some(ef => ef.name === f.name)) {
				gToast(`중복 파일: ${f.name}`, 'warning'); return;
			}
			if (f.size > MAX_FILE_SIZE) {
				gToast(`파일 크기 초과 (최대 20MB): ${f.name}`, 'warning'); return;
			}
			if (attachFiles.length >= MAX_FILES) {
				gToast(`첨부파일은 최대 ${MAX_FILES}개까지 가능합니다.`, 'warning'); return;
			}
			attachFiles.push(f);
		});
		$(this).val('');
		$modal.renderAttachList();
	});

	// ── 첨부파일 목록 UI 갱신 ─────────────────────────────────────────
	$modal.renderAttachList = function () {
		const $list = $('.attach-file-list', $modal).empty();
		attachFiles.forEach((f, idx) => {
			const sizeKb = (f.size / 1024).toFixed(1);
			$list.append(`
				<li class="d-flex align-items-center" style="gap:6px;font-size:.83rem;">
					<i class="bi bi-file-earmark text-secondary"></i>
					<span>${$('<span>').text(f.name).html()}</span>
					<span class="text-muted">(${sizeKb}KB)</span>
					<button type="button" class="btn btn-sm p-0 btnRemoveAttach text-danger"
						data-idx="${idx}" style="line-height:1;">
						<i class="bi bi-x-circle"></i>
					</button>
				</li>`);
		});
	};

	// ── 이벤트 바인딩 ─────────────────────────────────────────────────

	// 탭 버튼 클릭
	$modal.on('click', '.work-tab-btn', async function () {
		await $modal.switchTab($(this).data('tab'));
	});

	// 첨부파일 삭제
	$modal.on('click', '.btnRemoveAttach', function () {
		attachFiles.splice(Number($(this).data('idx')), 1);
		$modal.renderAttachList();
	});

	// 새 문의 작성 버튼
	$modal.on('click', '.btnGoWrite', async function () {
		await $modal.initWritePanel();
		$modal.showPanel('write');
	});

	// 목록으로 버튼
	$modal.on('click', '.btnBack', function () {
		$modal.showPanel('list');
	});

	// 댓글 등록 버튼
	$modal.on('click', '.btnAddComment', async function () {
		await $modal.submitComment();
	});

	// 문의 목록 행 클릭은 workGrid.on('click', ...) 에서 처리 (loadWorkList 내부)

	// ── init_modal ────────────────────────────────────────────────────
	$modal.init_modal = async function (param) {
		$modal.param = param;

		// 문의 목록 + 공지사항 병렬 로드
		// (공지사항은 d-none 탭이므로 데이터만 보관 → 탭 전환 시 그리드 초기화)
		$modal.loadWorkList();
		$modal.loadNoticeList();
		$modal.showPanel('list');

		// workId로 직접 진입 시 → API 조회 후 상세 패널로 이동
		const initWorkId = $('#initWorkId', $modal).val();
		if (initWorkId && initWorkId !== 'null') {
			try {
				const res  = await fetch(`/api/dev/works/${initWorkId}`);
				if (!res.ok) throw res;
				const json = await res.json();
				if (json.code === 1) {
					currentWorkId = Number(initWorkId);
					await $modal.renderDetail(json.data);
					$modal.showPanel('detail');
				}
			} catch (e) {
				gApiErrorHandler(e, { defaultMessage: '문의 상세를 불러오는 중 오류가 발생했습니다.' });
			}
		}
	};

	// confirm_modal — 푸터 등록 버튼
	$modal.confirm_modal = async function () {
		return await $modal.submitWork();
	};

	// ── 표준 패턴: modal-data 등록 + init_modal 자동 호출 ────────────
	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		const p = $modal.data('param') || {};
		$modal.init_modal(p);
	}
});
