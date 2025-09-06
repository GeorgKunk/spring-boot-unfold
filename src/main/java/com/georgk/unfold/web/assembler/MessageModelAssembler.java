package com.georgk.unfold.web.assembler;

import com.georgk.unfold.domain.Message;
import com.georgk.unfold.web.ThreadController;
import com.georgk.unfold.web.UserController;
import com.georgk.unfold.web.model.MessageModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MessageModelAssembler implements RepresentationModelAssembler<Message, MessageModel> {
    @Override
    public MessageModel toModel(Message entity) {
        MessageModel model = new MessageModel(entity.getId(), entity.getThread().getId(), entity.getSender().getId(), entity.getContent(), entity.getCreatedAt());
        model.add(linkTo(methodOn(ThreadController.class).getMessage(entity.getThread().getId(), entity.getId())).withSelfRel());
        model.add(linkTo(methodOn(ThreadController.class).getThread(entity.getThread().getId())).withRel("thread"));
        model.add(linkTo(methodOn(UserController.class).getUser(entity.getSender().getId())).withRel("sender"));
        return model;
    }
}
