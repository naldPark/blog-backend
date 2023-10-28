package me.nald.blog.model;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.*;
import me.nald.blog.util.Util;
import org.joda.time.DateTime;

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
//        labels = meta.getLabels();
        age = Util.dataToAge(meta.getCreationTimestamp().toDate());
        runningTime = meta.getCreationTimestamp().toDate();
        containers = pod.getSpec().getContainers().size();
        status = pod.getStatus().getPhase();
        nodeName = pod.getSpec().getNodeName();
    }

}
