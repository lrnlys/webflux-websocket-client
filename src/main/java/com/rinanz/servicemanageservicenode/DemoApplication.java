package com.rinanz.servicemanageservicenode;

import com.rinanz.servicemanageservicenode.service.NodeService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class DemoApplication implements ApplicationListener<ApplicationReadyEvent> {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
		applicationReadyEvent.getApplicationContext().getBean(NodeService.class).connectToMaster();
	}
}
