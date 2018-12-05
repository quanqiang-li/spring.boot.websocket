/**
 * 字符串判断以什么内容开头
 */
String.prototype.startWith = function(s) {
	if (s == null || s == "" || this.length == 0 || s.length > this.length)
		return false;
	if (this.substr(0, s.length) == s)
		return true;
	else
		return false;
	return true;
}
// 检查浏览器支持
if (!"WebSocket" in window) {
	alert("您的浏览器不支持WebSocket");
}

// 打开遮罩层,创建昵称,如果有昵称,下一步
var nickName;
function confirmNickName() {
	nickName = document.getElementById("nickName").value;
	if(nickName == "" || nickName == undefined){
		alert("请先确认名称");
		return;
	}
	createConn();
}

var websocket;
// 创建连接
function createConn() {
	var wsServer = "ws://192.168.20.51:8081/ws?nickname=" + nickName;
	websocket = new WebSocket(wsServer);
	// 监听连接打开
	websocket.onopen = function(evt) {
		var msg = document.getElementById("msg");
		msg.innerHTML = "The connection is open";
	};

	// 监听服务器数据推送
	websocket.onmessage = function(evt) {
		var data = evt.data;
		// 总人数
		if (data.startWith("total:")) {
			var total = document.getElementById("total");
			total.innerHTML = data.substring(6);
			return;
		}
		// 房间内消息
		var msg = document.getElementById("msg");
		msg.innerHTML += "<br>" + data;
	};

	// 监听连接关闭
	websocket.onclose = function(evt) {
		alert("连接关闭");
	};
}
//创建房间并进入
var roomName;
function confirmRoomName() {
	roomName = document.getElementById("roomName").value;
	if(nickName == "" || nickName == undefined){
		alert("请先确认昵称");
		return;
	}
	if(roomName == "" || roomName == undefined){
		alert("请先确认房间");
		return;
	}
	var msg = document.getElementById("msg");
	msg.innerHTML += "<br>" + "进入房间" + roomName;
	websocket.send("enter:{'roomName':'"+roomName+"','createUser':'"+nickName+"'}");
	
}

/* 发消息 */
function send() {
	if(nickName == "" || nickName == undefined){
		alert("请先确认昵称");
		return;
	}
	if(roomName == "" || roomName == undefined){
		alert("请先确认房间");
		return;
	}
	var text = document.getElementById("text").value
	var msg = "msg:{'roomName':'"+roomName+"','nickname':'"+nickName+"','msg':'"+text+"'}";
	websocket.send(msg);
}