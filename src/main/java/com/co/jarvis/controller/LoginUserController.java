package com.co.jarvis.controller;

import com.co.jarvis.dto.LoginUserDto;
import com.co.jarvis.dto.TokenLoginUser;
import com.co.jarvis.service.impl.LoginUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "https://34kn8bds-4200.use2.devtunnels.ms")
public class LoginUserController {

    @Autowired
    private LoginUserService loginUserService;

    @PostMapping("/login")
    public ResponseEntity<TokenLoginUser> login(@RequestBody LoginUserDto loginUser) {
        TokenLoginUser tokenLoginUser = loginUserService.authenticateAndGenerateToken(loginUser.getNumberIdentity(), loginUser.getPassword());
        return ResponseEntity.ok(tokenLoginUser);
    }

}
