package com.co.jarvis.service;

import com.co.jarvis.dto.ExpenseDto;
import com.co.jarvis.dto.ExpensePageDto;
import java.util.List;

public interface ExpenseService {

    ExpenseDto create(ExpenseDto dto);

    ExpensePageDto list(String fromDate, String toDate, String category, Integer page, Integer size, String sort);

    List<String> listCategories();

    ExpenseDto update(String id, ExpenseDto dto);

    void delete(String id);
}
