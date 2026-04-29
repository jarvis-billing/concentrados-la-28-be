package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.bankaccount.BankAccountDto;
import com.co.jarvis.dto.bankaccount.CreateBankAccountRequest;

import java.util.List;

public interface BankAccountService {

    List<BankAccountDto> listActive();

    List<BankAccountDto> listAll();

    BankAccountDto getById(String id);

    BankAccountDto create(CreateBankAccountRequest request, UserDto user);

    BankAccountDto update(String id, CreateBankAccountRequest request, UserDto user);

    void deactivate(String id, UserDto user);
}
