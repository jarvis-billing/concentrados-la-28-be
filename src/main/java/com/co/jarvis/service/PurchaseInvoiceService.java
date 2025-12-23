package com.co.jarvis.service;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;

import java.util.List;

public interface PurchaseInvoiceService {
    
    List<PurchaseInvoiceDto> list(PurchaseFilterDto filter);
    
    PurchaseInvoiceDto findById(String id);
    
    PurchaseInvoiceDto create(PurchaseInvoiceDto dto);
    
    PurchaseInvoiceDto update(String id, PurchaseInvoiceDto dto);
    
    void deleteById(String id);
}
