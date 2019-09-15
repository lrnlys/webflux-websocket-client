package com.rinanz.servicemanageservicenode.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service {

    private String hostname;
    private String groupId;
    private String artifactId;
    private String group;
    private List<Integer> ports= Arrays.asList(5566);
    private String healthEndPoint;
    private String version;
    private AtomicInteger delayReportCount=new AtomicInteger(0);
    private boolean isAvailable;
    private boolean isAlive;

    public String getId(){
        return String.join("_",groupId,artifactId,group);
    }

}
