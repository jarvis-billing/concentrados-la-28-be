package com.co.jarvis.entity;

import com.co.jarvis.dto.SaleDetailDto;
import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.enums.EBilling;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "SALES_BILLING")
public class Billing {

    @Id
    private String id;
    private String billNumber;
    private OffsetDateTime dateTimeRecord;
    private Client client;
    private Order order;
    private List<SaleDetailDto> saleDetails;
    private User creationUser;
    private BigDecimal subTotalSale;
    private BigDecimal receivedValue;
    private BigDecimal returnedValue;
    private BigDecimal totalIVAT;
    private BigDecimal totalBilling;
    private EBilling billingType;
    private List<EPaymentMethod> paymentMethods;
    private Boolean isReportInvoice;
    private EPaymentType saleType;
}

