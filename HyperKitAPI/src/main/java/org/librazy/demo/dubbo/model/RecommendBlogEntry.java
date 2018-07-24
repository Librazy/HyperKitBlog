package org.librazy.demo.dubbo.model;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.io.Serializable;

public class RecommendBlogEntry implements Serializable {

    private static final long serialVersionUID = 1066499180182941836L;

    @Positive
    private Long id;

    @NotBlank
    private String title;


    public static RecommendBlogEntry fromEntity(BlogEntryEntity entity) {
        RecommendBlogEntry recommendblogEntry = new RecommendBlogEntry();
        recommendblogEntry.setId(entity.getId());
        recommendblogEntry.setTitle(entity.getTitle());
        return recommendblogEntry;
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
}
