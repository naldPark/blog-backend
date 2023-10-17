package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.service.NamespaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
public class NamespaceController {
    private final NamespaceService namespaceService;

    @GetMapping("/namespace/{id}/clusterResource")
    public Callable<Object> getClusterResource(@PathVariable Long id) {
        return () -> namespaceService.getClusterInfo();
    }

}
