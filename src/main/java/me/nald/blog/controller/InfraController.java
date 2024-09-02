package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.InfraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/infra")
public class InfraController {
    private final InfraService infraService;

    @GetMapping("/diagram/list")
    public Callable<ResponseObject> getDiagramList() {
        return () -> infraService.getDiagramList();
    }

    @GetMapping("/clusterInfo")
    public Callable<ResponseObject> clusterInfo() {
        return () -> infraService.getClusterInfo();
    }

    @GetMapping("/clusterResource")
    public Callable<ResponseObject> getClusterResource() {
        return () -> infraService.getClusterInfo();
    }

    @GetMapping("/sandboxAccessPoint")
    public Callable<ResponseObject> getSandboxAccessPoint() {
        return () -> infraService.getSandboxAccessPoint();
    }

}

