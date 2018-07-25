package org.librazy.demo.dubbo.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.*;
import org.librazy.demo.dubbo.service.BlogService;
import org.librazy.demo.dubbo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
@Api(value = "/blog", tags = "博客")
public class BlogController {

    private static Logger logger = LoggerFactory.getLogger(BlogController.class);

    private final UserService userService;

    private final BlogService blogService;

    @Autowired
    public BlogController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @ApiOperation("创建博文")
    @ApiResponses(
            @ApiResponse(code = 201, message = "成功创建博文", response = IdResult.class)
    )
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(value = "/blog/", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<IdResult> create(@RequestBody BlogEntry blogForm) throws IOException {
        UserEntity author = userService.loadUserByUsername(String.valueOf(blogForm.getAuthorId()));
        BlogEntryEntity blogEntryEntity = blogService.create(author, blogForm);
        return ResponseEntity.created(URI.create("/blog/" + blogEntryEntity.getId() + "/")).body(IdResult.from(blogEntryEntity.getId()));
    }

    @ApiOperation("更新博文")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功更新博文", response = BlogEntry.class),
            @ApiResponse(code = 400, message = "更新的博文ID不符"),
            @ApiResponse(code = 404, message = "找不到博文"),
    })
    @RequestMapping(value = "/blog/{entry:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT}, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<BlogEntry> update(@PathVariable BlogEntryEntity entry, @RequestBody BlogEntry blogForm) throws IOException {
        if ((blogForm.getId() != null) && (entry.getId() != blogForm.getId())) {
            throw new BadRequestException();
        }
        BlogEntryEntity update = blogService.update(entry, blogForm);
        return ResponseEntity.ok().body(BlogEntry.fromEntity(update));
    }

    @ApiOperation("删除博文")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "成功删除博文"),
            @ApiResponse(code = 404, message = "找不到博文"),
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @DeleteMapping("/blog/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#entry.author.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #entry.author.username))")
    public ResponseEntity<Void> delete(@PathVariable BlogEntryEntity entry) throws IOException {
        blogService.delete(entry);
        return ResponseEntity.noContent().build();
    }

    @ApiOperation("获取博文")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取博文", response = BlogEntry.class),
            @ApiResponse(code = 404, message = "找不到博文"),
    })
    @GetMapping("/blog/{entry:\\d+}/")
    public ResponseEntity<BlogEntry> get(@PathVariable BlogEntryEntity entry) {
        return ResponseEntity.ok(BlogEntry.fromEntity(entry));
    }

    @ApiOperation("搜索博文")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功搜索博文"),
    })
    @GetMapping("/blog/search")
    public ResponseEntity<List<BlogEntrySearchResult>> search(@RequestParam("q") String keyword) throws IOException {
        return ResponseEntity.ok(blogService.search(keyword));
    }

    @ApiOperation("获取所有博文分页列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户博文列表"),
    })
    @GetMapping("/blog/")
    public ResponseEntity<Page<BlogEntry>> getBlogPaged(@PageableDefault Pageable page) {
        Page<BlogEntryEntity> blogEntryEntities = blogService.getBlogPaged(page);
        Page<BlogEntry> blogEntries = blogEntryEntities.map(BlogEntry::fromEntity);
        return ResponseEntity.ok(blogEntries);
    }

    @ApiOperation("获取用户博文分页列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户博文列表"),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @GetMapping("/blog/user/{user:\\d+}/")
    public ResponseEntity<Page<BlogEntry>> getUserBlogPaged(@PathVariable UserEntity user, @PageableDefault Pageable page) {
        Page<BlogEntryEntity> blogEntryEntities = blogService.getUserBlogPaged(user, page);
        Page<BlogEntry> blogEntries = blogEntryEntities.map(BlogEntry::fromEntity);
        return ResponseEntity.ok(blogEntries);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(UserEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                try {
                    setValue(userService.loadUserByUsername(String.valueOf(text)));
                } catch (UsernameNotFoundException e){
                    logger.info("binder error when resovling user {}", text);
                    logger.info("exception: {}", e);
                    throw new NotFoundException(text);
                }
            }
        });

        binder.registerCustomEditor(BlogEntryEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(blogService.get(Long.valueOf(text)));
            }
        });
    }
}
