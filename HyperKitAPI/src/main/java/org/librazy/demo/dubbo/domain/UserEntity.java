package org.librazy.demo.dubbo.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.AntPathMatcher;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table
public class UserEntity implements UserDetails, Serializable {

    private static final long serialVersionUID = -3527158508891454204L;

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    @Column(nullable = false)
    private Timestamp version;

    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    @Column
    private String nick;

    @Column
    private String avatar;

    @Column
    private String bio;

    @Column
    @OrderColumn
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>(Collections.singleton("ROLE_USER"));

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
    private SrpAccountEntity srpAccount;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.REMOVE}, fetch = FetchType.LAZY, mappedBy = "author", orphanRemoval = true)
    private List<BlogEntryEntity> blogEntries;

    @ManyToMany(mappedBy = "stargazers")
    private List<BlogEntryEntity> starredEntries;

    @ManyToMany(mappedBy = "followers")
    private List<UserEntity> following;

    @ManyToMany
    @JoinTable
    private List<UserEntity> followers;

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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean hasRole(String requirement) {
        return roles.contains(requirement);
    }

    public boolean matchRole(String requirement) {
        return roles.parallelStream().anyMatch(role -> new AntPathMatcher(".").match(role, requirement));
    }

    public Timestamp getVersion() {
        return version;
    }

    protected void setVersion(Timestamp version) {
        this.version = version;
    }

    public List<BlogEntryEntity> getBlogEntries() {
        return blogEntries;
    }

    public void setBlogEntries(List<BlogEntryEntity> blogEntries) {
        this.blogEntries = blogEntries;
    }

    public List<BlogEntryEntity> getStarredEntries() {
        return starredEntries;
    }

    public void setStarredEntries(List<BlogEntryEntity> starredEntries) {
        this.starredEntries = starredEntries;
    }

    public void addStarredEntries(BlogEntryEntity starred) {
        this.starredEntries.add(starred);
        starred.getStargazers().add(this);
    }

    public void removeStarredEntries(BlogEntryEntity starred) {
        this.starredEntries.remove(starred);
        starred.getStargazers().remove(this);
    }

    public List<UserEntity> getFollowing() {
        return following;
    }

    public void setFollowing(List<UserEntity> following) {
        this.following = following;
    }

    public List<UserEntity> getFollowers() {
        return followers;
    }

    public void setFollowers(List<UserEntity> followers) {
        this.followers = followers;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static UserEntity cast(UserDetails userDetails) {
        return (UserEntity) userDetails;
    }
}
