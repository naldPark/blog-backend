package me.nald.blog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.kubernetes.client.openapi.models.*;
import lombok.*;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
public class Node {
    String name;
    Map<String, String> annotations;
    Map<String, String> labels;
    DateTime creationTimestamp;
    V1NodeSpec spec;
    V1NodeStatus status;
    Map<String, Object> nodeSummary;
    String condition;

    public Node(V1Node node) {
        V1ObjectMeta meta = node.getMetadata();
        name = meta.getName();
        annotations = meta.getAnnotations();
        labels = meta.getLabels();
        creationTimestamp = meta.getCreationTimestamp();
        spec = node.getSpec();
        status = node.getStatus();
        Optional<V1NodeCondition> first = node.getStatus().getConditions().stream().skip(node.getStatus().getConditions().size() > 1 ? node.getStatus().getConditions().size() - 1 : 0).findFirst();
        condition = first.get().getType();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeVO {
        private DateTime created;
        private String name;
        private Map<String, String> address;
        private String os;
        private String osImage;
        private String kernelVersion;
        private String containerRuntime;
        private String kubeletVersion;
        private Map<String, String> annotations;
        private Map<String, String> labels;
        private String taints;
        private String conditions;
    }
}
