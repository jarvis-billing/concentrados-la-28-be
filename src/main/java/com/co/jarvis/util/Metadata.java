package com.co.jarvis.util;

import com.co.jarvis.dto.UserDto;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Metadata implements Serializable {

    private LocalDateTime dateCreation;
    private LocalDateTime dateUpdateLast;
    private UserDto userCreation;
    private UserDto userModify;
}
