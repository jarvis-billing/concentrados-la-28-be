package com.co.jarvis.service;

import com.co.jarvis.dto.CompanyDto;
import com.co.jarvis.enums.EStatus;

public interface CompanyService extends BaseService<CompanyDto> {

    CompanyDto findByStatus(EStatus estado);
}
