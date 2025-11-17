package com.co.jarvis.dto;

import com.co.jarvis.dto.api.model.OrderApi;
import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.enums.EBilling;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingDto implements Serializable {

    private String id;
    private String billNumber;
    private OffsetDateTime dateTimeRecord;
    private ClientDto client;
    private OrderApi order;
    private CompanyDto company;
    private List<SaleDetailDto> saleDetails;
    private UserDto creationUser;
    private BigDecimal subTotalSale;
    private BigDecimal receivedValue;
    private BigDecimal returnedValue;
    private BigDecimal totalIVAT;
    private BigDecimal totalBilling;
    private EBilling billingType;
    private List<EPaymentMethod> paymentMethods;
    private Boolean isReportInvoice;
    private EPaymentType saleType;
    private List<PaymentEntryDto> payments;
}
