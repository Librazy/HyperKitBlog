package org.librazy.demo.dubbo.model;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

public class BlogEntry implements Serializable {

    private static final long serialVersionUID = 3302270788285451101L;

    @Positive
    private Long id;

    private String title;

    private String content;

    private String simhash;

    private Long authorId;

    @Positive
    private Long publish;

    @Positive
    private Long updated;

    private List<RecommendBlogEntry> recommended;

    public static BlogEntry fromEntity(BlogEntryEntity entity) {
        BlogEntry blogEntry = new BlogEntry();
        blogEntry.setId(entity.getId());
        blogEntry.setAuthorId(entity.getAuthor().getId());
        blogEntry.setContent(entity.getContent());
        blogEntry.setTitle(entity.getTitle());
        blogEntry.setPublish(entity.getPublish().getTime());
        blogEntry.setUpdated(entity.getVersion().getTime());
        blogEntry.setSimhash(entity.getSimhash());
        return blogEntry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setContent(String content) {
        this.content = content;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    @NotNull
    public Long getPublish() {
        return publish;
    }

    public void setPublish(@NotNull Long publish) {
        this.publish = publish;
    }

    @NotNull
    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(@NotNull Long updated) {
        this.updated = updated;
    }

    public String getSimhash() {
        return simhash;
    }

    public void setSimhash(String simhash) {
        this.simhash = simhash;
    }

    public List<RecommendBlogEntry> getRecommended() {
        return recommended;
    }

    public void setRecommended(List<RecommendBlogEntry> recommended) {
        this.recommended = recommended;
    }
}
