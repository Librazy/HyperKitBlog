package org.librazy.demo.dubbo.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table
public class BlogEntryEntity implements Serializable {

    private static final long serialVersionUID = -3527158508891454204L;

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    @Column(nullable = false)
    private Timestamp version;

    @Column(nullable = false)
    private Timestamp publish;

    @NotBlank
    @Column(columnDefinition = "TINYTEXT")
    private String title;

    @NotBlank
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    
    @Column(columnDefinition = "BIGINT")
    private String simhash;

    @ManyToOne
    @JoinColumn(nullable = false)
    private UserEntity author;

    @ManyToMany
    @JoinTable
    private List<UserEntity> stargazers;

    protected BlogEntryEntity() {
    }

    public BlogEntryEntity(@NotNull UserEntity author) {
        this.author = author;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setSimhash(String simhash) {
        this.simhash = simhash;
    }
    
    
    public String getSimhash() {
        return simhash;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @NotNull
    public UserEntity getAuthor() {
        return author;
    }

    public void setAuthor(@NotNull UserEntity author) {
        this.author = author;
    }

    public List<UserEntity> getStargazers() {
        return stargazers;
    }

    public void setStargazers(List<UserEntity> stargazers) {
        this.stargazers = stargazers;
    }

    public void addStargazer(UserEntity stargazer) {
        this.stargazers.add(stargazer);
        stargazer.getStarredEntries().add(this);
    }

    public void removeStargazer(UserEntity stargazer) {
        this.stargazers.remove(stargazer);
        stargazer.getStarredEntries().remove(this);
    }

    public Timestamp getPublish() {
        return publish;
    }

    public void setPublish(Timestamp publish) {
        this.publish = publish;
    }
}
