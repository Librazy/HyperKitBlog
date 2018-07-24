package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.IdResult;
import org.librazy.demo.dubbo.service.BlogService;
import org.librazy.demo.dubbo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class BlogController {

    private final UserService userService;
    private final BlogService blogService;

    @Autowired
    public BlogController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @PostMapping("/blog/")
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<IdResult> create(@RequestBody BlogEntry blogForm) throws IOException {
        UserEntity author = userService.loadUserByUsername(String.valueOf(blogForm.getAuthorId()));
        BlogEntryEntity blogEntryEntity = blogService.create(author, blogForm);
        return ResponseEntity.created(URI.create("/blog/" + blogEntryEntity.getId() + "/")).body(IdResult.from(blogEntryEntity.getId()));
    }

    @RequestMapping(value = "/blog/{entry:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<Void> update(@PathVariable BlogEntryEntity entry, @RequestBody BlogEntry blogForm) throws IOException {
        if ((blogForm.getId() != null) && (entry.getId() != blogForm.getId())) {
            return ResponseEntity.badRequest().build();
        }
        blogService.update(entry, blogForm);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/blog/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#entry.author.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #entry.author.username))")
    public ResponseEntity<Void> delete(@PathVariable BlogEntryEntity entry) throws IOException {
        blogService.delete(entry);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blog/{entry:\\d+}/")
    public ResponseEntity<BlogEntry> get(@PathVariable BlogEntryEntity entry) {
        return ResponseEntity.ok(BlogEntry.fromEntity(entry));
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BlogEntryEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(blogService.get(Long.valueOf(text)));
            }
        });
    }
}
