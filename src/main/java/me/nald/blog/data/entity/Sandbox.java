package me.nald.blog.data.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
public class Sandbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sandbox_seq")
    private Long seq;

    @Column(name = "deploy_name")
    private String deploymentName;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "is_default")
    private Boolean isDefault;

    @JsonBackReference
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account account;


    public static Sandbox createSandbox(Account account, String deploymentName, boolean isDefault){

        Sandbox sandbox = new Sandbox();
        sandbox.setNamespace("sandbox");
        sandbox.setAccount(account);
        sandbox.setDeploymentName(deploymentName);
        sandbox.setIsDefault(isDefault);
        return sandbox;

    }


}
