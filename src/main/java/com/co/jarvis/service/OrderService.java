package com.co.jarvis.service;

import com.co.jarvis.dto.OrderDto;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.enums.EStatusOrder;
import com.co.jarvis.util.exception.FieldsException;

import java.util.List;

public interface OrderService extends BaseService<OrderDto> {

    Long nextOrderNumber();

    OrderDto findByOrderNumber(Long numeroOrden) throws FieldsException;

    List<OrderDto> findByOrderStatus(String statusOrder) throws FieldsException;

    PaginationDto<OrderDto> findByOrderStatus(int pageNumber, int pageSize, EStatusOrder statusOrder) throws FieldsException;

    OrderDto startOrder();

    OrderDto endOrder(Long numeroOrden);

    void cancelOrder(Long numeroOrden);

    void changeStatus(Long orderNumber, EStatusOrder statusOrder);
}
