package com.georgk.unfold.web;

import com.georgk.unfold.domain.Message;
import com.georgk.unfold.domain.MessageThread;
import com.georgk.unfold.service.ThreadService;
import com.georgk.unfold.web.assembler.MessageModelAssembler;
import com.georgk.unfold.web.assembler.ThreadModelAssembler;
import com.georgk.unfold.web.model.MessageModel;
import com.georgk.unfold.web.model.ThreadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping(path = "/", produces = MediaTypes.HAL_JSON_VALUE)
public class ThreadController {

    private final ThreadService service;
    private final ThreadModelAssembler threadAssembler;
    private final MessageModelAssembler messageAssembler;
    private final PagedResourcesAssembler<MessageThread> threadPagedAssembler;
    private final PagedResourcesAssembler<Message> messagePagedAssembler;

    public ThreadController(ThreadService service,
                            ThreadModelAssembler threadAssembler,
                            MessageModelAssembler messageAssembler,
                            PagedResourcesAssembler<MessageThread> threadPagedAssembler,
                            PagedResourcesAssembler<Message> messagePagedAssembler) {
        this.service = service;
        this.threadAssembler = threadAssembler;
        this.messageAssembler = messageAssembler;
        this.threadPagedAssembler = threadPagedAssembler;
        this.messagePagedAssembler = messagePagedAssembler;
    }

    public record DirectThreadRequest(UUID user1Id, UUID user2Id) {}
    public record GroupThreadRequest(List<UUID> participantIds, String name, UUID senderId, String initialMessage) {}
    public record MessageRequest(UUID senderId, String content) {}

    @PutMapping(path = "/threads/direct", consumes = "application/json")
    public ResponseEntity<ThreadModel> createOrGetDirect(@RequestBody DirectThreadRequest req) {
        MessageThread thread = service.getOrCreateDirectThread(req.user1Id(), req.user2Id());
        ThreadModel model = threadAssembler.toModel(thread);
        return ResponseEntity.ok(model);
    }

    @PostMapping(path = "/threads/group", consumes = "application/json")
    public ResponseEntity<ThreadModel> createGroup(@RequestBody GroupThreadRequest req) {
        MessageThread thread = service.createGroupThread(new LinkedHashSet<>(req.participantIds()), req.name());
        if (req.initialMessage() != null && !req.initialMessage().isBlank()) {
            if (req.senderId() == null) {
                throw new IllegalArgumentException("senderId is required when initialMessage is provided");
            }
            service.postMessage(thread.getId(), req.senderId(), req.initialMessage());
        }
        ThreadModel model = threadAssembler.toModel(thread);
        return ResponseEntity.created(URI.create(model.getRequiredLink("self").getHref())).body(model);
    }

    @GetMapping("/threads/{id}")
    public ThreadModel getThread(@PathVariable UUID id) {
        return threadAssembler.toModel(service.getThread(id));
    }

    @GetMapping("/threads/{id}/messages")
    public PagedModel<MessageModel> listMessages(@PathVariable UUID id,
                                                 @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                 @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> result = service.getMessages(id, pageable);
        return messagePagedAssembler.toModel(result, messageAssembler);
    }

    @PostMapping(path = "/threads/{id}/messages", consumes = "application/json")
    public ResponseEntity<MessageModel> postMessage(@PathVariable UUID id, @RequestBody MessageRequest req) {
        Message message = service.postMessage(id, req.senderId(), req.content());
        MessageModel model = messageAssembler.toModel(message);
        return ResponseEntity.created(URI.create(model.getRequiredLink("self").getHref())).body(model);
    }

    @GetMapping("/threads/{threadId}/messages/{messageId}")
    public MessageModel getMessage(@PathVariable UUID threadId, @PathVariable UUID messageId) {
        return messageAssembler.toModel(service.getMessage(threadId, messageId));
    }
}
