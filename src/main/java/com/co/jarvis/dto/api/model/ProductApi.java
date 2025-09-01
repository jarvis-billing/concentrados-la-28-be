package com.co.jarvis.dto.api.model;

import com.co.jarvis.enums.EVat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductApi implements Serializable {

    private String id;
    private String barcode;
    private String description;
    private EVat vatType;

}
