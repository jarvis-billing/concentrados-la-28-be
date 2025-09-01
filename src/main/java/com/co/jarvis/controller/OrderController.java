 package com.co.jarvis.controller;

import com.co.jarvis.dto.OrderDto;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.enums.EStatusOrder;
import com.co.jarvis.service.OrderService;
import com.co.jarvis.util.exception.FieldsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

 @RestController
@RequestMapping(value = "/api/order", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController extends GenericController<OrderDto, OrderService> {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService service;

    @Override
    protected OrderService getService() {
        return service;
    }

    @GetMapping("/findByOrderNumber/{orderNumber}")
    public ResponseEntity<OrderDto> findByOrderNumber(@PathVariable Long orderNumber) throws FieldsException {
        logger.info("OrderController -> findByOrderNumber");
        OrderDto order = service.findByOrderNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

     @GetMapping("/findByOrderStatus/{orderStatus}")
     public ResponseEntity<List<OrderDto>> findByOrderStatus(@PathVariable String orderStatus) throws FieldsException {
         logger.info("OrderController -> findByOrderStatus");
         List<OrderDto> order = service.findByOrderStatus(orderStatus);
         return ResponseEntity.ok(order);
     }

     @GetMapping("/ordersFinished")
     public ResponseEntity<PaginationDto<OrderDto>> ordersFinished(
             @RequestParam(required = false, name = "page-number", defaultValue = "0") int pageNumber,
             @RequestParam(required = false, name = "page-size", defaultValue = "10") int pageSize
     ) throws FieldsException {
         logger.info("OrderController -> ordersFinished");
         PaginationDto<OrderDto> order = service.findByOrderStatus(pageNumber, pageSize, EStatusOrder.FINALIZADO);
         return ResponseEntity.ok(order);
     }

    @GetMapping("/startOrder")
    public ResponseEntity<OrderDto> startOrder() {
        logger.info("OrderController -> startOrder");
        OrderDto response = service.startOrder();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/endOrder/{orderNumber}")
    public ResponseEntity<OrderDto> endOrder(@PathVariable Long orderNumber){
        logger.info("OrderController -> endOrder");
        OrderDto order = service.endOrder(orderNumber);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/cancelOrder/{orderNumber}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderNumber){
        logger.info("OrderController -> cancelOrder");
        service.cancelOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
