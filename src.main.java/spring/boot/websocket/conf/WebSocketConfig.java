package spring.boot.websocket.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yeauty.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
	/**
	 * 当ServerEndpointExporter类通过Spring配置进行声明并被使用，<br>
	 * 它将会去扫描带有@ServerEndpoint注解的类 被注解的类将被注册成为一个WebSocket端点<br>
	 * 所有的配置项都在这个注解的属性中 ( 如:@ServerEndpoint("/ws") )
	 * @return
	 */
    @Bean 
    public ServerEndpointExporter serverEndpointExporter() {
    	System.out.println("实例化ServerEndpointExporter");
        return new ServerEndpointExporter();
    }
}