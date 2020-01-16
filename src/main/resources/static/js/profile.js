$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		// 关注TA
		$.post(
			CONTEXT_PATH + "/follow",
			{"entityType":3, "entityId":$(btn).prev().val()}, // 该节点的前一个节点的值，即隐藏的<input>值，即当前用户id
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0){
					window.location.reload(); //更新页面
				} else {
					alert(data.msg);
				}
			}
		);
		//$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		// 取消关注
		$.post(
			CONTEXT_PATH + "/unfollow",
			{"entityType":3, "entityId":$(btn).prev().val()}, // 该节点的前一个节点的值，即隐藏的<input>值，即当前用户id
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0){
					window.location.reload(); //更新页面
				} else {
					alert(data.msg);
				}
			}
		);
		//$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}