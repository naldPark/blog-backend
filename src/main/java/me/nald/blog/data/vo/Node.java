package me.nald.blog.data.vo;

import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
public class Node {
    String name;
    Map<String, String> labels;
    String createdDt;
    Map<String, Long> capacity;
    String condition;
    String percentMemory;
    String percentCpu;


    public Node(V1Node node, Map<String, Object> nodeUsage) {
        V1ObjectMeta meta = node.getMetadata();
        name = meta.getName();
        labels = meta.getLabels();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
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
}
