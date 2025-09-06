package com.georgk.unfold.web.assembler;

import com.georgk.unfold.domain.MessageThread;
import com.georgk.unfold.web.ThreadController;
import com.georgk.unfold.web.UserController;
import com.georgk.unfold.web.model.ThreadModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ThreadModelAssembler implements RepresentationModelAssembler<MessageThread, ThreadModel> {
    @Override
    public ThreadModel toModel(MessageThread entity) {
        List<UUID> participantIds = entity.getParticipants().stream().map(p -> p.getId()).toList();
        ThreadModel model = new ThreadModel(entity.getId(), entity.getType(), entity.getName(), participantIds, entity.getCreatedAt(), entity.getUpdatedAt());
        Link self = linkTo(methodOn(ThreadController.class).getThread(entity.getId())).withSelfRel();
        model.add(self);
        model.add(linkTo(methodOn(ThreadController.class).listMessages(entity.getId(), null, null)).withRel("messages"));
        model.add(linkTo(methodOn(ThreadController.class).postMessage(entity.getId(), null)).withRel("send-message"));
        participantIds.forEach(uid -> model.add(linkTo(methodOn(UserController.class).getUser(uid)).withRel("participant")));
        return model;
    }
}
