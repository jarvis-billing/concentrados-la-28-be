package com.co.jarvis.dto;

import com.co.jarvis.enums.EStatusOrder;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class OrderDto implements Serializable {

    private String id;
    private Long orderNumber;
    private List<ProductDto> products;
    private BigDecimal totalOrder;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private UserDto creationUser;
    private EStatusOrder status;
    private ClientDto client;


}
