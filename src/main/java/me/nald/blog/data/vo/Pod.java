package me.nald.blog.data.vo;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.nald.blog.util.KubeUtils;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
public class Pod {
    String name;
    String namespace;
    Map<String, String> labels;
    String age;
    LocalDate runningTime;
    int containers;
    String status;
    String nodeName;


    public Pod(V1Pod pod) {
        V1ObjectMeta meta = pod.getMetadata();
        name = meta.getName();
        namespace = meta.getNamespace();
        age = KubeUtils.dataToAge(meta.getCreationTimestamp().toLocalDateTime());
        runningTime = meta.getCreationTimestamp().toLocalDate();
        containers = pod.getSpec().getContainers().size();
        status = pod.getStatus().getPhase();
        nodeName = pod.getSpec().getNodeName();
    }

}
