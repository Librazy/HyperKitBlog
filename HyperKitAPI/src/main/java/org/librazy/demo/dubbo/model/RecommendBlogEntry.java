package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.io.Serializable;

public class RecommendBlogEntry implements Serializable {

    private static final long serialVersionUID = 1066499180182941836L;

    @Positive
    private Long id;

    @NotBlank
    private String title;


    public RecommendBlogEntry(@Positive Long id, @NotBlank String title) {
		super();
		this.id = id;
		this.title = title;
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
