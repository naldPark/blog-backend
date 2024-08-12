package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.RequireAuthBiz;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.InfraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/infra")
@Slf4j
public class InfraController {
    private final InfraService infraService;

    @GetMapping("/diagramList")
    public Callable<ResponseObject> getDiagramList() {
        return () -> infraService.getDiagramList();
    }

    @RequireAuthBiz
    @GetMapping("/clusterInfo")
    public Callable<ResponseObject> clusterInfo() {
        return () -> infraService.getClusterInfo();
    }

    @RequireAuthBiz
    @GetMapping("/clusterResource")
    public Callable<ResponseObject> getClusterResource() {
        return () -> infraService.getClusterInfo();
    }

    @RequireAuthBiz
    @GetMapping("/sandboxAccessPoint")
    public Callable<ResponseObject> getSandboxAccessPoint() {
        return () -> infraService.getSandboxAccessPoint();
    }

}

