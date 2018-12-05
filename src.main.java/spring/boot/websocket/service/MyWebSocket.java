package spring.boot.websocket.service;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yeauty.annotation.OnBinary;
import org.yeauty.annotation.OnClose;
import org.yeauty.annotation.OnError;
import org.yeauty.annotation.OnEvent;
import org.yeauty.annotation.OnMessage;
import org.yeauty.annotation.OnOpen;
import org.yeauty.annotation.ServerEndpoint;
import org.yeauty.pojo.ParameterMap;
import org.yeauty.pojo.Session;

import com.alibaba.fastjson.JSON;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import spring.boot.websocket.vo.RoomInfo;

/**
 * 
 * @author liqq<br>
 *         js端建立连接:var websocket = new WebSocket("ws://localhost:8081/ws");<br>
 *         注意端口是不能共用web的,不同session请求都是原型的,即线程安全
 */
@ServerEndpoint(value = "/ws", port = 8081)
@Component
public class MyWebSocket {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	// 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
	private static AtomicInteger onlineCount = new AtomicInteger(0);

	// 使用map来收集session，key为roomName，value为同一个房间的用户集合
	// concurrentMap的key不存在时报错，不是返回null
	public static final Map<String, CopyOnWriteArraySet<MyWebSocket>> rooms = new ConcurrentHashMap<String, CopyOnWriteArraySet<MyWebSocket>>();
	// 聊天室的基本信息
	public static final Map<String, RoomInfo> roomInfos = new ConcurrentHashMap<String, RoomInfo>();
	// 定义一个记录客户端的聊天昵称
	private String nickname;
	// 与某个客户端的连接会话，需要通过它来给客户端发送数据
	private Session session;
	// 客户端ip
	private String ip;
	// 默认未进入房间
	private static final String NO_ROOM = "no_room";

	/**
	 * 当有新的WebSocket连接进入时，对该方法进行回调 注入参数的类型:Session、HttpHeaders、ParameterMap
	 * 
	 * @param session
	 * @param headers
	 * @param parameterMap
	 * @throws IOException
	 */
	@OnOpen
	public void onOpen(Session session, HttpHeaders headers, ParameterMap parameterMap) throws IOException {
		this.session = session;
		this.nickname = parameterMap.getParameter("nickname");
		this.ip = session.remoteAddress().toString();
		log.info("新的连接加入,sessionId:{},websocket实例{},昵称{},ip{}", session.id(), this, nickname, ip);
		// 初始MyWebSocket对象放置到NO_ROOM房间
		if (!rooms.containsKey(NO_ROOM)) {
			// 创建房间不存在时，创建房间
			CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();
			// 添加
			webSocketSet.add(this);
			rooms.put(NO_ROOM, webSocketSet);
			RoomInfo roomInfo = new RoomInfo();
			roomInfo.createDate = new Date();
			roomInfo.createUser = "admin";
			roomInfo.roomName = NO_ROOM;

			roomInfos.put(NO_ROOM, roomInfo);
		} else {
			// 房间已存在，直接添加用户到相应的房间
			rooms.get(NO_ROOM).add(this);
		}
		int total = onlineCount.incrementAndGet();
		this.broadcastAll("total:" + total);

	}

	/**
	 * 当有WebSocket连接关闭时，对该方法进行回调 注入参数的类型:Session
	 * 
	 * @param session
	 * @throws IOException
	 */
	@OnClose
	public void onClose(Session session) throws IOException {
		log.info("断开连接,sessionId:{},websocket实例{},昵称{},ip{}", session.id(), this, nickname, ip);
		Set<Entry<String, CopyOnWriteArraySet<MyWebSocket>>> entrySet = rooms.entrySet();
		Iterator<Entry<String, CopyOnWriteArraySet<MyWebSocket>>> iterator = entrySet.iterator();
		// 遍历房间
		while (iterator.hasNext()) {
			Map.Entry<String, CopyOnWriteArraySet<MyWebSocket>> entry = iterator.next();
			String roomName = entry.getKey();
			CopyOnWriteArraySet<MyWebSocket> set = entry.getValue();
			set.remove(this);
			// 没有人员的房间,清理掉
			if (set.isEmpty()) {
				rooms.remove(roomName);
				roomInfos.remove(roomName);
			}

		}
		int total = onlineCount.decrementAndGet();
		this.broadcastAll("total:" + total);

	}

	/**
	 * 当有WebSocket抛出异常时，对该方法进行回调 注入参数的类型:Session、Throwable
	 * 
	 * @param session
	 * @param throwable
	 */
	@OnError
	public void onError(Session session, Throwable throwable) {
		throwable.printStackTrace();
	}

