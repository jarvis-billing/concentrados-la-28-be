package com.co.jarvis.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SearchCriteriaClient {

    private String idNumber;
    private String documentType;

}
