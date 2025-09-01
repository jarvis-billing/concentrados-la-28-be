package com.co.jarvis.service.impl;

import com.co.jarvis.config.security.JwtProvider;
import com.co.jarvis.dto.TokenLoginUser;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.User;
import com.co.jarvis.repository.UserRepository;
import com.co.jarvis.util.exception.PasswordUserBadException;
import com.co.jarvis.util.exception.UserNotFoundException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class LoginUserService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    GenericMapper<User, UserDto> mapper = new GenericMapper<>(User.class, UserDto.class);

    @Override
    public UserDetails loadUserByUsername(String userIdentity) throws UsernameNotFoundException {
        Optional<User> user = repository.findByNumberIdentity(userIdentity);

        if (user.isPresent()) {
            return new org.springframework.security.core.userdetails.User(userIdentity,
                    new BCryptPasswordEncoder().encode(user.get().getPassword()), new ArrayList<>());
        }

        throw new UsernameNotFoundException(MessageConstants.USER_PASSWORD_WRONG);
    }


    public TokenLoginUser authenticateAndGenerateToken(String userIdentity, String password) {
        Optional<User> user = repository.findByNumberIdentity(userIdentity);
        if (user.isEmpty()) {
            throw new UserNotFoundException("Él usuario no se encuentra registrado.");
        }

        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            throw new PasswordUserBadException("Contraseña incorrecta.");
        }

        UserDto userDto = mapper.mapToDto(user.get());
        userDto.setPassword(null);
        String token = jwtProvider.generateToken(userIdentity, userDto);

        return TokenLoginUser.builder()
                .token(token)
                .expireIn(jwtProvider.extractExpiration(token))
                .type("Bearer")
                .build();
    }

    public UserDto getUserLoginContext() {
        return (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
