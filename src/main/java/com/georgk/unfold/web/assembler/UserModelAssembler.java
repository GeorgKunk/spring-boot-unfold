package com.georgk.unfold.web.assembler;

import com.georgk.unfold.domain.UserAccount;
import com.georgk.unfold.web.UserController;
import com.georgk.unfold.web.model.UserModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserAccount, UserModel> {
    @Override
    public UserModel toModel(UserAccount entity) {
        UserModel model = new UserModel(entity.getId(), entity.getUsername(), entity.getCreatedAt());
        Link self = linkTo(methodOn(UserController.class).getUser(entity.getId())).withSelfRel();
        model.add(self);
        model.add(linkTo(methodOn(UserController.class).listThreadsForUser(entity.getId(), null, null)).withRel("threads"));
        return model;
    }
}
