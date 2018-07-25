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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Api(value = "/user/", tags = "用户")
@RequestMapping(produces = APPLICATION_JSON_VALUE)
@RestController
public class UserController {

    private final UserService userService;

    private final BlogService blogService;

    @Autowired
    public UserController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @ApiOperation("更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功更新用户信息", response = User.class),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @RequestMapping(value = "/user/{user:\\d+}/", method = {RequestMethod.PATCH, RequestMethod.PUT}, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<User> update(@PathVariable UserEntity user, @RequestBody User userForm) {
        if (userForm.getId() != null && user.getId() != userForm.getId() || userForm.getEmail() != null && !user.getEmail().equals(userForm.getEmail())
        ) {
            return ResponseEntity.badRequest().build();
        }
        UserEntity update = userService.update(user, userForm);
        return ResponseEntity.ok().body(User.fromEntity(update));
    }

    @ApiOperation("获取用户信息")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户信息", response = User.class),
            @ApiResponse(code = 404, message = "用户不存在")
    })
    @GetMapping("/user/{user:\\d+}/")
    public ResponseEntity<User> get(@PathVariable UserEntity user) {
        User userForm = User.fromEntity(user);
        return ResponseEntity.ok(userForm);
    }

    @ApiOperation("获取用户博文列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户博文列表"),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @GetMapping("/user/{user:\\d+}/blog/")
    public ResponseEntity<Set<BlogEntry>> getBlog(@PathVariable UserEntity user) {
        Set<BlogEntryEntity> blogEntryEntities = user.getBlogEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @ApiOperation("获取用户收藏博文列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户收藏博文列表"),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @GetMapping("/user/{user:\\d+}/star/")
    public ResponseEntity<Set<BlogEntry>> getStarEntity(@PathVariable UserEntity user) {
        Set<BlogEntryEntity> blogEntryEntities = user.getStarredEntries();
        Set<BlogEntry> blogEntries = blogEntryEntities.stream().map(BlogEntry::fromEntity).collect(Collectors.toSet());
        return ResponseEntity.ok(blogEntries);
    }

    @ApiOperation("获取用户关注列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户关注列表"),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @GetMapping("/user/{user:\\d+}/following/")
    public ResponseEntity<Set<User>> getFollowing(@PathVariable UserEntity user) {
        Set<UserEntity> userEntities = user.getFollowing();
        Set<User> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(User.fromEntity(u));
        }
        return ResponseEntity.ok(userForms);
    }

    @ApiOperation("获取用户粉丝列表")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "成功获取用户粉丝列表"),
            @ApiResponse(code = 404, message = "用户不存在"),
    })
    @GetMapping("/user/{user:\\d+}/follower/")
    public ResponseEntity<Set<User>> getFollower(@PathVariable UserEntity user) {
        Set<UserEntity> userEntities = user.getFollowers();
        Set<User> userForms = new HashSet<>();
        for (UserEntity u : userEntities) {
            userForms.add(User.fromEntity(u));
        }
        return ResponseEntity.ok(userForms);
    }

    @ApiOperation("收藏博文")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "点赞博文"),
            @ApiResponse(code = 400, message = "收藏自己的文章"),
            @ApiResponse(code = 404, message = "文章或用户不存在"),
            @ApiResponse(code = 409, message = "已经收藏了"),
    })
    @PutMapping("/user/{user:\\d+}/star/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<Void> addStar(@PathVariable UserEntity user, @PathVariable BlogEntryEntity entry) {
        if (user.getId() == entry.getAuthor().getId()) {
            throw new BadRequestException();
        }
        boolean addStarredEntries = userService.addStarredEntries(user, entry);
        if (addStarredEntries) {
            return ResponseEntity.created(URI.create("/user/" + user.getId() + "/star/" + entry.getId() + "/")).build();
        } else {
            throw new ConflictException();
        }
    }

    @ApiOperation("取消收藏博文")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "取消收藏博文"),
            @ApiResponse(code = 404, message = "文章或用户不存在"),
            @ApiResponse(code = 409, message = "未收藏"),
    })
    @DeleteMapping("/user/{user:\\d+}/star/{entry:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#user.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #user.username))")
    public ResponseEntity<Void> removeStar(@PathVariable UserEntity user, @PathVariable BlogEntryEntity entry) {
        boolean removeStarredEntries = userService.removeStarredEntries(user, entry);
        if (removeStarredEntries) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ConflictException();
        }
    }

    @ApiOperation("关注用户")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "关注用户"),
            @ApiResponse(code = 400, message = "不能关注自己"),
            @ApiResponse(code = 404, message = "用户不存在"),
            @ApiResponse(code = 409, message = "已经关注"),
    })
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

    @ApiOperation("取消关注用户")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "取消关注用户"),
            @ApiResponse(code = 404, message = "文章或用户不存在"),
            @ApiResponse(code = 409, message = "未关注"),
    })
    @DeleteMapping("/user/{follower:\\d+}/following/{following:\\d+}/")
    @PreAuthorize("hasRole('USER') && (#follower.username.equals(principal.username) || T(org.librazy.demo.dubbo.domain.UserEntity).cast(principal).matchRole(\"ADMIN.IMPERSONATE_\" + #follower.username))")
    public ResponseEntity<Void> deleteFollowing(@PathVariable UserEntity follower, @PathVariable UserEntity following) {
        boolean removeFollowing = userService.removeFollowing(follower, following);
        if (removeFollowing) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ConflictException();
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
