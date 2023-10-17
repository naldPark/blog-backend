package me.nald.blog.response;

import lombok.Data;

@Data
public class ServerResourceResponse {
    private String nodeName;
    private Float cpuUsed;
    private Float cpuTotal;
    private Float memUsed;
    private Float memTotal;
    private int gpuUsed;
    private int gpuTotal;
}
