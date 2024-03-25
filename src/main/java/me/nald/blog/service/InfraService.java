package me.nald.blog.service;

import com.google.gson.Gson;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.adaptor.KubernetesAdaptor;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Sandbox;
import me.nald.blog.model.Node;
import me.nald.blog.model.Pod;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.repository.SandboxRepository;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.Response;
import me.nald.blog.response.ServerResourceResponse;
import me.nald.blog.util.Util;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static me.nald.blog.util.Constants.K8S_SANDBOX_NAMESPACE;
import static me.nald.blog.util.Constants.USER_ID;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfraService {
    private final KubernetesAdaptor kubeAdaptor;
    private final SandboxRepository sandboxRepository;
    private final AccountRepository accountRepository;


    public CommonResponse getClusterInfo() throws Exception {

        CommonResponse commonResponse = CommonResponse.of();

        List<V1Node> nodesList = kubeAdaptor.agentWith().listNode("").getItems();
        List<Map<String, Object>> nodeUsageSummary = kubeAdaptor.agentWith().getNodeSummary();
        List<Node> nodeResult = new ArrayList<>();

        for (V1Node node : nodesList) {
            Map<String, Object> nodeUsage= nodeUsageSummary.stream().filter(s -> s.get("name").equals(node.getMetadata().getName())).findAny().get();
            nodeResult.add(new Node(node, nodeUsage));
        }

        List<V1Pod> podsList = kubeAdaptor.agentWith().listPodForAllNamespaces().getItems();
        List<Pod> podResult = podsList.stream().map(Pod::new).collect(Collectors.toList());

        commonResponse.addData("nodeResult", nodeResult);
        commonResponse.addData("podResult",podResult);
        return commonResponse;
    }

    public Response.CommonRes getDiagramList() {

        List<String> diagramList = Arrays.asList(
                "{ key: 0, name: 'Nald', icon: 'nald', description: 'nald.me' }",
                "{ key: 1, parent: 0, name: 'Infra', icon: 'infra', description: 'infra' }",
                "{ key: 2, parent: 0, name: 'Frontend', icon: 'frontend', description: 'frontend' }",
                "{ key: 3, parent: 0, name: 'Backend', icon: 'backend', description: 'backend' }",
                "{ key: 101, parent: 0, name: 'Infra',  isGroup: true }",
                "{ key: 102, parent: 0, name: 'Frontend', isGroup: true }",
                "{ key: 103, parent: 0, name: 'Backend',  isGroup: true }",
                "{ key: 4, parent: 1, name: 'Jenkins', icon: 'jenkins', group: 101 }",
                "{ key: 5, parent: 1, name: 'Argocd', icon: 'argocd', group: 101 }",
                "{ key: 7, parent: 3, name: 'Java', icon: 'java', asd: 'backend language', group: 103 }",
                "{ key: 8, parent: 2, name: 'Vue', icon: 'vue',  group: 102 }",
                "{ key: 9, parent: 3, name: 'JPA', icon: 'jpa', asd: 'JPA', group: 103 }",
                "{ key: 10, parent: 3, name: 'Typescript', icon: 'typescript', group: 102 }",
                "{ key: 12, parent: 1, name: 'Kubernetes', icon: 'kubernetes', group: 101 }",
                "{ key: 13, parent: 1, name: 'Docker', icon: 'docker',group: 101 }",
                "{ key: 14, parent: 2, name: 'Nginx', icon: 'nginx',  group: 102 }",
                "{ key: 15, parent: 1, name: 'Ubuntu', icon: 'ubuntu', group: 101 }",
                "{ key: 16, parent: 3, name: 'SpringBoot', icon: 'springBoot',  group: 103 }",
                "{ key: 18, parent: 1, name: 'Nexus', icon: 'nexus', group: 101 }",
                "{ key: 19, parent: 2, name: 'JavaScript', icon: 'javaScript', group: 102 }",
                "{ key: 20, parent: 3, name: 'Mariadb', icon: 'mariadb', group: 103 }",
                "{ key: 21, parent: 1, name: 'Nas', icon: 'nas', group: 101 }",
                "{ key: 22, parent: 1, name: 'Github', icon: 'git', group: 101 }",
                "{ key: 23, parent: 1, name: 'Helm', icon: 'helm',  group: 101 }"
        );
        return Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(diagramList))
                .build();
    }

    public CommonResponse getClusterResource() throws Exception {

       CommonResponse commonResponse = CommonResponse.of();

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

    public CommonResponse getSandboxAccessPoint() throws ApiException {
        CommonResponse commonResponse = CommonResponse.of();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Account user = accountRepository.findByAccountId((String) request.getAttribute(USER_ID));
        String objectName = user.getAccountId()+"-sandbox";

        if(user.getSandbox().isEmpty()){
            try {
                String result = kubeAdaptor.agentWith().createNamespacedDeployment(objectName);
                System.out.println("리절트는 = "+ result);
                Sandbox sandbox = Sandbox.createSandbox(user, objectName, true);
                sandboxRepository.save(sandbox);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
        V1PodList podList = kubeAdaptor.agentWith().listNamespacePod(K8S_SANDBOX_NAMESPACE, "app="+objectName);
        String serviceName = kubeAdaptor.agentWith().createNamespacedService(objectName);

        System.out.println("서비스 네임"+ serviceName);
        return commonResponse;
    }
}
