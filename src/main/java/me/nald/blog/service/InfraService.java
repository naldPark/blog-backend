package me.nald.blog.service;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.adaptor.KubernetesAdaptor;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.Sandbox;
import me.nald.blog.data.vo.Node;
import me.nald.blog.data.vo.Pod;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.repository.SandboxRepository;
import me.nald.blog.response.ResponseObject;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

import static me.nald.blog.util.CommonUtils.stringListToHashMapList;
import static me.nald.blog.util.Constants.K8S_SANDBOX_NAMESPACE;
import static me.nald.blog.util.Constants.USER_ID;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfraService {
    private final KubernetesAdaptor kubeAdaptor;
    private final SandboxRepository sandboxRepository;
    private final AccountRepository accountRepository;



    public ResponseObject getClusterInfo() throws Exception {

        List<V1Node> nodesList = kubeAdaptor.getAgent().listNodes("").getItems();
        List<Map<String, Object>> nodeUsageSummary = kubeAdaptor.getAgent().getNodeSummary();
        List<Node> nodeResult = new ArrayList<>();

        for (V1Node node : nodesList) {
            System.out.println("node.getMetadata().getName()"+node.getMetadata().getName());
            Optional<Map<String, Object>> nodeUsage= nodeUsageSummary.stream().filter(s -> s.get("name").equals(node.getMetadata().getName())).findAny();
            System.out.println("nodeUsage"+nodeUsage.get());
            nodeResult.add(new Node(node, nodeUsage.get()));
        }

        List<V1Pod> podsList = kubeAdaptor.getAgent().listPodForAllNamespaces().getItems();
        List<Pod> podResult = podsList.stream().map(Pod::new).collect(Collectors.toList());
        HashMap<String, Object> data = new HashMap<>();
        data.put("nodeResult", nodeResult);
        data.put("podResult", podResult);

        ResponseObject result = new ResponseObject();
        result.putData(data);

        return result;
    }

    public ResponseObject getDiagramList() {

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
                "{ key: 7, parent: 3, name: 'Java', icon: 'java',  group: 103 }",
                "{ key: 8, parent: 2, name: 'Vue', icon: 'vue',  group: 102 }",
                "{ key: 9, parent: 3, name: 'JPA', icon: 'jpa',  group: 103 }",
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
        ResponseObject result = new ResponseObject();
        result.putData(stringListToHashMapList(diagramList));

        return result;
    }

    public ResponseObject getSandboxAccessPoint() throws ApiException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Account user = accountRepository.findByAccountId((String) request.getAttribute(USER_ID));
        String objectName = user.getAccountId()+"-sandbox";

        if(user.getSandbox().isEmpty()){
            try {
                String result = kubeAdaptor.getAgent().createNamespacedDeployment(objectName);
                System.out.println("리절트는 = "+ result);
                Sandbox sandbox = Sandbox.createSandbox(user, objectName, true);
                sandboxRepository.save(sandbox);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
        Map<String,String> labelSelector =new HashMap<>();
        labelSelector.put("app",objectName);
        V1PodList podList = kubeAdaptor.getAgent().listNamespacePods(K8S_SANDBOX_NAMESPACE, labelSelector);
        String serviceName = kubeAdaptor.getAgent().createNamespacedService(objectName);

        return new ResponseObject();
    }
}
