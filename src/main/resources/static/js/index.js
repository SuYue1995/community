$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");//点击“发布”时，隐藏填写帖子内容的对话框

	//获取标题和内容
	var title = $("#recipient-name").val();//$("#id")jQuery id选择器，选中id为recipient-name的文本框，.val()得到文本框中的值
	var content = $("#message-text").val();

	//发送异步请求(POST)
	$.post( //三个参数
		//访问路径
		CONTEXT_PATH + "/discuss/add",
		//传入的数据：标题，内容
		{"title":title, "content":content},
		//回调函数，处理返回的结果
		function(data){
			data = $.parseJSON(data); // 字符串转换为对象
			//将返回的消息显示在提示框中
			$("#hintBody").text(data.msg); //id选择器获取提示框，然后.text()修改文本内容为msg
			// 显示提示框
			$("#hintModal").modal("show");
			//2秒后自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 如果成功，刷新页面
				if (data.code == 0){
					window.location.reload();//重新加载当前页面
				}
			}, 2000);

		}
	);


}