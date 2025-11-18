package com.co.jarvis.service.impl;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.User;
import com.co.jarvis.repository.UserRepository;
import com.co.jarvis.service.UserService;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.exception.UserNotFoundException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    GenericMapper<User, UserDto> mapper = new GenericMapper<>(User.class, UserDto.class);

    @Override
    public UserDto loginUser(String idNumber, String password) {
        logger.info("UserServiceImpl -> loginUser");
        Optional<User> user = repository.findByNumberIdentity(idNumber);

        if (user.isEmpty()) {
            logger.error("UserServiceImpl -> loginUser -> Error: Usuario no encontrado");
            throw new UserNotFoundException(MessageConstants.RESOURCE_NOT_FOUND + ": " + idNumber);
        }
        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            logger.error("UserServiceImpl -> loginUser -> Error: Contraseña incorrecta");
            throw new IllegalArgumentException(MessageConstants.PASSWORD_WRONG);
        }
        return mapper.mapToDto(user.get());
    }

    @Override
    public List<UserDto> findAll() {
        logger.info("UserServiceImpl -> findAll");
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public UserDto findById(String id) {
        logger.info("UserServiceImpl -> findById");
        return mapper.mapToDto(repository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND)));
    }

    @Override
    public UserDto save(UserDto dto) {
        logger.info("UserServiceImpl -> save");
        isExistUser(dto);
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = repository.save(mapper.mapToEntity(dto));
        return mapper.mapToDto(user);
    }

    private void isExistUser(UserDto dto) {
        Optional<User> user = repository.findByNumberIdentity(dto.getNumberIdentity());
        if (user.isPresent()) {
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
        }
    }

    @Override
    public void deleteById(String id) {
        logger.info("UserServiceImpl -> deleteById");
        repository.deleteById(id);
    }

    @Override
    public UserDto update(UserDto dto, String id) {
        logger.info("UserServiceImpl -> update");
        try {
            Optional<User> opEntity = repository.findById(id);
            User entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));
            if (!entity.getId().equalsIgnoreCase(dto.getId())) {
                isExistUser(dto);
            }
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
            User userToUpdate = mapper.mapToEntity(dto);
            userToUpdate.setId(id);
            return mapper.mapToDto(repository.save(userToUpdate));
        } catch (ResourceNotFoundException e) {
            log.error("UserServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(e.getMessage());
        } catch (DuplicateRecordException e) {
            log.error("UserServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            log.error("UserServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }
}
