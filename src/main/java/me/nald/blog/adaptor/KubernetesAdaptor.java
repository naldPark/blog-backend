package me.nald.blog.adaptor;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.util.KubeUtils;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.nald.blog.util.Constants.*;
import static me.nald.blog.util.KubeUtils.executeCommand;

@Service("kubeAdaptor")
@Slf4j
public class KubernetesAdaptor {
  private final BlogProperties blogProperties;
  private static Agent defaultAgent;

  public KubernetesAdaptor(BlogProperties blogProperties) {
    this.blogProperties = blogProperties;
    if (defaultAgent == null) {
      try {
        ApiClient apiClient = createApiClient();
        Configuration.setDefaultApiClient(apiClient);
        defaultAgent = new Agent(apiClient);
      } catch (IOException e) {
        log.error("Failed to create API client", e);
        throw new RuntimeException(e);
      }
    }
  }

  private ApiClient createApiClient() throws IOException {
    String kubeConfigPath = blogProperties.getCommonPath() + "/k8s-config";
    return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
  }

  public Agent getAgent() {
    return defaultAgent;
  }


  @RequiredArgsConstructor
  public static class Agent {
    private final ApiClient apiClient;

    private CoreV1Api coreV1Api() {
      return new CoreV1Api(apiClient);
    }

    private AppsV1Api appsV1Api() {
      return new AppsV1Api(apiClient);
    }

    public List<Map<String, Object>> getNodeSummary() throws IOException {
      List<Map<String, Object>> result = new ArrayList<>();
      executeCommand("kubectl get nodes", new KubeUtils(KubeUtils.NODE, result));
      executeCommand("kubectl top nodes", new KubeUtils(KubeUtils.NODE_USAGE, result));
      return result;
    }

    public V1NodeList listNodes(String fieldSelector) throws ApiException {
      return coreV1Api().listNode().execute();
    }

    public String createNamespacedDeployment(String name) throws ApiException {
      V1DeploymentList deployments = appsV1Api().listNamespacedDeployment(K8S_SANDBOX_NAMESPACE)
              .execute();
      V1Deployment defaultDeployment = deployments.getItems().stream()
              .filter(d -> d.getMetadata().getName().equals(K8S_SANDBOX_DEFAULT_LABEL))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Default deployment not found"));

      Map<String, String> label = Map.of("app", name);
      List<V1LocalObjectReference> imagePullSecrets = List.of(new V1LocalObjectReference().name(K8S_IMAGE_PULL_SECRET_NAME));
      V1PodSpec podSpec = new V1PodSpec()
              .containers(defaultDeployment.getSpec().getTemplate().getSpec().getContainers())
              .imagePullSecrets(imagePullSecrets);

      V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
              .replicas(1)
              .selector(new V1LabelSelector().matchLabels(label))
              .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta().labels(label)).spec(podSpec));

      V1Deployment newDeployment = new V1Deployment()
              .kind("Deployment")
              .metadata(new V1ObjectMeta().name(name))
              .spec(deploymentSpec);
      V1Deployment result = appsV1Api().createNamespacedDeployment(K8S_SANDBOX_NAMESPACE, newDeployment)
              .execute();
      System.out.println(result);
      return result.getMetadata().getName();
    }

    public String createNamespacedService(String name) throws ApiException {
      Map<String, String> selector = Map.of("app", name);
      V1ServiceSpec spec = new V1ServiceSpec()
              .ports(List.of(new V1ServicePort().port(8088)))
              .selector(selector)
              .type("ClusterIP");

      V1Service service = new V1Service()
              .kind("Service")
              .metadata(new V1ObjectMeta().name(name))
              .spec(spec);

        return coreV1Api().createNamespacedService(K8S_SANDBOX_NAMESPACE, service).execute()
              .getMetadata().getName();
    }

    public V1PodList listPodForAllNamespaces() throws ApiException {
      return coreV1Api().listPodForAllNamespaces().execute();
    }

    public static String convertNamespaceName(String name) {
      return K8S_NAMESPACE_PREFIX + name;
    }

    public V1PodList listNamespacePods(String namespace, Map<String, String> labelSelector) throws ApiException {
      List<V1Pod> filteredPods = coreV1Api().listNamespacedPod(convertNamespaceName(namespace)).execute().getItems()
              .stream()
              .filter(pod -> pod.getMetadata().getLabels().equals(labelSelector))
              .collect(Collectors.toList());

      V1PodList filteredPodList = new V1PodList();
      filteredPodList.setItems(filteredPods);
      return filteredPodList;
    }


  }

}
