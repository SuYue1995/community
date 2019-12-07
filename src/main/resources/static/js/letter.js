$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");//隐藏对话框

	// 向服务端发送数据，接受到返回数据后再提示
	// 从页面上获取接受人和内容
	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 异步发送post请求，三个参数
	$.post(
		CONTEXT_PATH + "/letter/send", //访问路径
		{"toName":toName, "content":content},// 声明传的数据的参数
		function(data){// 处理服务端返回的结果，接受数据data。data是普通字符串，满足JSON格式，转换为js对象
			data = $.parseJSON(data); //用jQuery转换为js对象
			if (data.code == 0){ // 发送成功
				// 显示在提示框里
				$("#hintBody").text("发送成功");
			}else{
				$("#hintBody").text(data.msg);
			}
			// 刷新页面
			$("#hintModal").modal("show");//显示提示框
			setTimeout(function(){//两秒后，关闭提示框
				$("#hintModal").modal("hide");
				location.reload(); // 重载当前页面，刷新页面
			}, 2000);
		}
	);


}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}