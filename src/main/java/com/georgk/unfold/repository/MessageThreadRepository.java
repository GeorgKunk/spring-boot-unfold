package com.georgk.unfold.repository;

import com.georgk.unfold.domain.MessageThread;
import com.georgk.unfold.domain.ThreadType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    @Override
    @EntityGraph(attributePaths = {"participants"})
    Optional<MessageThread> findById(UUID id);

    @EntityGraph(attributePaths = {"participants"})
    Optional<MessageThread> findByTypeAndDirectKey(ThreadType type, String directKey);

    @EntityGraph(attributePaths = {"participants"})
    Page<MessageThread> findDistinctByParticipants_IdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
}
