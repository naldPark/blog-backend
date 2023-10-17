package me.nald.blog.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {
    private Long id;
    private String name;
    private String description;
    private String cspName;
    private int csp;
    private String defaultYn;
    private String deployableYn;
    private String k8sConfig;
    private Long owner;
    private String createdAt;
    private long resourceGroup;
    private int groupId;
    private String ingressUrl;
    private String servingLogYn;

    @Builder
    public Cluster(long id, String name, String description, String k8sConfig, Long owner, int csp, String defaultYn, String deployableYn, int groupId, String ingressUrl, String servingLogYn) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.k8sConfig = k8sConfig;
        this.csp = csp;
        this.owner = owner;
        this.defaultYn = defaultYn;
        this.deployableYn = deployableYn;
        this.groupId = groupId;
        this.ingressUrl = ingressUrl;
        this.servingLogYn = servingLogYn;
    }
}

