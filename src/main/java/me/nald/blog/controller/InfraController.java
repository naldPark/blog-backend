package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.RequireAuthBiz;
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
    public Callable<Object> getDiagramList() {
        return () -> infraService.getDiagramList();
    }

    @RequireAuthBiz
    @GetMapping("/clusterInfo")
    public Callable<Object> clusterInfo() {
        return () -> infraService.getClusterInfo();
    }

    @RequireAuthBiz
    @GetMapping("/clusterResource")
    public Callable<Object> getClusterResource() {
        return () -> infraService.getClusterInfo();
    }

    @RequireAuthBiz
    @GetMapping("/sandboxAccessPoint")
    public Callable<Object> getSandboxAccessPoint() {
        return () -> infraService.getSandboxAccessPoint();
    }

}

