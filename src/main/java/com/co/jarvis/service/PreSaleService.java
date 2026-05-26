package com.co.jarvis.service;

import com.co.jarvis.dto.presale.CreatePreSaleRequest;
import com.co.jarvis.dto.presale.PreSaleFilterDto;
import com.co.jarvis.entity.PreSale;

import java.util.List;

public interface PreSaleService {

    PreSale create(CreatePreSaleRequest request, String createdBy);

    PreSale findById(String id);

    List<PreSale> list(PreSaleFilterDto filter);

    PreSale cancel(String id, String cancelledBy);

    PreSale markAsBilled(String id, String billingId, String billedBy);
}
