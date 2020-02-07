// 给uploadForm定义事件。点击submit按钮，触发表单的提交事件
$(function () { // 页面加载完后调用function
   $("#uploadForm").submit(upload); // 表单提交事件由upload处理
});

function upload() {
    $.ajax({ // $.get/post是对ajax的简化
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false, // 不把表单内容转为字符串。默认情况下，浏览器将表单内容转换为字符串传给服务器。上传文件不需要转换
        contentType: false, // 不让JQuery设置上传类型，浏览器自动进行设置。提交文件为二进制，和其他数据混编在一起时，浏览器会添加随机字符串确定边界，容易拆分内容。如果指定contentType，JQuery自动设置类型，无法设置边界，上传会有问题
        data: new FormData($("#uploadForm")[0]), // js对象，用来封装表单数据，传文件需要这样特殊处理.jquery对象是dom对象的数组
        success: function (data) { // 七牛云返回json格式数据data，不需要解析
            if (data && data.code == 0){ // 如果成功，处理响应信息
                // 更新头像访问路径，异步方式访问controller
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName": $("input[name='key']").val()}, // fileName从表单中取，$()元素选择器，[]属性选择器，将fileName传递给服务端
                    function (data) { // 服务端接收到后，返回响应
                        data = $.parseJSON(data); // 服务端返回普通字符串，格式为json，所以转换为json对象
                        if (data.code == 0){
                            window.location.reload(); // 刷新页面
                        } else {
                            alert("上传失败！");
                        }
                    }
                );
            } else {
                alert("上传失败");
            }
        }
    });
    return false; // return false，不继续提交表单。如果true，仍会尝试提交表单，表单中没有定义action，导致问题。
}