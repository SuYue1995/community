function like(btn, entityType, entityId){
    $.post(
        CONTEXT_PATH + "/like", // 功能的访问路径
        {"entityType": entityType, "entityId": entityId}, // 携带的参数
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