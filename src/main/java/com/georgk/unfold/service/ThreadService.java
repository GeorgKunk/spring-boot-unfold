package com.georgk.unfold.service;

import com.georgk.unfold.domain.Message;
import com.georgk.unfold.domain.MessageThread;
import com.georgk.unfold.domain.ThreadType;
import com.georgk.unfold.domain.UserAccount;
import com.georgk.unfold.repository.MessageRepository;
import com.georgk.unfold.repository.MessageThreadRepository;
import com.georgk.unfold.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ThreadService {

    private final UserAccountRepository userRepo;
    private final MessageThreadRepository threadRepo;
    private final MessageRepository messageRepo;

    public ThreadService(UserAccountRepository userRepo, MessageThreadRepository threadRepo, MessageRepository messageRepo) {
        this.userRepo = userRepo;
        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
    }

    public UserAccount createUser(String username) {
        Objects.requireNonNull(username, "username");
        userRepo.findByUsername(username).ifPresent(u -> {
            throw new BadRequestException("Username already exists: " + username);
        });
        return userRepo.save(new UserAccount(username));
    }

    @Transactional(readOnly = true)
    public UserAccount getUser(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> listUsers(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    public MessageThread getOrCreateDirectThread(UUID user1, UUID user2) {
        if (Objects.equals(user1, user2)) {
            throw new BadRequestException("Direct thread requires two distinct users");
        }
        UserAccount u1 = getUser(user1);
        UserAccount u2 = getUser(user2);

        String directKey = normalizeDirectKey(user1, user2);
        Optional<MessageThread> existing = threadRepo.findByTypeAndDirectKey(ThreadType.DIRECT, directKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        MessageThread thread = new MessageThread(ThreadType.DIRECT, null, directKey);
        thread.getParticipants().add(u1);
        thread.getParticipants().add(u2);
        return threadRepo.save(thread);
    }

    public MessageThread createGroupThread(Collection<UUID> participantIds, String name) {
        if (participantIds == null || participantIds.size() < 3) {
            throw new BadRequestException("Group thread requires at least 3 participants");
        }
        Set<UserAccount> users = participantIds.stream().map(this::getUser).collect(Collectors.toCollection(LinkedHashSet::new));
        MessageThread thread = new MessageThread(ThreadType.GROUP, name, null);
        thread.getParticipants().addAll(users);
        return threadRepo.save(thread);
    }

    public Message postMessage(UUID threadId, UUID senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content cannot be empty");
        }
        MessageThread thread = getThread(threadId);
        UserAccount sender = getUser(senderId);
        if (!thread.getParticipants().contains(sender)) {
            throw new BadRequestException("Sender is not a participant of the thread");
        }
        Message message = messageRepo.save(new Message(thread, sender, content));
        // update thread's updatedAt to reflect new activity
        thread.touch();
        return message;
    }

    @Transactional(readOnly = true)
    public MessageThread getThread(UUID id) {
        return threadRepo.findById(id).orElseThrow(() -> new NotFoundException("Thread not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<MessageThread> getThreadsForUser(UUID userId, Pageable pageable) {
        getUser(userId); // ensure exists
        return threadRepo.findDistinctByParticipants_IdOrderByUpdatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessages(UUID threadId, Pageable pageable) {
        getThread(threadId); // ensure exists
        return messageRepo.findByThread_IdOrderByCreatedAtAsc(threadId, pageable);
    }

    @Transactional(readOnly = true)
    public Message getMessage(UUID threadId, UUID messageId) {
        return messageRepo.findByIdAndThread_Id(messageId, threadId)
                .orElseThrow(() -> new NotFoundException("Message not found in thread"));
    }

    private static String normalizeDirectKey(UUID a, UUID b) {
        String s1 = a.toString();
        String s2 = b.toString();
        return (s1.compareTo(s2) < 0) ? s1 + ":" + s2 : s2 + ":" + s1;
        }
}
