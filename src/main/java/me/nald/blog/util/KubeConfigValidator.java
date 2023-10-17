package me.nald.blog.util;

import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class KubeConfigValidator {
    private KubeConfigValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean validate(KubeConfig kubeConfig) {
        if (kubeConfig == null) {
            throw new RuntimeException();
        }
        List<String> errorMessages = new ArrayList<>();
        // 구성 요소 체크
        String currentContext = kubeConfig.getCurrentContext();
        if (currentContext == null || currentContext.isEmpty()) {
            errorMessages.add("'current-context'가 없습니다.");
        }
        List<Object> contexts = kubeConfig.getContexts();
        if (contexts == null || contexts.isEmpty()) {
            errorMessages.add("'contexts'가 없습니다.");
        } else {
            errorMessages.addAll(validateContexts(contexts));
        }
        ArrayList<Object> clusters = kubeConfig.getClusters();
        if (clusters == null || clusters.isEmpty()) {
            errorMessages.add("'clusters'가 없습니다.");
        } else {
            errorMessages.addAll(validateClusters(clusters));
        }
        ArrayList<Object> users = kubeConfig.getUsers();
        if (users == null || users.isEmpty()) {
            errorMessages.add("'users'가 없습니다.");
        } else {
            errorMessages.addAll(validateUsers(users));
        }
        // current 요소 체크
        errorMessages.addAll(validateCurrents(kubeConfig, contexts, currentContext));
        if (!errorMessages.isEmpty()) {
            String joinedMessage = String.join(", ", errorMessages);
            String message = String.format("Cluster의 K8S-Config가 유효하지 않은 값으로 설정되어 있습니다. Cluster Owner 에게 문의하세요.(%s)", joinedMessage);
            new RuntimeException(message);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateContexts(List<Object> contexts) {
        List<String> errorMessages = new ArrayList<>();
        int index = 0;
        for (Object obj : contexts) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map != null ) {
                errorMessages.addAll(validateContextsElement(map, index));
            }
            index++;
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateContextsElement(Map<String, Object> contextsElement, int index) {
        List<String> errorMessages = new ArrayList<>();
        String name = (String) contextsElement.get("name");
        if (name == null || name.isEmpty()) {
            errorMessages.add(String.format("contexts[%d]에 'name'이 없습니다.", index));
        }
        Map<String, Object> context = (Map<String, Object>) contextsElement.get("context");
        if (context == null) {
            errorMessages.add(String.format("contexts[%d]에 'context'가 없습니다.", index));
        } else {
            String cluster = (String) context.get("cluster");
            if (cluster == null || cluster.isEmpty()) {
                errorMessages.add(String.format("contexts[%d].context에 'cluster'가 없습니다.", index));
            }
            String user = (String) context.get("user");
            if (user == null || user.isEmpty()) {
                errorMessages.add(String.format("contexts[%d].context에 'user'가 없습니다.", index));
            }
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateClusters(List<Object> clusters) {
        List<String> errorMessages = new ArrayList<>();
        int index = 0;
        for (Object obj : clusters) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map != null ) {
                errorMessages.addAll(validateClustersElement(map, index));
            }
            index++;
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateClustersElement(Map<String, Object> clustersElement, int index) {
        List<String> errorMessages = new ArrayList<>();
        String name = (String) clustersElement.get("name");
        if (name == null || name.isEmpty()) {
            errorMessages.add(String.format("clusters[%d]에 'name'이 없습니다.", index));
        }
        Map<String, Object> cluster = (Map<String, Object>) clustersElement.get("cluster");
        if (cluster == null) {
            errorMessages.add(String.format("clusters[%d]에 'cluster'가 없습니다.", index));
        } else {
            String server = (String) cluster.get("server");
            if (server == null || server.isEmpty()) {
                errorMessages.add(String.format("clusters[%d].cluster에 'server'가 없습니다.", index));
            }
            String certificateAuthorityData = (String) cluster.get("certificate-authority-data");
            if (certificateAuthorityData == null || certificateAuthorityData.isEmpty()) {
                errorMessages.add(String.format("clusters[%d].cluster에 'certificate-authority-data'가 없습니다.", index));
            }
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateUsers(List<Object> users) {
        List<String> errorMessages = new ArrayList<>();
        int index = 0;
        for (Object obj : users) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map != null ) {
                errorMessages.addAll(validateUsersElement(map, index));
            }
            index++;
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateUsersElement(Map<String, Object> usersElement, int index) {
        List<String> errorMessages = new ArrayList<>();
        String name = (String) usersElement.get("name");
        if (name == null || name.isEmpty()) {
            errorMessages.add(String.format("users[%d]에 'name'이 없습니다.", index));
        }
        Map<String, Object> user = (Map<String, Object>) usersElement.get("user");
        if (user == null) {
            errorMessages.add(String.format("users[%d]에 'user'가 없습니다.", index));
        }
        return errorMessages;
    }
    
    private static List<String> validateCurrents(KubeConfig kubeConfig, List<Object> contexts, String currentContext) {
        List<String> errorMessages = new ArrayList<>();
        // current context 구성 요소 체크
        if (contexts == null || currentContext == null) {
            return errorMessages;
        }
        Map<String, Object> context = findCurrentContext(contexts, currentContext);
        if (context == null) {
            errorMessages.add("contexts에 current-context가 없습니다.");
        } else {
            // current cluster 구성 요소 체크
            String server = kubeConfig.getServer();
            if (server == null || server.isEmpty()) {
                errorMessages.add("current cluster에 'server'가 없습니다.");
            }
            String certificateAuthorityData = kubeConfig.getCertificateAuthorityData();
            if (certificateAuthorityData == null || certificateAuthorityData.isEmpty()) {
                errorMessages.add("current cluster에 'certificate-authority-data'가 없습니다.");
            }
            // current user 구성 요소 체크
            String accessToken = kubeConfig.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                errorMessages.add("current user의 access token이 없습니다.");
            }
        }
        return errorMessages;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findCurrentContext(List<Object> contexts, String currentContextName) {
        if (contexts == null) {
            return null;
        }
        for (Object obj : contexts) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map != null && currentContextName.equals(map.get("name"))) {
                return (Map<String, Object>) map.get("context");
            }
        }
        return null;
    }
}
