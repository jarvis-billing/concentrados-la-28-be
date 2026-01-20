package com.co.jarvis.service.impl;

import com.co.jarvis.dto.OrderDto;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.Order;
import com.co.jarvis.entity.User;
import com.co.jarvis.enums.EStatusOrder;
import com.co.jarvis.repository.OrderRepository;
import com.co.jarvis.service.OrderService;
import com.co.jarvis.util.DateTimeUtil;
import com.co.jarvis.util.exception.*;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mappers.PaginationMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    public static final String ND = ": El número de orden ya existe";

    GenericMapper<Order, OrderDto> mapper = new GenericMapper<>(Order.class, OrderDto.class);
    GenericMapper<User, UserDto> mapperUser = new GenericMapper<>(User.class, UserDto.class);
    PaginationMapper<Order, OrderDto> paginationMapper = new PaginationMapper<>(Order.class, OrderDto.class);


    @Autowired
    private OrderRepository repository;

    @Autowired
    private LoginUserService loginUserService;

    @Override
    public List<OrderDto> findAll() {
        logger.info("OrderServiceImpl -> findAll");
        List<Order> lstBd = repository.findAll();
        return mapper.mapToDtoList(lstBd);
    }

    @Override
    public OrderDto save(OrderDto dto) {
        logger.info("OrderServiceImpl -> save");
        try {
            validateOrderNumber(dto.getOrderNumber());
            dto.setCreationUser(loginUserService.getUserLoginContext());
            Order entityBd = mapper.mapToEntity(dto);
            entityBd = repository.save(entityBd);
            dto = mapper.mapToDto(entityBd);
            return dto;
        } catch (DuplicateRecordException e) {
            logger.error("OrderServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    @Override
    public OrderDto findById(String id) {
        logger.info("OrderServiceImpl -> findById");
        Optional<Order> opEntity = repository.findById(id);
        return opEntity.map(entity -> mapper.mapToDto(entity)).orElse(null);
    }

    @Override
    public void deleteById(String id) {
        logger.info("OrderServiceImpl -> deleteById");
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            logger.error("PersonaServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            logger.error("PersonaServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR);
        }
    }

    @Override
    public OrderDto update(OrderDto dto, String id) {
        logger.info("OrderServiceImpl -> update");
        try {
            Optional<Order> opEntity = repository.findById(id);

            Order entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));

            if (!entity.getOrderNumber().toString().equalsIgnoreCase(dto.getOrderNumber().toString())) {
                validateOrderNumber(entity.getOrderNumber());
            }
            return updatePresent(dto, entity.getId(), entity.getCreationDate());
        } catch (ResourceNotFoundException e) {
            logger.error("OrderServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND, e);
        } catch (DuplicateRecordException e) {
            logger.error("OrderServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }

    @Override
    public Long nextOrderNumber() {
        logger.info("OrderServiceImpl -> nextOrderNumber");
        Order entity = repository.findFirstByOrderByCreationDateDesc();
        if (entity == null) {
            return 1L;
        }
        return entity.getOrderNumber() + 1L;
    }

    @Override
    public OrderDto findByOrderNumber(Long orderNumber) throws FieldsException {
        logger.info("OrderServiceImpl -> findByOrderNumber");
        try {
            if (orderNumber == null || orderNumber < 0) {
                throw new FieldsException(MessageConstants.EMPTY_FIELDS, List.of("Número Orden"));
            }
            Order entity = repository.findFirstByOrderNumber(orderNumber);
            if (entity == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            return mapper.mapToDto(entity);
        } catch (FieldsException e) {
            logger.error("OrderServiceImpl -> findByOrderNumber -> ERROR: {}", "Número de orden no valido");
            throw new FieldsException(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.error("OrderServiceImpl -> findByOrderNumber -> ERROR: {}", "La Orden no se encuentra registrada");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> findByOrderNumber -> ERROR: {}", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

    @Override
    public List<OrderDto> findByOrderStatus(String statusOrder) throws FieldsException {
        logger.info("OrderServiceImpl -> findByOrderStatus");
        try {
            if (statusOrder == null || statusOrder.isEmpty()) {
                throw new FieldsException(MessageConstants.EMPTY_FIELDS, List.of("Estado Orden"));
            }
            User userLogin = mapperUser.mapToEntity(loginUserService.getUserLoginContext());
            List<Order> ordersOpens = repository.findByStatusAndCreationUser(
                    EStatusOrder.valueOf(statusOrder.toUpperCase()), userLogin);
            if (ordersOpens.isEmpty()){
                return Collections.emptyList();
            }
            return mapper.mapToDtoList(ordersOpens);
        } catch (FieldsException e) {
            logger.error("OrderServiceImpl -> findByOrderStatus -> ERROR: {}", "Número de orden no valido");
            throw new FieldsException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> findByOrderStatus -> ERROR: {}", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

    @Override
    public PaginationDto<OrderDto> findByOrderStatus(int pageNumber, int pageSize, EStatusOrder statusOrder) throws FieldsException {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Order> orders = repository.findByStatus(statusOrder, pageable);
        return paginationMapper.pageToPagination(orders);
    }

    @Override
    public OrderDto startOrder() {
        logger.info("OrderServiceImpl -> startOrder");
        try {
            Long orderNumber = nextOrderNumber();

            Order entityBd = new Order();
            entityBd.setOrderNumber(orderNumber);
            entityBd.setCreationDate(DateTimeUtil.nowLocalDateTime());
            entityBd.setStatus(EStatusOrder.INICIADO);
            entityBd.setCreationUser(mapperUser.mapToEntity(loginUserService.getUserLoginContext()));

            entityBd = repository.save(entityBd);
            return mapper.mapToDto(entityBd);
        } catch (DuplicateRecordException e) {
            logger.error("OrderServiceImpl -> startOrder -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> startOrder -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    @Override
    public OrderDto endOrder(Long orderNumber) {
        try {
            Order order = repository.findFirstByOrderNumber(orderNumber);
            if (order == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            order.setStatus(EStatusOrder.FINALIZADO);
            Order endOrder = repository.save(order);
            return mapper.mapToDto(endOrder);
        } catch (ResourceNotFoundException e) {
            logger.error("OrderServiceImpl -> finalizar -> ERROR: Orden no encontrada");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> finalizar -> ERROR {}:", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

    @Override
    public void cancelOrder(Long orderNumber) {
        try {
            Order order = repository.findFirstByOrderNumber(orderNumber);
            if (order == null) {
                logger.error("OrderServiceImpl -> cancelOrder -> ERROR: Orden no encontrada");
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            deleteById(order.getId());
        } catch (ResourceNotFoundException e) {
            logger.error("OrderServiceImpl -> cancelOrder -> ERROR: Orden no encontrada");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> cancelOrder -> ERROR: {}", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

    @Override
    public void changeStatus(Long orderNumber, EStatusOrder statusOrder) {
        try {
            Order order = repository.findFirstByOrderNumber(orderNumber);
            if (order == null) {
                logger.error("OrderServiceImpl -> changeStatus -> ERROR: Orden no encontrada");
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            order.setStatus(statusOrder);
            repository.save(order);
        } catch (ResourceNotFoundException e) {
            logger.error("OrderServiceImpl -> changeStatus -> ERROR: Orden no encontrada");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("OrderServiceImpl -> changeStatus -> ERROR: {}", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

    private OrderDto updatePresent(OrderDto dto, String id, LocalDateTime creacion) {
        logger.info("OrderServiceImpl -> updatePresent");
        Order entity = mapper.mapToEntity(dto);
        entity.setId(id);
        entity.setCreationDate(creacion);
        entity.setUpdateDate(DateTimeUtil.nowLocalDateTime());
        entity = repository.save(entity);
        return mapper.mapToDto(entity);
    }

    private void validateOrderNumber(Long orderNumber) {
        logger.info("OrderServiceImpl -> validateOrderNumber");
        Order entity = repository.findFirstByOrderNumber(orderNumber);
        if (entity != null) {
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR + ND);
        }
    }


}