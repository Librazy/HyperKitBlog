package org.librazy.demo.dubbo.model;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.Date;

public class BlogEntry implements Serializable {

    private static final long serialVersionUID = 1066499180182941836L;

    @Positive
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @Positive
    private Long authorId;

    @NotNull
    @Positive
    private Long publish = new Date().getTime();

    @NotNull
    @Positive
    private Long updated = new Date().getTime();

    public static BlogEntry fromEntity(BlogEntryEntity entity) {
        BlogEntry blogEntry = new BlogEntry();
        blogEntry.setId(entity.getId());
        blogEntry.setAuthorId(entity.getAuthor().getId());
        blogEntry.setContent(entity.getContent());
        blogEntry.setTitle(entity.getTitle());
        blogEntry.setPublish(entity.getPublish().getTime());
        blogEntry.setUpdated(entity.getVersion().getTime());
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
}
