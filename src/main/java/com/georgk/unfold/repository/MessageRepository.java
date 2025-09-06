package com.georgk.unfold.repository;

import com.georgk.unfold.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @EntityGraph(attributePaths = {"sender", "thread"})
    Page<Message> findByThread_IdOrderByCreatedAtAsc(UUID threadId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "thread"})
    Optional<Message> findByIdAndThread_Id(UUID id, UUID threadId);
}
