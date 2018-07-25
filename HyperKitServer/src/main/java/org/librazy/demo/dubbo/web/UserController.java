package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.UserForm;
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

    @RequestMapping(value = "/user/{userId:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT,
            RequestMethod.POST})
    @PreAuthorize("hasRole('USER') && (#userId.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #userId))")
    public ResponseEntity<Void> update(@PathVariable long userId, @RequestBody UserForm userForm) {
        if ((userForm.getId() != null) && (userId != userForm.getId())) {
            return ResponseEntity.badRequest().build();
        }
        userForm.setId(userId);
        userService.update(userForm);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId:\\d+}/")
    public ResponseEntity<UserForm> get(@PathVariable long userId) {
        UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
        UserForm userForm = new UserForm(userEntity);
        return ResponseEntity.ok(userForm);
    }

    @GetMapping("/user/{userId:\\d+}/blog/")
    public ResponseEntity<Set<BlogEntry>> getBlog(@PathVariable long userId) {
        UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
        Set<BlogEntryEntity> blogEntryEntities = userEntity.getBlogEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @GetMapping("/user/{userId:\\d+}/star/")
    public ResponseEntity<Set<BlogEntry>> getStarEntity(@PathVariable long userId) {
        UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
        Set<BlogEntryEntity> blogEntryEntities = userEntity.getStarredEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @GetMapping("/user/{userId:\\d+}/following/")
    public ResponseEntity<Set<UserForm>> getFollowing(@PathVariable long userId) {
        UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
        Set<UserEntity> userEntities = userEntity.getFollowing();
        Set<UserForm> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(new UserForm(u));
        }
        return ResponseEntity.ok(userForms);
    }

    @GetMapping("/user/{userId:\\d+}/follower/")
    public ResponseEntity<Set<UserForm>> getFollower(@PathVariable long userId) {
        UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
        Set<UserEntity> userEntities = userEntity.getFollowers();
        Set<UserForm> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(new UserForm(u));
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
