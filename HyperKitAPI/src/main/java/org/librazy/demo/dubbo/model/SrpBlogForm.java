package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class SrpBlogForm implements Serializable {

    private static final long serialVersionUID = 1066499180182941836L;

    @NotBlank
    private Long id;
    
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private Long authorId;

    @NotBlank
    private String publish;

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

	public String getPublish() {
		return publish;
	}

	public void setPublish(String publish) {
		this.publish = publish;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
    
    

}
