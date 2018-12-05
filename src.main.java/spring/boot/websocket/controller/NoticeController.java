package spring.boot.websocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("notice")
public class NoticeController {

	@GetMapping("sendNotice")
	@ResponseBody
	public String sendNotice(String msg) {
		return "ok";
	}
	
}
