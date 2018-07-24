package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.User;
import org.librazy.demo.dubbo.service.BlogService;
import org.librazy.demo.dubbo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private final UserService userService;

    private final BlogService blogService;

    @Autowired
    public UserController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @RequestMapping(value = "/user/{user:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT,
            RequestMethod.POST})
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<Void> update(@PathVariable UserEntity user, @RequestBody User userForm) {
        if (userForm.getId() != null && user.getId() != userForm.getId() || userForm.getEmail() != null && !user.getEmail().equals(userForm.getEmail())
        ) {
            return ResponseEntity.badRequest().build();
        }
        userService.update(user, userForm);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{user:\\d+}/")
    public ResponseEntity<User> get(@PathVariable UserEntity user) {
        User userForm = User.fromEntity(user);
        return ResponseEntity.ok(userForm);
    }

    @GetMapping("/user/{user:\\d+}/blog/")
    public ResponseEntity<Set<BlogEntry>> getBlog(@PathVariable UserEntity user) {
        Set<BlogEntryEntity> blogEntryEntities = user.getBlogEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @GetMapping("/user/{user:\\d+}/star/")
    public ResponseEntity<Set<BlogEntry>> getStarEntity(@PathVariable UserEntity user) {
        Set<BlogEntryEntity> blogEntryEntities = user.getStarredEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @GetMapping("/user/{user:\\d+}/following/")
    public ResponseEntity<Set<User>> getFollowing(@PathVariable UserEntity user) {
        Set<UserEntity> userEntities = user.getFollowing();
        Set<User> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(User.fromEntity(u));
        }
        return ResponseEntity.ok(userForms);
    }

    @GetMapping("/user/{user:\\d+}/follower/")
    public ResponseEntity<Set<User>> getFollower(@PathVariable UserEntity user) {
        Set<UserEntity> userEntities = user.getFollowers();
        Set<User> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(User.fromEntity(u));
        }
        return ResponseEntity.ok(userForms);
    }

    @PutMapping("/user/{user:\\d+}/star/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<Void> addStar(@PathVariable UserEntity user, @PathVariable BlogEntryEntity entry) {
        if (user.getId() == entry.getAuthor().getId()) {
            return ResponseEntity.badRequest().build();
        }
        boolean addStarredEntries = userService.addStarredEntries(user, entry);
        if (addStarredEntries) {
            return ResponseEntity.created(URI.create("/user/" + user.getId() + "/star/" + entry.getId() + "/")).build();
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @DeleteMapping("/user/{user:\\d+}/star/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<Void> removeStar(@PathVariable UserEntity user, @PathVariable BlogEntryEntity entry) {
        boolean removeStarredEntries = userService.removeStarredEntries(user, entry);
        if (removeStarredEntries) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @PutMapping("/user/{follower:\\d+}/following/{following:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#follower.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #follower.username))")
    public ResponseEntity<Void> addFollowing(@PathVariable UserEntity follower, @PathVariable UserEntity following) {
        if (follower.getId() == following.getId()) {
            return ResponseEntity.badRequest().build();
        }
        boolean addFollowing = userService.addFollowing(follower, following);
        if (addFollowing) {
            return ResponseEntity.created(URI.create("/user/" + follower.getId() + "/following/" + following.getId() + "/")).build();
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @DeleteMapping("/user/{follower:\\d+}/following/{following:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#follower.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #follower.username))")
    public ResponseEntity<Void> deleteFollowing(@PathVariable UserEntity follower, @PathVariable UserEntity following) {
        boolean removeFollowing = userService.removeFollowing(follower, following);
        if (removeFollowing) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(UserEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(userService.loadUserByUsername(String.valueOf(text)));
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
