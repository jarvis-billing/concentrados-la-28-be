package com.co.jarvis.service;

import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.BillingReportFilterDto;
import com.co.jarvis.dto.ProductSalesSummary;
import com.co.jarvis.util.exception.FieldsException;

import java.util.List;

public interface SaleService extends BaseService<BillingDto> {

    BillingDto findByBillingNumber(String numeroFactura);

    BillingDto importSaleOrder(Long numeroOrden) throws FieldsException;

    String getLastBillingNumber();

    byte[] printTicketBilling(BillingDto dto);

    List<BillingDto> findAllBilling(BillingReportFilterDto dto);

    List<ProductSalesSummary> getProductSalesSummary(
            BillingReportFilterDto dto
    );
}
