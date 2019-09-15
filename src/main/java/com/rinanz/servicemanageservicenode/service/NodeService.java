package com.rinanz.servicemanageservicenode.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinanz.servicemanageservicenode.domain.Server;
import com.rinanz.servicemanageservicenode.domain.Service;
import com.rinanz.servicemanageservicenode.entity.NodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@Component
public class NodeService  {

    private Logger logger = LoggerFactory.getLogger(NodeService.class);
    private String masterUri = "ws://localhost:8080/service-manager-service";

    private Server server;
    private Flux<Service> healthFlux;
    @Autowired
    private WebSocketClient webSocketClient;
    @Autowired
    private ObjectMapper objectMapper;

    public NodeService(Server server) {
        this.server = server;
        this.healthFlux = this.server.pollHealth();
    }

    public void connectToMaster(){
        logger.info("going to connect to master {}.",masterUri);
        webSocketClient.execute(URI.create(masterUri),
                session -> {
                    Mono<Void> input = session.receive().then();

                    Flux<WebSocketMessage> serviceManageEventFlux = this.healthFlux.
                            map(service -> new NodeEvent<>("service", service)).
                            map(event -> {
                                try {
                                    return objectMapper.writeValueAsString(event);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }).
                            map(session::textMessage).
                            onErrorContinue((e,o)->logger.error("fail to process a service manage event", e));

                    Mono<Void> output = session.
                            send(serviceManageEventFlux).
                            onErrorContinue((e,o)->logger.error("failed to send service manage event to master.", e)).
                            then();

                    serviceManageEventFlux.subscribe();

                    return Flux.zip(input, output).then();
        }).subscribe(
                aVoid -> logger.info("websocket client started"),
                error -> {
                    logger.error("failed to connect to master {} , going to restart after 5 second.",masterUri,error);
                    Mono.just(this).
                            delayElement(Duration.ofSeconds(5)).
                            doOnNext(NodeService::connectToMaster).subscribe();
                });
    }


}
