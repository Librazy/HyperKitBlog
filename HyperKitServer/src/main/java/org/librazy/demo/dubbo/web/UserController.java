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
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

	private static final String STATUS = "status";

	private final UserService userService;
	private final BlogService blogService;

	@Autowired
	public UserController(UserService userService, BlogService blogService) {
		this.userService = userService;
		this.blogService = blogService;
	}

	@RequestMapping(value = "/user/{userId:\\d+}/", method = { RequestMethod.PATCH, RequestMethod.PUT,
			RequestMethod.POST })
	@PreAuthorize("hasRole('USER')") // TODO:user can only edit himself
	public ResponseEntity<Map<String, String>> update(@PathVariable long userId, @RequestBody UserForm userForm) {
		Map<String, String> result = new HashMap<>();
		if ((userForm.getId() != null) && (userId != userForm.getId())) {
			result.put(STATUS, "ERROR");
			return ResponseEntity.badRequest().body(result);
		}
		userForm.setId(userId);
		userService.update(userForm);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/user/{userId:\\d+}/")
	public ResponseEntity<UserForm> get(@PathVariable long userId) {
		UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
		UserForm userForm = new UserForm(userEntity);
		return ResponseEntity.ok(userForm);
	}

	@GetMapping("/user/{userId:\\d+}/blog/")
	public ResponseEntity<List<BlogEntry>> getBlog(@PathVariable long userId) {
		UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
		List<BlogEntryEntity> blogEntryEntities = userEntity.getBlogEntries();
		List<BlogEntry> blogEntries = new ArrayList<>();
		for (BlogEntryEntity b : blogEntryEntities) {
			BlogEntry blogEntry = new BlogEntry();
			blogEntry.setContent(b.getContent());
			blogEntry.setTitle(b.getTitle());
			blogEntries.add(blogEntry);
		}
		return ResponseEntity.ok(blogEntries);
	}
	
	@GetMapping("/user/{userId:\\d+}/star/")
	public ResponseEntity<List<BlogEntry>> getStarEntity(@PathVariable long userId) {
		UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
		List<BlogEntryEntity> blogEntryEntities = userEntity.getStarredEntries();
		List<BlogEntry> blogEntries = new ArrayList<>();
		for (BlogEntryEntity b : blogEntryEntities) {
			BlogEntry blogEntry = new BlogEntry();
			blogEntry.setContent(b.getContent());
			blogEntry.setTitle(b.getTitle());
			blogEntries.add(blogEntry);
		}
		return ResponseEntity.ok(blogEntries);
	}

	@GetMapping("/user/{userId:\\d+}/following/")
	public ResponseEntity<List<UserForm>> getFollowing(@PathVariable long userId) {
		UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
		List<UserEntity> userEntities = userEntity.getFollowing();
		List<UserForm> userForms = new ArrayList<>();
		for (UserEntity u : userEntities) {
			userForms.add(new UserForm(u));
		}
		return ResponseEntity.ok(userForms);
	}

	@GetMapping("/user/{userId:\\d+}/follower/")
	public ResponseEntity<List<UserForm>> getFollower(@PathVariable long userId) {
		UserEntity userEntity = userService.loadUserByUsername(String.valueOf(userId));
		List<UserEntity> userEntities = userEntity.getFollowers();
		List<UserForm> userForms = new ArrayList<>();
		for (UserEntity u : userEntities) {
			userForms.add(new UserForm(u));
		}
		return ResponseEntity.ok(userForms);
	}
	
	@PostMapping("/user/{userId:\\d+}/star/{entryId:\\d+}/")
	@PreAuthorize("hasRole('USER')") // TODO:user can only edit himself and cannot star blogs authored by himself
	public ResponseEntity<Map<String, String>> addStarEntity(@PathVariable long userId, @PathVariable long entryId) {
		Map<String, String> result = new HashMap<>();
		BlogEntryEntity blogEntryEntity=blogService.get(entryId);
		if ((blogEntryEntity.getAuthor() != null) && (userId == blogEntryEntity.getAuthor().getId())) {
			result.put(STATUS, "ERROR");
			return ResponseEntity.badRequest().body(result);
		}
		UserEntity userEntity=userService.loadUserByUsername(String.valueOf(userId));
		userService.addStarredEntries(userEntity, blogEntryEntity);
		return ResponseEntity.ok(result);
	}
	
	@DeleteMapping("/user/{userId:\\d+}/star/{entryId:\\d+}/")
	@PreAuthorize("hasRole('USER')")// TODO:user can only edit himself
	public ResponseEntity<Map<String, String>> removeStarEntity(@PathVariable long userId, @PathVariable long entryId) {
		Map<String, String> result = new HashMap<>();
		BlogEntryEntity blogEntryEntity=blogService.get(entryId);
		UserEntity userEntity=userService.loadUserByUsername(String.valueOf(userId));
		userService.removeStarredEntries(userEntity, blogEntryEntity);
		return ResponseEntity.ok(result);
	}
	
	@PostMapping("/user/{userId:\\d+}/follower/{followerId:\\d+}/")
	@PreAuthorize("hasRole('USER')") // TODO:user can only edit himself and cannot follow himself
	public ResponseEntity<Map<String, String>> addFollowing(@PathVariable long userId, @PathVariable long followerId) {
		Map<String, String> result = new HashMap<>();
		if (userId==followerId) {
			result.put(STATUS, "ERROR");
			return ResponseEntity.badRequest().body(result);
		}
		UserEntity followed=userService.loadUserByUsername(String.valueOf(followerId));
		UserEntity following=userService.loadUserByUsername(String.valueOf(userId));
		userService.addFollowing(following, followed);
		return ResponseEntity.ok(result);
	}
	
	@DeleteMapping("/user/{userId:\\d+}/follower/{followerId:\\d+}/")
	@PreAuthorize("hasRole('USER')") // TODO:user can only edit himself
	public ResponseEntity<Map<String, String>> deleteFollowing(@PathVariable long userId, @PathVariable long followerId) {
		Map<String, String> result = new HashMap<>();
		UserEntity followed=userService.loadUserByUsername(String.valueOf(followerId));
		UserEntity following=userService.loadUserByUsername(String.valueOf(userId));
		userService.removeFollowing(following, followed);
		return ResponseEntity.ok(result);
	}
}
