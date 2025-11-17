package com.co.jarvis.entity;

import com.co.jarvis.enums.DocumentType;
import com.co.jarvis.enums.SupplierStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "SUPPLIERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "uq_doc_type_id_number", def = "{ 'documentType': 1, 'idNumber': 1 }", unique = true)
})
public class Supplier {
    @Id
    private String id;
    private String name;
    private DocumentType documentType;
    private String idNumber;
    private String phone;
    private String email;
    private String address;
    @Indexed
    private SupplierStatus status;
}
