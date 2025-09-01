package com.co.jarvis.controller;

import com.co.jarvis.dto.LoginUserDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController extends GenericController<UserDto, UserService> {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService service;

    @Override
    protected UserService getService() {
        return service;
    }

    @PostMapping(value = "/loginUser")
    public ResponseEntity<?> loginUser(@RequestBody LoginUserDto loginUserDto) {
        logger.info("UserController -> loginUser");
        return ResponseEntity.ok(service.loginUser(loginUserDto.getNumberIdentity(), loginUserDto.getPassword()));
    }


}
