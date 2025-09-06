package com.georgk.unfold.web;

import com.georgk.unfold.domain.MessageThread;
import com.georgk.unfold.domain.UserAccount;
import com.georgk.unfold.service.ThreadService;
import com.georgk.unfold.web.assembler.ThreadModelAssembler;
import com.georgk.unfold.web.assembler.UserModelAssembler;
import com.georgk.unfold.web.model.ThreadModel;
import com.georgk.unfold.web.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/users", produces = MediaTypes.HAL_JSON_VALUE)
public class UserController {

    private final ThreadService service;
    private final UserModelAssembler userAssembler;
    private final ThreadModelAssembler threadAssembler;
    private final PagedResourcesAssembler<MessageThread> pagedAssembler;
    private final PagedResourcesAssembler<UserAccount> userPagedAssembler;

    public UserController(ThreadService service,
                          UserModelAssembler userAssembler,
                          ThreadModelAssembler threadAssembler,
                          PagedResourcesAssembler<MessageThread> pagedAssembler,
                          PagedResourcesAssembler<UserAccount> userPagedAssembler) {
        this.service = service;
        this.userAssembler = userAssembler;
        this.threadAssembler = threadAssembler;
        this.pagedAssembler = pagedAssembler;
        this.userPagedAssembler = userPagedAssembler;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<UserModel> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        UserAccount created = service.createUser(username);
        UserModel model = userAssembler.toModel(created);
        return ResponseEntity.created(URI.create(model.getRequiredLink("self").getHref())).body(model);
    }

    @GetMapping("/{id}")
    public UserModel getUser(@PathVariable UUID id) {
        return userAssembler.toModel(service.getUser(id));
    }

    @GetMapping
    public PagedModel<UserModel> listUsers(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                           @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserAccount> users = service.listUsers(pageable);
        return userPagedAssembler.toModel(users, userAssembler);
    }

    @GetMapping("/{id}/threads")
    public PagedModel<ThreadModel> listThreadsForUser(@PathVariable UUID id,
                                                      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                      @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageThread> result = service.getThreadsForUser(id, pageable);
        return pagedAssembler.toModel(result, threadAssembler);
    }
}
