package com.rinanz.servicemanageservicenode.domain;

import com.rinanz.servicemanageservicenode.entity.Health;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
@Component
public class Server {

    private Logger logger = LoggerFactory.getLogger(Server.class);
    private String hostname;
    private Map<String,Service> services;

    private File servicesFolder;

    public Server() throws UnknownHostException {
        this.hostname = InetAddress.getLocalHost().getCanonicalHostName();
        this.services = new ConcurrentHashMap<>();
        this.servicesFolder = new File("/Users/rinanz/apps");
        this.scanService().subscribe();
    }

    private Flux<Service> scanService(){
        return Flux.interval(Duration.ofSeconds(10)).
                flatMap(index->Flux.fromArray(servicesFolder.listFiles())).
                map(candidate->
                        Paths.get(candidate.getAbsolutePath(),"current","a.file").toFile()
                ).
                publishOn(Schedulers.elastic()).
                filter(File::exists).
                map(instanceProfile-> {
                    Service service = new Service();
                    service.setHostname(hostname);
                    service.setAvailable(false);
                    return service;
                }).
                map(service->{
                    Service oldVersion = this.getServices().get(service.getId());
                    if(!service.equals(oldVersion) || service.getDelayReportCount().intValue()>=6){
                        service.getDelayReportCount().set(0);
                        this.getServices().put(service.getId(),service);
                        return service;
                    }else{
                        service.getDelayReportCount().incrementAndGet();
                        return null;
                    }
                }).filter(service -> service!=null).onErrorContinue((e,o)->{
                    logger.error("error",e);
                });
    }


    public Flux<Service> pollHealth(){
        return Flux.interval(Duration.ofSeconds(5),Duration.ofSeconds(10)).
                flatMap(index->Flux.fromIterable(this.services.values())).
                publishOn(Schedulers.elastic()).
                map(service -> {
                    for(int port : service.getPorts()){
                        WebClient webClient = WebClient.create(
                                String.format("http://localhost:%s/%s/api/health",port , service.getArtifactId()));
                        try {
                            Health health = webClient.get().retrieve().bodyToMono(Health.class).block(Duration.ofSeconds(2));
                            service.setAvailable(health.isAvailable());
                            service.setVersion(health.getVersion());
                            return service;
                        }catch (Exception e){
                            if(e.getCause()!=null && e.getCause().getCause() instanceof ConnectException) {
                                service.setAvailable(false);
                                service.setAlive(false);
                                return service;
                            }
                            throw e;
                        }
                    }
                    return null;
                }).filter(service -> service!=null).onErrorContinue((e,o)->logger.error("error",e)).share();
    }

}
