package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.domain.repo.UserRepository;
import org.librazy.demo.dubbo.model.SrpBlogForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.sql.Timestamp;

@Service
public class BlogServiceImpl implements BlogService {

    private static Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository,UserRepository userRepository) {
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
    }

	@Override
	public Boolean deleteBlogById(Long id) {
		BlogEntryEntity blogEntryEntity=blogRepository.getOne(id);
		if(blogEntryEntity!=null)
			blogRepository.delete(blogEntryEntity);
		return true;
	}

	@Override
	public Boolean createBlog(SrpBlogForm blogForm) {
		
		String publishString=blogForm.getPublish();
		Timestamp publish=null;
		publish=Timestamp.valueOf(publishString);
		Date nowDate = new Date();
		UserEntity author=userRepository.getOne(blogForm.getAuthorId());
		
		BlogEntryEntity blogEntryEntity=new BlogEntryEntity(author);
		
		//blogEntryEntity.setId(blogForm.getId());
		blogEntryEntity.setContent(blogForm.getContent());
		blogEntryEntity.setTitle(blogForm.getTitle());
		blogEntryEntity.setPublish(publish);
		blogEntryEntity.setVersion(new Timestamp(nowDate.getTime()));
		return blogRepository.save(blogEntryEntity)==null;
	}

	@Override
	public Boolean updateBlog(SrpBlogForm blogForm) {

		String publishString=blogForm.getPublish();
		Timestamp publish=null;
		publish=Timestamp.valueOf(publishString);
		Date nowDate = new Date();
		UserEntity author=userRepository.getOne(blogForm.getAuthorId());
		
		BlogEntryEntity blogEntryEntity=new BlogEntryEntity(author);
		
		blogEntryEntity.setId(blogForm.getId());
		blogEntryEntity.setContent(blogForm.getContent());
		blogEntryEntity.setTitle(blogForm.getTitle());
		blogEntryEntity.setPublish(publish);
		blogEntryEntity.setVersion(new Timestamp(nowDate.getTime()));
		return blogRepository.save(blogEntryEntity)==null;
	}

	@Override
	public BlogEntryEntity getBlogById(Long id) {		
		return blogRepository.getOne(id);
	}

	@Override
	public List<UserEntity> listBlogStargazers(Long id) {
		return blogRepository.getOne(id).getStargazers();
	}
}
