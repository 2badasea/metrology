$(function () {
    console.log("++ basic/home.js");

    let $modal = $(".modal-view:not(.modal-view-applied)");
    let $modal_root = $modal.closest(".modal");

    $modal.init_modal = (param) => {
        console.log("🚀 ~ param:", param);
    };


    $modal.data("modal-data", $modal);
    $modal.addClass("modal-view-applied");
    if ($modal.hasClass("modal-body")) {
        //모달 팝업창인경우
        $modal_root.on("modal_ready", function (e, p) {
            $modal.init_modal(p);
            if (typeof $modal.grid == "object") {
                $modal.grid.refreshLayout();
            }
        });
    }

    if (typeof window.modal_deferred == "object") {
        window.modal_deferred.resolve("script end");
    } else {
        if (!$modal_root.length) {
            init_page($modal);
        }
    }    


});