	/**
	 * 当接收到字符串消息时，对该方法进行回调 注入参数的类型:Session、String
	 * 
	 * @param session
	 * @param message
	 */
	@OnMessage
	public void onMessage(Session session, String message) {
		log.info("接收客户端{}消息:{}", session.id(),message);
		// 创建并加入房间"createAndEnter:{'roomName':'aaaa','createUser':'wsdcv'}"
		if (message.startsWith("createAndEnter")) {
			// 创建房间
			RoomInfo room = JSON.parseObject(message.substring(15), RoomInfo.class);
			room.createDate = new Date();
			roomInfos.put(room.roomName, room);
			// 加入房间
			CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();
			webSocketSet.add(this);
			rooms.put(room.roomName, webSocketSet);
			//退出默认房间
			rooms.get(NO_ROOM).remove(this);
		}
		// 加入房间"enter:{'roomName':'aaaa','createUser':'wsdcv'}"
		if (message.startsWith("enter")) {
			RoomInfo room = JSON.parseObject(message.substring(6), RoomInfo.class);
			CopyOnWriteArraySet<MyWebSocket> copyOnWriteArraySet = rooms.get(room.roomName);
			// 加入房间[TODO 如果不存在先创建在加入]
			if(copyOnWriteArraySet==null){
				room.createDate = new Date();
				roomInfos.put(room.roomName, room);
				copyOnWriteArraySet = new CopyOnWriteArraySet<MyWebSocket>();
				rooms.put(room.roomName, copyOnWriteArraySet);
			}
			copyOnWriteArraySet.add(this);
			//退出默认房间
			rooms.get(NO_ROOM).remove(this);
		}
		// 退出房间"quit:{'roomName':'aaaa'}"
		if (message.startsWith("quit")) {
			RoomInfo room = JSON.parseObject(message.substring(5), RoomInfo.class);
			// 退出房间
			rooms.get(room.roomName).remove(this);
			//加入默认房间
			rooms.get(NO_ROOM).add(this);
		}

		// 房间内消息"msg:{'roomName':'aaaa','nickname':'a1','msg':'helloworld'}"
		if (message.startsWith("msg")) {
			RoomInfo room = JSON.parseObject(message.substring(4), RoomInfo.class);
			// 房间内消息
			broadcast(room.roomName,room.nickname, room.msg);
		}
		//房间列表"roomList"
		if (message.startsWith("roomList")) {
			session.sendText(JSON.toJSONString(roomInfos));
		}

	}

	/**
	 * 当接收到二进制消息时，对该方法进行回调 注入参数的类型:Session、byte[]
	 * 
	 * @param session
	 * @param bytes
	 */
	@OnBinary
	public void onBinary(Session session, byte[] bytes) {
		for (byte b : bytes) {
			System.out.println(b);
		}
		session.sendBinary(bytes);
	}

	/**
	 * 当接收到Netty的事件时，对该方法进行回调 注入参数的类型:Session、Object
	 * 
	 * @param session
	 * @param evt
	 */
	@OnEvent
	public void onEvent(Session session, Object evt) {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
			switch (idleStateEvent.state()) {
			case READER_IDLE:
				System.out.println("read idle");
				break;
			case WRITER_IDLE:
				System.out.println("write idle");
				break;
			case ALL_IDLE:
				System.out.println("all idle");
				break;
			default:
				break;
			}
		}
	}

	// 按照房间名进行广播
	public void broadcast(String roomName,String nickname, String msg) {
		CopyOnWriteArraySet<MyWebSocket> set = rooms.get(roomName);
		Iterator<MyWebSocket> iterator2 = set.iterator();
		while (iterator2.hasNext()) {
			MyWebSocket myWebSocket = iterator2.next();
			myWebSocket.session.sendText(nickname+":" + msg);
		}

	}

	// 全部广播
	public void broadcastAll(String msg) {
		Set<Entry<String, CopyOnWriteArraySet<MyWebSocket>>> entrySet = rooms.entrySet();
		Iterator<Entry<String, CopyOnWriteArraySet<MyWebSocket>>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CopyOnWriteArraySet<MyWebSocket>> entry = iterator.next();
			CopyOnWriteArraySet<MyWebSocket> set = entry.getValue();
			Iterator<MyWebSocket> iterator2 = set.iterator();
			while (iterator2.hasNext()) {
				MyWebSocket myWebSocket = iterator2.next();
				myWebSocket.session.sendText(msg);
			}

		}
	}
}