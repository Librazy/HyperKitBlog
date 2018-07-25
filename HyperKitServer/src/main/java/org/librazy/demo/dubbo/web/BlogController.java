package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.librazy.demo.dubbo.service.BlogService;
import org.librazy.demo.dubbo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BlogController {

    private static final String STATUS = "status";

    private final UserService userService;
    private final BlogService blogService;

    @Autowired
    public BlogController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @PostMapping("/blog/")
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<Map<String, String>> create(@RequestBody BlogEntry blogForm) throws IOException {
        Map<String, String> result = new HashMap<>();
        UserEntity author = userService.loadUserByUsername(String.valueOf(blogForm.getAuthorId()));
        BlogEntryEntity blogEntryEntity = blogService.create(author, blogForm);
        result.put(STATUS, "OK");
        result.put("id", String.valueOf(blogEntryEntity.getId()));
        return ResponseEntity.status(201).header("Location", "/blog/" + blogEntryEntity.getId() + "/").body(result);
    }

    @RequestMapping(value = "/blog/{entryId:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('USER') && (#blogForm.authorId.toString().equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #blogForm.authorId))")
    public ResponseEntity<Map<String, String>> update(@PathVariable long entryId, @RequestBody BlogEntry blogForm) throws IOException {
        Map<String, String> result = new HashMap<>();
        if ((blogForm.getId() != null) && (entryId != blogForm.getId())) {
            result.put(STATUS, "ERROR");
            return ResponseEntity.badRequest().body(result);
        }
        blogForm.setId(entryId);
        blogService.update(blogForm);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/blog/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#entry.author.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #entry.author.username))")
    public ResponseEntity<Map<String, String>> delete(@PathVariable BlogEntryEntity entry) throws IOException {
        blogService.delete(entry);
        Map<String, String> result = new HashMap<>();
        result.put(STATUS, "OK");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/blog/{entry:\\d+}/")
    public ResponseEntity<BlogEntry> get(@PathVariable BlogEntryEntity entry) {
        return ResponseEntity.ok(BlogEntry.fromEntity(entry));
    }

    @GetMapping("/blog/search")
    public ResponseEntity<List<BlogEntrySearchResult>> search(@RequestParam("q") String keyword) throws IOException {
        return ResponseEntity.ok(blogService.search(keyword));
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
