package com.co.jarvis.dto.api.model;

import com.co.jarvis.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderApi {

    private String id;
    private Long orderNumber;
    private LocalDateTime creationDate;
    private UserDto creationUser;
}
