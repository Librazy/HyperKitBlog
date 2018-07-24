package org.librazy.demo.dubbo.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.AntPathMatcher;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
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
    private Set<String> roles = Collections.singleton("ROLE_USER");

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
    private SrpAccountEntity srpAccount;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.REMOVE}, fetch = FetchType.LAZY, mappedBy = "author", orphanRemoval = true)
    private Set<BlogEntryEntity> blogEntries;

    @ManyToMany(mappedBy = "stargazers")
    private Set<BlogEntryEntity> starredEntries;

    @ManyToMany(mappedBy = "followers")
    private Set<UserEntity> following;

    @ManyToMany
    @JoinTable
    private Set<UserEntity> followers;

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

    public static UserEntity cast(UserDetails userDetails) {
        return (UserEntity) userDetails;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
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

    public Set<BlogEntryEntity> getBlogEntries() {
        return blogEntries;
    }

    public void setBlogEntries(Set<BlogEntryEntity> blogEntries) {
        this.blogEntries = blogEntries;
    }

    public Set<BlogEntryEntity> getStarredEntries() {
        return starredEntries;
    }

    public void setStarredEntries(Set<BlogEntryEntity> starredEntries) {
        this.starredEntries = starredEntries;
    }

    public boolean addStarredEntries(BlogEntryEntity starred) {
        starred.getStargazers().add(this);
        return this.starredEntries.add(starred);

    }

    public boolean removeStarredEntry(BlogEntryEntity starred) {
        starred.getStargazers().remove(this);
        return this.starredEntries.remove(starred);
    }

    public boolean addFollowers(UserEntity user) {
        user.getFollowing().add(this);
        return this.followers.add(user);
    }

    public boolean removeFollower(UserEntity user) {
        user.getFollowing().remove(this);
        return this.followers.remove(user);
    }

    public boolean addFollowing(UserEntity user) {
        user.getFollowers().add(this);
        return this.following.add(user);
    }

    public boolean removeFollowing(UserEntity user) {
        user.getFollowers().remove(this);
        return this.following.remove(user);
    }

    public Set<UserEntity> getFollowing() {
        return following;
    }

    public void setFollowing(Set<UserEntity> following) {
        this.following = following;
    }

    public Set<UserEntity> getFollowers() {
        return followers;
    }

    public void setFollowers(Set<UserEntity> followers) {
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
}
