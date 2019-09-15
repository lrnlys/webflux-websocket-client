package com.rinanz.servicemanageservicenode.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

@Configuration
public class NodeConfiguration {

    @Bean
    public WebSocketClient webSocketClient(){
        return new ReactorNettyWebSocketClient();
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
