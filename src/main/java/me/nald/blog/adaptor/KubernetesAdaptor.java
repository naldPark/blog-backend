package me.nald.blog.adaptor;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.model.Cluster;
import me.nald.blog.model.JsonPatch;
import me.nald.blog.util.KubeConfigValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.error.YAMLException;

import static me.nald.blog.util.Constants.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;

@Service("kubeAdaptor")
@Slf4j
public class KubernetesAdaptor {
    private static Agent defaultAgent;
    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }


    @Value("${docker.endpoint}")
    private String dockerEndpoint;
    @Value("${docker.username}")
    private String dockerUsername;
    @Value("${docker.secret-password}")
    private String dockerSecretPassword;
    @Value("${docker.email}")
    private String dockerEmail;

    @Autowired
    public KubernetesAdaptor() {
        // Do nothing
    }

    //agentwith가 3가지로 주입가능한데 나는 그냥 resource하위에있는 config로 불러오기
    public static Agent agentWith(String... k8sConfig) {
        if (Objects.nonNull(k8sConfig) && k8sConfig.length > 0 && !StringUtils.isEmpty(k8sConfig[0])) {
            defaultAgent = new Agent(k8sConfig[0], null);
            return defaultAgent;
        } else {
            return agentWith();
        }
    }

    public static Agent agentWith(Cluster cluster) {
        return Optional.ofNullable(cluster)
                .map(Cluster::getK8sConfig)
                .map(KubernetesAdaptor::agentWith)
                .orElse(KubernetesAdaptor.agentWith());
    }

    public static Agent agentWith() {
        ApiClient apiClient = null;
        if (defaultAgent == null) {
            try {
                ClassPathResource k8sConfig = new ClassPathResource("k8s-config");
                if (k8sConfig.exists()) {
                    apiClient = Config.fromConfig(new ClassPathResource("k8s-config").getInputStream());
                } else {
                    Path configPath = Paths.get(blogProperties.getCommonPath() + "/k8s-config");
                    apiClient = Config.fromConfig(Files.newInputStream(configPath));
                }
                apiClient.setReadTimeout(60 * 1000);
                Configuration.setDefaultApiClient(apiClient);
                defaultAgent = new Agent("", apiClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return defaultAgent;
    }

    public static String convertNamespaceName(String name) {
        return KUBERNETES_NAMESPACE_PREFIX + name;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Agent {
        private final String k8sConfig;
        private final ApiClient defaultApiClient;

        private ApiClient apiClient() {
            if (Objects.nonNull(defaultApiClient)) {
                return defaultApiClient;
            } else {
                ApiClient client;
                KubeConfig kubeConfig;
                try (InputStream inputStream = new ByteArrayInputStream(k8sConfig.getBytes())) {
                    kubeConfig = KubeConfig.loadKubeConfig(new InputStreamReader(inputStream, StandardCharsets.UTF_8.name()));
                    KubeConfigValidator.validate(kubeConfig);
                } catch (IOException | YAMLException | ClassCastException e) {
                    log.error("apiClient", e);
                    String message = String.format("K8S-Config 로드가 실패했습니다.(%s)", e.getMessage());
                    throw new RuntimeException(e);
                }
                try {
                    client = Config.fromConfig(kubeConfig);
                } catch (IOException | RuntimeException e) {
                    log.error("apiClient", e);
                    String message = String.format("Cluster의 K8S-Config가 유효하지 않은 값으로 설정되어 있습니다. Cluster Owner 에게 문의하세요.(%s)", e.getMessage());
                    throw new RuntimeException(e);
                }
                client.setReadTimeout(60 * 1000);
                return client;
            }
        }

        private CoreV1Api coreV1Api() {
            return new CoreV1Api(apiClient());
        }

        private AppsV1Api appsV1Api() {
            return new AppsV1Api(apiClient());
        }

        public V1NodeList listNode(String fieldSelector) throws ApiException {
            return coreV1Api().listNode(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }

        //        TODO sa-922
        public V1ConfigMap readNamespacedConfigMap(String configMapName, String namespace) throws ApiException {
            V1ConfigMap v1ConfigMap = coreV1Api().readNamespacedConfigMap(configMapName, namespace, "true", false, false);
            return v1ConfigMap;
        }

        public V1NamespaceList listNamespace(String fieldSelector) throws ApiException {
            return coreV1Api().listNamespace(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }

        public V1Namespace readNamespace(String namespace, boolean convert) throws ApiException {
            return coreV1Api().readNamespace(convert ? convertNamespaceName(namespace) : namespace, STR_FALSE, null, null);
        }

        public V1ResourceQuota readNamespacedResourceQuota(String name, boolean convert) throws ApiException {
            return coreV1Api().readNamespacedResourceQuota(RESOURCE_QUOTA_NAME, convert ? convertNamespaceName(name) : name, null, null, null);
        }

        public V1Namespace createNamespace(String name) throws ApiException {
            Map<String, String> labels = new HashMap<>();
            labels.put("katib-metricscollector-injection", "enabled");
            V1ObjectMeta meta = new V1ObjectMeta().labels(labels).name(convertNamespaceName(name));
            V1Namespace namespace = new V1Namespace().kind("Namespace").metadata(meta);
            return coreV1Api().createNamespace(namespace, STR_FALSE, null, null);
        }

        public ApiResponse<V1Status> deleteNamespace(String namespace) throws ApiException {
            V1DeleteOptions option = new V1DeleteOptions();
            return coreV1Api().deleteNamespaceWithHttpInfo(convertNamespaceName(namespace), STR_FALSE, null, null, null, STR_FALSE, option);
        }

        public V1ServiceAccount createNamespacedServiceAccount(String namespace, String name) throws ApiException {
            V1ServiceAccount serviceAccount = new V1ServiceAccount()
                    .kind("ServiceAccount").metadata(new V1ObjectMeta().name(name));
            return coreV1Api().createNamespacedServiceAccount(namespace, serviceAccount, STR_FALSE, null, null);
        }

        public V1ServiceAccount readNamespacedServiceAccount(String namespace, String name) throws ApiException {
            return coreV1Api().readNamespacedServiceAccount(name, namespace, STR_FALSE, null, null);
        }

        public V1ClusterRoleBinding createNamespacedClusterRoleBinding(String namespace, String name) throws ApiException {
            RbacAuthorizationV1Api rbacAuthorizationApi = rbacAuthorizationV1Api();
            V1ClusterRoleBinding body = new V1ClusterRoleBinding()
                    .kind("ClusterRoleBinding")
                    .metadata(new V1ObjectMeta().name(name))
                    .subjects(Arrays.asList(new V1Subject().kind("ServiceAccount").name("accu-modeler").namespace(namespace)))
                    .roleRef(new V1RoleRef().kind("ClusterRole").name("cluster-admin").apiGroup("rbac.authorization.k8s.io"));
            return rbacAuthorizationApi.createClusterRoleBinding(body, STR_FALSE, null, null);
        }

        private RbacAuthorizationV1Api rbacAuthorizationV1Api() {
            return new RbacAuthorizationV1Api(apiClient());
        }

        public V1Secret createSecret(String namespace, String dockerEndpoint, String dockerUsername, String dockerSecretPassword, String dockerEmail) throws ApiException, JSONException {
            Map<String, byte[]> data = new HashMap<>();
            JSONObject config = new JSONObject();
            JSONObject dockerInfo = new JSONObject()
                    .put("username", dockerUsername)
                    .put("password", dockerSecretPassword)
                    .put("email", dockerEmail);
            config.put("auths", new JSONObject().put(dockerEndpoint, dockerInfo));
            data.put(".dockerconfigjson", config.toString().getBytes());

            V1ObjectMeta metaData = new V1ObjectMeta().name("regcred").namespace(convertNamespaceName(namespace));
            V1Secret body = new V1Secret().metadata(metaData).type("kubernetes.io/dockerconfigjson").data(data);
            return coreV1Api().createNamespacedSecret(convertNamespaceName(namespace), body, STR_FALSE, null, null);
        }

        public V1SecretList listNamespaceSecret(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedSecret(namespace, STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1Secret readNamespaceSecret(String name, String namespace) throws ApiException {
            return coreV1Api().readNamespacedSecret(name, namespace, STR_TRUE, null, null);
        }

        public V1DeploymentList listNamespaceDeployment(String namespace) throws ApiException {
            return appsV1Api().listNamespacedDeployment(convertNamespaceName(namespace), STR_FALSE, true, "", "", "", null, "", null, 0, false);
        }

        public V1PodList listNamespacePod(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1PodList listDefaultNamespacePod(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedPod(namespace, STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1Pod readNamespacedPod(String namespace, String name) {
            V1Pod pod = null;
            try {
                pod = coreV1Api().readNamespacedPodStatus(name, convertNamespaceName(namespace), STR_TRUE);
            } catch (ApiException e) {
                e.printStackTrace();
                pod = readErrorPod(convertNamespaceName(namespace), name);
            }
            return pod;
        }

        public V1PodList listPodForAllNamespaces(String labelSelector) throws ApiException {
            return coreV1Api().listPodForAllNamespaces(false, null, null, labelSelector, null, null, null, null, null, false);
        }

        public V1ResourceQuotaList listNamespacedResourceQuota() throws ApiException {
            return coreV1Api().listResourceQuotaForAllNamespaces(false, null, RESOURCE_QUOTA_FIELD_SELECTOR, null, null, null, null, null, null, false);
        }

        private V1Pod readErrorPod(String namespace, String name) {
            V1Pod pod = null;
            try {
                V1PodList list = coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", null, null, "", null, 0, false);
                for (V1Pod p : list.getItems()) {
                    if (p.getMetadata().getName().startsWith(name)) {
                        pod = p;
                    }
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return pod;
        }

//        public V1EventList listNamespacedPodEvent(String namespace, String name) throws ApiException {
//            String fieldSelector = String.format("involvedObject.name=%s", name);
//            return coreV1Api().listNamespacedEvent(convertNamespaceName(namespace), STR_FALSE, null, null, fieldSelector, null, 100, null, null,10, false);
//        }

        // GPU 리소스 제한은 업데이트가 불가능 하여 디비로만 관리한다고 함, gpu 값 제거
        public V1ResourceQuota createNamespacedResourceQuota(String namespace, Float cpu, Float mem) throws ApiException {
            V1ObjectMeta meta = new V1ObjectMeta().name(RESOURCE_QUOTA_NAME);

            Map<String, Quantity> hard = new HashMap<>();
            hard.put(K8S_RESOURCE_QUOTA_LIMIT_CPU, new Quantity(cpuToCoreUnit(cpu)));
            hard.put(K8S_RESOURCE_QUOTA_LIMIT_MEMORY, new Quantity(memToCoreUnit(mem)));

            V1ResourceQuotaSpec spec = new V1ResourceQuotaSpec().hard(hard);
            V1ResourceQuota body = new V1ResourceQuota().kind("ResourceQuota").metadata(meta).spec(spec);

            return coreV1Api()
                    .createNamespacedResourceQuota(convertNamespaceName(namespace), body, STR_FALSE, null, null);
        }

        // GPU 리소스 제한은 업데이트가 불가능 하여 디비로만 관리한다고 함, gpu 값 제거
        public V1ResourceQuota patchNamespacedResourceQuota(String namespace, Float cpu, Float mem) throws ApiException {
            return coreV1Api().patchNamespacedResourceQuota(
                    RESOURCE_QUOTA_NAME,
                    convertNamespaceName(namespace),
                    new io.kubernetes.client.custom.V1Patch(
                            Arrays.asList(
                                    JsonPatch.replace("/spec/hard/limits.cpu", cpuToCoreUnit(cpu)),
                                    JsonPatch.replace("/spec/hard/limits.memory", memToCoreUnit(mem))
                            ).toString()),
                    STR_FALSE,
                    null,
                    null,
                    null
            );
        }

        /*
        private String gpuToCoreUnit(Float gpu) {
            return Math.round(gpu) + K8S_GPU_UNIT;
        }
        */

        private String cpuToCoreUnit(Float cpu) {
            return Math.round(cpu * 1000) + K8S_CPU_UNIT;
        }

        private String memToCoreUnit(Float memory) {
            return Math.round(memory * 1000) + K8S_MEMORY_UNIT;
        }

        /*
        private int cpuToCoreValue(Float cpu) {
            return Math.round(cpu * 1000);
        }

        private int memToCoreValue(Float memory) {
            return Math.round(memory * 1000);
        }

        private Float CoreUnitToCpu(String cpu) {
            if (cpu.indexOf(K8S_CPU_UNIT) != -1) {
                return Float.valueOf(cpu.replace(K8S_CPU_UNIT, "")).floatValue() / 1000;
            } else {
                return Float.valueOf(cpu).floatValue();
            }
        }

        private Float CoreUnitToMem(String mem) {
            return Float.valueOf(mem.replace(K8S_MEMORY_UNIT, "")).floatValue() / 1000;
        }
        */

        public V1Status deleteNamspacedSecret(String namespace) {
            V1DeleteOptions body = new V1DeleteOptions();
            try {
                return coreV1Api().deleteNamespacedSecret("regcred", convertNamespaceName(namespace), STR_FALSE, null, 0, false, STR_TRUE, body);
            } catch (ApiException ignore) {
                log.error("deleteNamspacedSecret {}", ignore);
                return null;
            }
        }

        public Float getMaximumGpuResourceCapacity() throws ApiException {
            Float max = 0F;

            V1NodeList nodeList = listNode("");
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1 && node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")) {
                    continue;
                }

                Float gpu = Optional.ofNullable(node)
                        .map(V1Node::getStatus)
                        .map(V1NodeStatus::getCapacity)
                        .map(capa -> capa.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU))
                        .map(Quantity::getNumber)
                        .map(BigDecimal::floatValue)
                        .orElse(0F);

                max = (max.compareTo(gpu) > 0) ? max : gpu;
            }
            return max;
        }

        public Map<String, List<Float>> getClusterResource() throws ApiException, IOException {
            HashMap<String, List<Float>> result = new HashMap<>();

            V1NodeList nodeList = KubernetesAdaptor.agentWith().listNode("");
//            System.out.println("노드리스트 ="+nodeList);
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1
                        && (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)))) {
                    List<Float> value = getAllResourceNode(node.getMetadata().getName(), 0);
                    result.put(node.getMetadata().getName(), value);
                }
            }
            return result;
        }

        public Map<String, List<Float>> getClusterResource(Cluster cluster) throws ApiException, IOException {
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            HashMap<String, List<Float>> result = new HashMap<>();

            V1NodeList nodeList = KubernetesAdaptor.agentWith(cluster).listNode("");
//            System.out.println("노드리스트 ="+nodeList);
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1
                        && (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)))) {
                    List<Float> value = getAllResourceNode(node.getMetadata().getName(), cId);
                    result.put(node.getMetadata().getName(), value);
                }
            }
            return result;
        }

        public Map<String, List<Object>> getUsageResource(String namespaces, Cluster cluster, String podType, String type) throws IOException {
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            return getResourcelimitsOfPods(namespaces, cId, podType, type);
        }

        public Float getUsedNodeResource(String type, Cluster cluster) throws ApiException, IOException {
            Float result = 0F;
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            V1NodeList nodeList = KubernetesAdaptor.agentWith(cluster).listNode("");
            // 노드가 두개 이상(마스터 제외한 노드가 있을 때)
            if (nodeList.getItems().size() > 1) {
                for (V1Node node : nodeList.getItems()) {
                    if (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE) && isNodeTypeWithResource(type, node)) {
                        result += getResourcelimitsOfNode(node.getMetadata().getName(), cId, type);
                    }
                }
            }
            return result;
        }

        private boolean isNodeTypeWithResource(String type, V1Node node) {
            return (type.equals(K8S_RESOURCE_TYPE_CPU) && node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE))
                    || (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU) && node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE))
                    || (type.equals(K8S_RESOURCE_TYPE_MEMORY) && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                    || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)));
        }

        private Map<String, List<Object>> getResourcelimitsOfPods(String namespace, long cId, String podyType, String type) throws IOException {
            HashMap<String, List<Object>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl top pod -n %s --kubeconfig=%s", convertNamespaceName(namespace), fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf(podyType) == 0) {
                        String[] splitData = line.split("\\s+");
                        key = line.substring(0, line.indexOf(" "));
                        List<Object> dataList = dataMap.get(key);
                        if (dataList == null) {
                            dataList = new ArrayList<>();
                            dataMap.put(key, dataList);
                        }
                        if (type.equals(K8S_RESOURCE_TYPE_CPU)) {
                            dataList.add("NA");
                            dataList.add(String.valueOf(convertToFloat(splitData[1], true, false)));
                        } else if (type.equals(K8S_RESOURCE_TYPE_MEMORY)) {
                            dataList.add("NA");
                            dataList.add(String.valueOf(convertToFloat(splitData[2], false, true)));
                        } else if (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU)) {
                            dataList.add("NA");
                            dataList.add("0");
                        } else {
                            log.error("error : Type is unknown");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return dataMap;
        }

        private Float getResourcelimitsOfNode(String nodeName, long cId, String type) throws IOException {
            Float result = 0F;
            Map<String, List<String>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl describe node %s --kubeconfig=%s", nodeName, fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("name") == 0) {
                        dataMap = new LinkedHashMap<>();
                    }
                    if (dataMap != null) {
                        if (line.length() > 0) {
                            key = addDataToDataMap(line, dataMap, key);
                        }
                        if (line.indexOf("Events:") == 0) {
                            result = processEvents(type, dataMap);
                            dataMap.clear();
                            dataMap = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        private String addDataToDataMap(String line, Map<String, List<String>> dataMap, String key) {
            String[] splitData = line.split(":");
            if (line.charAt(0) != ' ') {
                key = splitData[0];
            }
            List<String> dataList = dataMap.computeIfAbsent(key, k -> new ArrayList<>());
            dataList.add(line);
            return key;
        }

        private Float processEvents(String type, Map<String, List<String>> dataMap) {
            List<String> resource = Arrays.asList(K8S_RESOURCE_TYPE_CPU, K8S_RESOURCE_TYPE_MEMORY, K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
//            Map<String, String> capacity = getMapData("Capacity", ":", dataMap);
            Map<String, Map<String, String>> allocated = getUsedData("Allocated resources", resource, dataMap);
            Map<String, String> cpuUsed = allocated.get(K8S_RESOURCE_TYPE_CPU);
            Map<String, String> memoryUsed = allocated.get(K8S_RESOURCE_TYPE_MEMORY);
            Map<String, String> gpuUsed = allocated.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            if (type.equals(K8S_RESOURCE_TYPE_CPU)) {
                return convertToFloat(String.valueOf(cpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], true, false);
            } else if (type.equals(K8S_RESOURCE_TYPE_MEMORY)) {
                return convertToFloat(String.valueOf(memoryUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], false, false);
            } else if (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU)) {
                if (gpuUsed != null) {
                    return Float.parseFloat(String.valueOf(gpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0].substring(0, 1));
                }
            } else {
                log.error("error : Type is unknown");
            }
            return 0f;
        }

        private List<Float> getAllResourceNode(String nodeName, long cId) throws IOException {
            List<Float> result = new ArrayList<>();
            Map<String, List<String>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl describe node %s --kubeconfig=%s", nodeName, fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("name") == 0) {
                        dataMap = new LinkedHashMap<>();
                    }
                    if (dataMap != null) {
                        if (line.length() > 0) {
                            key = addDataToDataMap(line, dataMap, key);
                        }
                        if (line.indexOf("Events:") == 0) {
                            List<Float> eventResult = processEvents(dataMap);
                            result.addAll(eventResult);
                            dataMap.clear();
                            dataMap = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        private List<Float> processEvents(Map<String, List<String>> dataMap) {
            List<Float> result = new ArrayList<>();
            List<String> resource = Arrays.asList(K8S_RESOURCE_TYPE_CPU, K8S_RESOURCE_TYPE_MEMORY, K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            Map<String, String> capacity = getMapData("Allocatable", ":", dataMap);
            Map<String, Map<String, String>> allocated = getUsedData("Allocated resources", resource, dataMap);
            Map<String, String> cpuUsed = allocated.get(K8S_RESOURCE_TYPE_CPU);
            Map<String, String> memoryUsed = allocated.get(K8S_RESOURCE_TYPE_MEMORY);
            Map<String, String> gpuUsed = allocated.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            result.add(convertToFloat(String.valueOf(cpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], true, false));
            result.add(convertToFloat(String.valueOf(capacity.get(K8S_RESOURCE_TYPE_CPU)), true, false));
            result.add(convertToFloat(String.valueOf(memoryUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], false, false));
            result.add(convertToFloat(String.valueOf(capacity.get(K8S_RESOURCE_TYPE_MEMORY)), false, false));
            if (gpuUsed != null) {
                result.add(Float.parseFloat(String.valueOf(gpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0].substring(0, 1)));
                result.add(convertToFloat(String.valueOf(capacity.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU)), true, false));
            } else {
                result.add(0f);
                result.add(0f);
            }
            return result;
        }

        private Map<String, String> getMapData(String keyword, String split, Map<String, List<String>> dataMap) {
            Map<String, String> result = new LinkedHashMap<>();
            List<String> dataList = dataMap.get(keyword);
            if (dataList != null) {
                String first = dataList.get(0);
                if (first.indexOf(":") + 1 < first.length()) {
                    first = first.substring(first.indexOf(":") + 1).trim();
                    dataList.set(0, first);
                }
                for (String data : dataList) {
                    String[] tmp = data.trim().split(split);
                    if (tmp.length > 1) {
                        result.put(tmp[0].trim(), tmp[1].trim());
                    }
                }
            }
            return result;
        }

        private Map<String, Map<String, String>> getUsedData(String keyword, List<String> keys, Map<String, List<String>> dataMap) {
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            if (dataMap.get(keyword) != null) {
                List<String> dataList = dataMap.get(keyword);
                boolean isData = false;
                for (String data : dataList) {
                    data = data.trim();
                    if (data.indexOf("--------") != -1) {
                        isData = true;
                        continue;
                    }
                    if (isData) {
                        String key = data.substring(0, data.indexOf(" "));
                        Map<String, String> map = processData(keys, key, data);
                        if (map != null) {
                            result.put(key, map);
                        }
                    }
                }
            }
            return result;
        }

        private Map<String, String> processData(List<String> keys, String key, String data) {
            Map<String, String> map = null;
            if (keys.contains(key) && (key.equals(K8S_RESOURCE_TYPE_CPU) || key.equals(K8S_RESOURCE_TYPE_MEMORY))) {
                map = new HashMap<>();
                map.put(K8S_RESOURCE_REQUESTS, data.substring(data.indexOf("   "), data.indexOf("(")).trim());
            } else if (keys.contains(key) && key.equals(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU)) {
                map = new HashMap<>();
                map.put(K8S_RESOURCE_REQUESTS, data.substring(data.indexOf("  ")).trim());
            }
            return map;
        }

        private static float convertToFloat(String value, boolean isCore, boolean isbyte) {
            if (isCore) {
                return convertToFloatForCore(value);
            } else {
                return convertToFloatForNoCore(value, isbyte);
            }
        }

        private static float convertToFloatForCore(String value) {
            String unit = value.substring(value.length() - 1);
            if (unit.equals("m")) {
                float result = Integer.parseInt(value.substring(0, value.length() - 1));
                return result / 1024;
            } else {
                return Integer.parseInt(value);
            }
        }

        private static float convertToFloatForNoCore(String value, boolean isbyte) {
            float result;
            if (value.contains("i")) {
                String unit = value.substring(value.length() - 2);
                result = Integer.parseInt(value.substring(0, value.length() - 2));
                if (isbyte) {
                    switch (unit) {
                        case "Gi":
                            result = result * 1024 * 1024 * 1024;
                            break;
                        case "Mi":
                            result = result * 1024 * 1024;
                            break;
                        case "Ki":
                            result = result * 1024;
                            break;
                        default:
                    }
                } else {
                    switch (unit) {
                        case "Gi":
                            break;
                        case "Mi":
                            result = result / 1024;
                            break;
                        case "Ki":
                            result = result / 1024 / 1024;
                            break;
                        default:
                    }
                }
            } else {
                result = Long.parseLong(value);
                if (!isbyte) {
                    result = result / 1024 / 1024 / 1024;
                }
            }
            return result;
        }

        private String getKubeConfigFilePath() {
            if (System.getProperty("os.name").contains("Windows")) {
                return KUBE_CONFIG_WINDOWS_PATH;
            } else {
                return KUBE_CONFIG_LINUX_PATH;
            }
        }

        private String getKubeConfigFileName(String filePath, long clusterId) {
            if (System.getProperty("os.name").contains("Windows")) {
                return String.format("%s\\%s_kubeconfig", filePath, String.valueOf(clusterId));
            } else {
                return String.format("%s/%s_kubeconfig", filePath, String.valueOf(clusterId));
            }
        }

        private void checkKubeconfigFile(Cluster cluster) {
            String clusterK8sConfig = cluster.getK8sConfig();
            String filePath = getKubeConfigFilePath();
            String fileName = getKubeConfigFileName(filePath, cluster.getId());

            File folder = new File(filePath);
            if (!folder.exists()) {
                try {
                    folder.mkdir();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            File file = new File(fileName);
            if (!file.exists()) {
                try {
                    file = new File(fileName);
                    try (FileWriter fw = new FileWriter(file, false)) {
                        fw.write(clusterK8sConfig);
                        fw.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                log.info("Kubeconfig file exists");
            }
        }
    }
}
