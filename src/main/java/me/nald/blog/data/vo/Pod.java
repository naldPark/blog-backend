package me.nald.blog.data.vo;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.nald.blog.util.CommonUtils;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
public class Pod {
    String name;
    String namespace;
    Map<String, String> labels;
    String age;
    Date runningTime;
    int containers;
    String status;
    String nodeName;


    public Pod(V1Pod pod) {
        V1ObjectMeta meta = pod.getMetadata();
        name = meta.getName();
        namespace = meta.getNamespace();
        age = CommonUtils.dataToAge(meta.getCreationTimestamp().toDate());
        runningTime = meta.getCreationTimestamp().toDate();
        containers = pod.getSpec().getContainers().size();
        status = pod.getStatus().getPhase();
        nodeName = pod.getSpec().getNodeName();
    }

}
