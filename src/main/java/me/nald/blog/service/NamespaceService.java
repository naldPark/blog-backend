package me.nald.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.adaptor.KubernetesAdaptor;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.model.Cluster;
import me.nald.blog.model.Node;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.ServerResourceResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class NamespaceService {
    private final KubernetesAdaptor kubeAdaptor;

    public CommonResponse getClusterInfo() throws Exception {

        CommonResponse commonResponse = CommonResponse.of();

        List<V1Node> list = kubeAdaptor.agentWith().listNode("").getItems();
        List<Node> result = new ArrayList<>();
        for (V1Node node : list) {
            result.add(new Node(node));
        }

        List<String> atun = new ArrayList<>();
        result.stream().forEach(s -> {
            atun.add(s.getName());
            System.out.println(s.getCondition());
        });
        System.out.println("아뚠테스트e="+atun);
        System.out.println("========="+commonResponse);
        log.info("^^^ return value getNodeResource {} ", commonResponse);
        return commonResponse;
    }

    public CommonResponse getClusterResource(Long id) throws Exception {

       CommonResponse commonResponse = CommonResponse.of();
        ApiClient kubeConfig = Config.fromConfig(new ClassPathResource("k8s-config").getInputStream());

//        Cluster cluster = Cluster.builder()
//                .csp(0)
//                .defaultYn("Y")
//                .groupId(7)
//                .id(31)
//                .name("nald-cluster")
//                .k8sConfig(kubeConfig)
//                .build();
        Map<String, Float> usedResource = new HashMap<>();

        List<ServerResourceResponse> serverResource = new ArrayList<>();
        String cpuPrioryty = null;
        String gpuPrioryty = null;
        String memoryPrioryty = null;
        float cpuFreeValue = 0f;
        float gpuFreeValue = 0f;
        float memoryFreeValue = 0f;

        HashMap<String, List<Float>> resourceValue = (HashMap<String, List<Float>>) kubeAdaptor.agentWith().getClusterResource();


        V1NamespaceList test =  kubeAdaptor.agentWith().listNamespace(null);

        System.out.println("아뚠테스트e="+test);

        Set set = resourceValue.entrySet();
        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            String key = (String)entry.getKey();
            float cpuUsed = resourceValue.get(key).get(0);
            float cpuTotal = resourceValue.get(key).get(1);
            float memoryUsed = resourceValue.get(key).get(2);
            float memoryTotal = resourceValue.get(key).get(3);
            float gpuUsed = resourceValue.get(key).get(4);
            float gpuTotal = resourceValue.get(key).get(5);

            ServerResourceResponse item = new ServerResourceResponse();
            item.setNodeName(key);
            item.setCpuUsed(Float.valueOf(String.format("%.2f",cpuUsed)));
            item.setCpuTotal(Float.valueOf(String.format("%.2f",cpuTotal)));

            item.setMemUsed(Float.valueOf(String.format("%.2f",memoryUsed)));
            item.setMemTotal(Float.valueOf(String.format("%.2f",memoryTotal)));

            item.setGpuUsed((int)gpuUsed);
            item.setGpuTotal((int)gpuTotal);

            if ((gpuTotal - gpuUsed) > gpuFreeValue) {
                gpuFreeValue = gpuTotal - gpuUsed;
                gpuPrioryty = key;
            } else {
                if ((cpuTotal - cpuUsed) > cpuFreeValue) {
                    cpuFreeValue = cpuTotal - cpuUsed;
                    cpuPrioryty = key;
                }
                if ((memoryTotal - memoryUsed) > memoryFreeValue) {
                    memoryFreeValue = memoryTotal - memoryUsed;
                    memoryPrioryty = key;
                }
            }
            serverResource.add(item);
        }

        commonResponse.addData("namespaceUsed", usedResource);
        commonResponse.addData("serverInfo", serverResource);
        commonResponse.addData("cpuPriority", cpuPrioryty);
        commonResponse.addData("memoryPriority", memoryPrioryty);
        commonResponse.addData("gpuPriority", gpuPrioryty);
        System.out.println("========="+commonResponse);
        log.info("^^^ return value getNodeResource {} ", commonResponse);
        return commonResponse;
    }
}
