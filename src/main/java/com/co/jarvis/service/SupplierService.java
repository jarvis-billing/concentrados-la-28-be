package com.co.jarvis.service;

import com.co.jarvis.dto.SupplierCreateDto;
import com.co.jarvis.dto.SupplierDto;
import com.co.jarvis.dto.SupplierUpdateDto;
import com.co.jarvis.enums.SupplierStatus;

import java.util.List;

public interface SupplierService {
    List<SupplierDto> list(String q, SupplierStatus status, Integer page, Integer size);
    long count(String q, SupplierStatus status);
    SupplierDto create(SupplierCreateDto dto);
    SupplierDto update(String id, SupplierUpdateDto dto);
    void updateStatus(String id, SupplierStatus status);
}
