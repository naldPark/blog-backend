package me.nald.blog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.*;
import lombok.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
public class Node {
    String name;
//    Map<String, String> annotations;
    Map<String, String> labels;
    String createdDt;
    Map<String, Long> capacity;
    String condition;
    String percentMemory;
    String percentCpu;


    public Node(V1Node node, Map<String, Object> nodeUsage) {
        V1ObjectMeta meta = node.getMetadata();
        name = meta.getName();
//        annotations = meta.getAnnotations();
        labels = meta.getLabels();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm");
        createdDt = dateFormat.format(meta.getCreationTimestamp().toDate());
        HashMap<String, Long> data = new HashMap<>();
        node.getStatus().getCapacity().forEach((strKey, strValue)->{
            data.put(strKey,strValue.getNumber().longValue());
        });
        capacity = data;
        Optional<V1NodeCondition> first = node.getStatus().getConditions().stream().skip(node.getStatus().getConditions().size() > 1 ? node.getStatus().getConditions().size() - 1 : 0).findFirst();
        condition = first.get().getType();
        percentMemory = (String) nodeUsage.get("percentMemory");
        percentCpu = (String) nodeUsage.get("percentCpu");
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeVO {
        private String createdDt;
        private String name;
        private Map<String, String> address;
        private String os;
        private String osImage;
        private String kernelVersion;
        private String containerRuntime;
        private String kubeletVersion;
//        private Map<String, String> annotations;
        private Map<String, String> labels;
        private String taints;
        private String conditions;
    }
}
