package org.librazy.demo.dubbo.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table
public class UserEntity implements Serializable {

    private static final long serialVersionUID = -3527158508891454204L;

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    @Column
    private Timestamp version;

    @Column(unique = true)
    private String email;

    @Column
    private String nick;

    @Column
    private String avatar;

    @Column
    @ElementCollection
    private List<String> role = new ArrayList<>(Collections.singleton("ROLE_USER"));

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
    private SrpAccountEntity srpAccount;

    protected UserEntity() {
    }

    public UserEntity(String email, String nick) {
        this.email = email;
        this.nick = nick;
    }

    public UserEntity(long id, String email) {
        this.id = id;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SrpAccountEntity getSrpAccount() {
        return srpAccount;
    }

    public void setSrpAccount(SrpAccountEntity srpAccount) {
        this.srpAccount = srpAccount;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<String> getRole() {
        return role;
    }

    public void setRole(List<String> role) {
        this.role = role;
    }

    public Timestamp getVersion() {
        return version;
    }

    protected void setVersion(Timestamp version) {
        this.version = version;
    }
}
