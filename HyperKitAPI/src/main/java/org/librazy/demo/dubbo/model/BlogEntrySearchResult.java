package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.StringJoiner;

public class BlogEntrySearchResult {

    @Positive
    @NotNull
    private Long id;

    @NotBlank
    @NotNull
    private String title;

    @NotBlank
    @NotNull
    private String content;

    @NotNull
    public Long getId() {
        return id;
    }

    public void setId(@NotNull Long id) {
        this.id = id;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    @NotNull
    public String getContent() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
    }

    public BlogEntrySearchResult(@NotNull @Positive Long id, @NotNull @NotBlank String title, @NotNull @NotBlank String content) {
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
