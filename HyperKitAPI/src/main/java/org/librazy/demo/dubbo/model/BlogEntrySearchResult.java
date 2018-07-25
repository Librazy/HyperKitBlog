package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.util.StringJoiner;

public class BlogEntrySearchResult {

    @Positive
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

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

    protected BlogEntrySearchResult(){
    }

    public BlogEntrySearchResult(@Positive Long id, @NotBlank String title, @NotBlank String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BlogEntrySearchResult.class.getSimpleName() + "[", "]")
                       .add("id=" + id)
                       .add("title='" + title + "'")
                       .add("content='" + content + "'")
                       .toString();
    }
}
