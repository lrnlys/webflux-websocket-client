package com.rinanz.servicemanageservicenode.entity;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeEvent<T> {
    private String type;
    private T data;
}
