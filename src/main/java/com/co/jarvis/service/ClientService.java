package com.co.jarvis.service;

import com.co.jarvis.dto.ClientDto;
import com.co.jarvis.util.SearchCriteriaClient;

import java.util.concurrent.CompletableFuture;

public interface ClientService extends BaseService<ClientDto> {

    CompletableFuture<ClientDto> findByCriteria(
            String idNumber, String documentType);

    ClientDto findByDocument(SearchCriteriaClient searchCriteriaClient);

    ClientDto getClientDefault();
}
