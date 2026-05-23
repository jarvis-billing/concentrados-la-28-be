package com.co.jarvis.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "sequences")
public class SequenceDocument {

    @Id
    private String id;

    private long seq;
}
