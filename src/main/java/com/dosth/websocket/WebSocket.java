package com.dosth.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ServerEndpoint("/websocket/{userId}") // 接口路径 ws://localhost:8087/websocket/userId
public class WebSocket {

	// 与某个客户端的连接会话,需要通过它来给客户端发送数据
	private Session session;
	// 用户Id
	private String userId;
	
	private static CopyOnWriteArraySet<WebSocket> webSockets = new CopyOnWriteArraySet<>();
	// 在线用户
	private static ConcurrentHashMap<String, Session> sessionPool = new ConcurrentHashMap<>();
	
	/**
	 * 链接成功调用的方法
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam(value="userId") String userId) {
		this.session = session;
		this.userId = userId;
		webSockets.add(this);
		sessionPool.put(userId, session);
		log.info("[websocket消息]有新的连接,总数为:{}", webSockets.size());
	}
	
	/**
	 * 链接关闭调用的方法
	 */
	@OnClose
	public void onClose() {
		webSockets.remove(this);
		sessionPool.remove(this.userId);
		log.info("[websocket消息]连接断开,总数为:{}", webSockets.size());
	}
	
	/**
	 * 收到客户端消息调用的方法
	 * @param message
	 */
	@OnMessage
	public void onMessage(String message) {
		log.info("[websocket消息]收到客户端消息:{}", message);
	}
	
	/**
	 * 发送错误时的处理
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.error("用户错误,原因:{}", error.getMessage());
	}
	
	/**
	 * 广播消息
	 * @param message
	 */
	public void sendAllMessage(String message) {
		log.info("[websocket消息]广播消息:{}", message);
		for (WebSocket webSocket : webSockets) {
			if (webSocket.session.isOpen()) {
				webSocket.session.getAsyncRemote().sendText(message);
			}
		}
	}
	
	/**
	 * 单点消息
	 * @param userId
	 * @param message
	 */
	public void sendOneMessage(String userId, String message) {
		Session session = sessionPool.get(userId);
		if (session != null && session.isOpen()) {
			log.info("[websocket消息]单点消息:{}", message);
			session.getAsyncRemote().sendText(message);
		}
	}
	
	/**
	 * 多人单点消息
	 * @param userId
	 * @param message
	 */
	public void sendMoreMessage(String[] userIds, String message) {
		Session session;
		for (String userId : userIds) {
			session = sessionPool.get(userId);
			if (session != null && session.isOpen()) {
				log.info("[websocket消息]单点消息:{}", message);
				session.getAsyncRemote().sendText(message);
			}
		}
	}
}