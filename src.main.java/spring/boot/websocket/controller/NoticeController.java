package spring.boot.websocket.controller;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import spring.boot.websocket.service.MyWebSocket;

@Controller
@RequestMapping("notice")
public class NoticeController {

	@GetMapping("sendNotice")
	@ResponseBody
	public String sendNotice(String msg) {
		return "ok";
	}
	
	public void addRoom(){
		
	}
	
	public String getRooms(){
		Map<String, CopyOnWriteArraySet<MyWebSocket>> rooms = MyWebSocket.rooms;
		return null;
	}
}
