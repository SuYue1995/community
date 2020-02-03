// 通常，在页面加载完以后，html标签加载完，用js得到标签，动态绑定事件
$(function () { // 页面加载事件，和js中的upload()一样，页面加载完以后调用
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);

});

function like(btn, entityType, entityId, entityUserId, postId){
    $.post(
        CONTEXT_PATH + "/like", // 功能的访问路径
        {"entityType": entityType, "entityId": entityId, "entityUserId":entityUserId, "postId": postId}, // 携带的参数
        function (data) { // 处理返回的数据
            data = $.parseJSON(data);
            if (data.code == 0){ //接受请求成功
                // 通过节点 获取子节点，即下级的<b>,<i>标签
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':'赞');
            } else { // 请求失败，弹出提示
                alert(data.msg);
            }
        }
    );
}

// 置顶
function setTop() {
    // 发送异步服务器请求传参
    $.post(
        CONTEXT_PATH + "/discuss/top", // 请求路径
        {"id":$("#postId").val()}, // 参数
        function (data) { //处理响应返回的结果
            data =  $.parseJSON(data);//将满足json格式的字符串解析成js对象
            if (data.code == 0){ // 成功
                // 修改按钮的可用性，点过“置顶”按钮后设置为不可用
                $("#topBtn").attr("disabled", "disabled");
            } else { // 失败
                alert(data.msg)
            }
        }
    );
}

// 加精
function setWonderful() {
    // 发送异步服务器请求传参
    $.post(
        CONTEXT_PATH + "/discuss/wonderful", // 请求路径
        {"id":$("#postId").val()}, // 参数
        function (data) { //处理响应返回的结果
            data =  $.parseJSON(data);//将满足json格式的字符串解析成js对象
            if (data.code == 0){ // 成功
                // 修改按钮的可用性，点过“加精”按钮后设置为不可用
                $("#wonderfulBtn").attr("disabled", "disabled");
            } else { // 失败
                alert(data.msg)
            }
        }
    );
}

// 删除
function setDelete() {
    // 发送异步服务器请求传参
    $.post(
        CONTEXT_PATH + "/discuss/delete", // 请求路径
        {"id":$("#postId").val()}, // 参数
        function (data) { //处理响应返回的结果
            data =  $.parseJSON(data);//将满足json格式的字符串解析成js对象
            if (data.code == 0){ // 成功
                // 跳转到首页
                location.href=CONTEXT_PATH+"/index";
            } else { // 失败
                alert(data.msg)
            }
        }
    );
}