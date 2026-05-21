package com.co.jarvis.service;

import com.co.jarvis.dto.MerchandiseReturnDto;
import com.co.jarvis.dto.ReturnFilterDto;

import java.util.List;

public interface MerchandiseReturnService {

    MerchandiseReturnDto createSaleReturn(MerchandiseReturnDto dto, String userId);

    MerchandiseReturnDto createPurchaseReturn(MerchandiseReturnDto dto, String userId);

    MerchandiseReturnDto findById(String id);

    MerchandiseReturnDto findByReturnNumber(String returnNumber);

    List<MerchandiseReturnDto> list(ReturnFilterDto filter);

    MerchandiseReturnDto cancelReturn(String id, String cancelReason, String userId);
}
