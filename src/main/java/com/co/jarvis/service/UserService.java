package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;

public interface UserService extends BaseService<UserDto> {

    UserDto loginUser(String numeroIdentidad, String password);
}
