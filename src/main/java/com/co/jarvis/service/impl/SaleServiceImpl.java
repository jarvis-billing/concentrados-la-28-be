package com.co.jarvis.service.impl;

import com.co.jarvis.dto.*;
import com.co.jarvis.dto.api.model.OrderApi;
import com.co.jarvis.dto.api.model.ProductApi;
import com.co.jarvis.entity.Billing;
import com.co.jarvis.enums.EStatus;
import com.co.jarvis.enums.EStatusOrder;
import com.co.jarvis.enums.EVat;
import com.co.jarvis.repository.BillingRepository;
import com.co.jarvis.service.*;
import com.co.jarvis.util.exception.*;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import com.co.jarvis.util.reports.ReportExporter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.lang.NumberFormatException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

@Slf4j
@Service
public class SaleServiceImpl implements SaleService {

    private static final Logger logger = LoggerFactory.getLogger(SaleServiceImpl.class);
    public static final String REPORT_TICKET_BILLING = "ticket_billing";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private BillingRepository repository;

    @Autowired
    private ProductVatTypeService productVatTypeService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ReportExporter reportExporter;

    @Autowired
    private MongoTemplate mongoTemplate;


    GenericMapper<Billing, BillingDto> mapper
            = new GenericMapper<>(Billing.class, BillingDto.class);

    @Override
    public BillingDto findByBillingNumber(String billingNumber) {
        logger.info("SaleServiceImpl -> findByBillingNumber");
        Billing billing = repository.findByBillNumber(billingNumber);
        if (billing == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        }
        return mapper.mapToDto(billing);
    }

    @Override
    public List<BillingDto> findAll() {
        log.info("SaleServiceImpl -> findAll");
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public BillingDto findById(String id) {
        log.info("SaleServiceImpl -> findById");
        return mapper.mapToDto(repository.findById(id).orElse(new Billing()));
    }

    @Override
    public BillingDto save(BillingDto dto) {
        log.info("SaleServiceImpl -> save");
        try {
            Billing billing = repository.findByBillNumber(dto.getBillNumber());
            if (billing != null) {
                throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
            }
            Billing venta = mapper.mapToEntity(dto);
            Long orderNumber = dto.getOrder().getOrderNumber();
            if (orderNumber != null && orderNumber.intValue() > 0) {
                orderService.changeStatus(orderNumber, EStatusOrder.FACTURADO);
            }

            return mapper.mapToDto(repository.save(venta));
        } catch (DuplicateRecordException e) {
            logger.error("SaleServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR, e.getCause());
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("SaleServiceImpl -> deleteById");
        repository.deleteById(id);
    }

    @Override
    public BillingDto update(BillingDto dto, String id) {
        log.info("SaleServiceImpl -> update");
        try {
            Billing billing = repository.findByBillNumber(dto.getBillNumber());
            if (billing == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            dto.setId(id);
            Billing entity = mapper.mapToEntity(dto);
            return mapper.mapToDto(repository.save(entity));
        } catch (ResourceNotFoundException e) {
            logger.error("SaleServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e.getCause());
        }
    }

    @Override
    public BillingDto importSaleOrder(Long orderNumber) throws FieldsException {
        logger.info("SaleServiceImpl -> importSaleOrder");
        try {
            if (orderNumber == null || orderNumber < 0) {
                throw new FieldsException(MessageConstants.EMPTY_FIELDS, List.of("Número Orden"));
            }

            OrderDto orderDto = orderService.findByOrderNumber(orderNumber);
            if (orderDto == null || orderDto.getProducts().isEmpty()) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            validateStatus(orderDto);
            return buildBilling(orderDto);
        } catch (IllegalStateException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        } catch (FieldsException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new FieldsException(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new ResourceNotFoundException(e.getMessage());
        } catch (ResourceEndException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new ResourceEndException(e.getMessage());
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    @Override
    public String getLastBillingNumber() {
        return generatedBillingNumber();
    }

    @Override
    public byte[] printTicketBilling(BillingDto dto) {
        logger.info("SaleServiceImpl -> printTicketBilling");
        try {
            Billing billing = mapper.mapToEntity(dto);
            return reportExporter.exportToPdf(billing, REPORT_TICKET_BILLING);
        } catch (JRException | FileNotFoundException e) {
            log.error(e.getCause().getMessage());
        }
        return null;
    }

    @Override
    public List<BillingDto> findAllBilling(BillingReportFilterDto dto) {
        try {
            logger.info("SaleServiceImpl -> findAllBilling");
            // Crear una lista de criterios para los filtros dinámicos
            List<Criteria> criteriaList = new ArrayList<>();

            // Filtro por rango de fechas
            if (dto.hasFilterDate()) {
                criteriaList.add(Criteria.where("dateTimeRecord")
                        .gte(dto.getToDate().atStartOfDay())
                        .lte(dto.getFromDate().atTime(LocalTime.now())));
            }

            // Filtro por numero de factura
            if (dto.getBillNumber() != null && !dto.getBillNumber().isEmpty()) {
                criteriaList.add(Criteria.where("billNumber").regex(".*" + dto.getBillNumber() + ".*", "i"));
            }

            // Filtro por usuario de venta
            if (dto.getUserSale() != null && !dto.getUserSale().isEmpty()) {
                criteriaList.add(Criteria.where("order.creationUser.numberIdentity").is(dto.getUserSale()));
            }

            // Filtro por cliente
            if (dto.getClient() != null && !dto.getClient().isEmpty()) {
                criteriaList.add(Criteria.where("client.idNumber").is(dto.getClient()));
            }

            // Construir el criterio final combinando todas las condiciones con AND
            Criteria finalCriteria = new Criteria();
            if (!criteriaList.isEmpty()) {
                finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
            }

            // Crear la consulta utilizando el criterio
            Query query = new Query(finalCriteria);
            query.with(Sort.by(Sort.Direction.DESC, "dateTimeRecord"));

            // Ejecutar la consulta y mapear los resultados a BillingDto
            List<Billing> results = mongoTemplate.find(query, Billing.class);

            return mapper.mapToDtoList(results);
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> findAllBilling -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    @Override
    public List<ProductSalesSummary> getProductSalesSummary(BillingReportFilterDto dto) {
        if (dto.hasFilterDate()) {
            return repository.getProductSalesSummaryByDate(
                    dto.getFromDate().atStartOfDay(), dto.getToDate().atTime(LocalTime.now()));
        }

        throw new RuntimeException("not filter data for products summary");
    }

    private static void validateStatus(OrderDto orderDto) {
        if (orderDto.getStatus() == null || orderDto.getStatus().equals(EStatusOrder.INICIADO)) {
            throw new ResourceEndException(format(MessageConstants.ORDER_NOT_FINISHED, orderDto.getOrderNumber(),
                    orderDto.getCreationUser().getFullName()));
        }
    }

    private BillingDto buildBilling(OrderDto orderDto) {
        List<SaleDetailDto> saleDetailDtos = getSaleDetail(orderDto);
        return BillingDto.builder()
                .billNumber(generatedBillingNumber())
                .client(getClientOrder(orderDto))
                .order(OrderApi.builder()
                        .id(orderDto.getId())
                        .orderNumber(orderDto.getOrderNumber())
                        .creationDate(orderDto.getCreationDate())
                        .creationUser(orderDto.getCreationUser())
                        .build())
                .dateTimeRecord(orderDto.getCreationDate())
                .saleDetails(saleDetailDtos)
                .subTotalSale(subTotalBill(saleDetailDtos))
                .totalIVAT(ivaTotalBill(saleDetailDtos))
                .company(companyService.findByStatus(EStatus.ACTIVO))
                .build();
    }

    private ClientDto getClientOrder(OrderDto order) {
        ClientDto clientDefault = clientService.getClientDefault();
        return order.getClient().getIdNumber().isBlank() ? clientDefault : order.getClient();
    }

    private String generatedBillingNumber() {
        CompanyDto companyDto = companyService.findByStatus(EStatus.ACTIVO);
        if (companyDto == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND + " - Empresa no Encontrada");
        }

        BillingConfigDto billingConfigDto = Optional.ofNullable(companyDto.getBillingConfig())
                .orElseThrow(() -> new ResourceNotFoundException("La configuración de facturación no está disponible para la companyDto"));

        Billing billing = repository.findFirstByOrderByDateTimeRecordDesc();
        String prefixBilling = billingConfigDto.getPrefixBill();

        if (billing == null) {
            return prefixBilling.concat("-1");
        }

        Long consecutiveBilling = getBillingConsecutive(billing, prefixBilling);

        if (consecutiveBilling.intValue() >= billingConfigDto.getBillFrom() &&
                consecutiveBilling.intValue() <= billingConfigDto.getBillUntil()) {
            return prefixBilling.concat("-").concat(consecutiveBilling.toString());
        } else {
            throw new IllegalStateException("El número de factura esta por fuera del rango de facturación establecido.");
        }

    }

    private static Long getBillingConsecutive(Billing billing, String prefixBilling) {
        String billingNumber = billing.getBillNumber();
        if (billingNumber == null || !billingNumber.startsWith(prefixBilling)) {
            throw new IllegalStateException("El número de factura es inválido o no tiene el prefijo esperado");
        }

        String consecutiveNumber = billingNumber.replace(prefixBilling + "-", "");
        long consecutiveBilling;
        try {
            consecutiveBilling = Long.parseLong(consecutiveNumber) + 1L;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El número de factura no es un número válido", e);
        }
        return consecutiveBilling;
    }

    private BigDecimal ivaTotalBill(List<SaleDetailDto> saleDetailDtos) {
        AtomicReference<BigDecimal> totalIvaBill = new AtomicReference<>(BigDecimal.ZERO);
        saleDetailDtos.forEach(sale -> totalIvaBill.set(totalIvaBill.get().add(sale.getTotalVat())));
        return totalIvaBill.get();
    }

    private BigDecimal subTotalBill(List<SaleDetailDto> saleDetailDtos) {
        AtomicReference<BigDecimal> subTotalBill = new AtomicReference<>(BigDecimal.ZERO);
        saleDetailDtos.forEach(sale -> subTotalBill.set(subTotalBill.get().add(sale.getSubTotal())));
        return subTotalBill.get();
    }

    private List<SaleDetailDto> getSaleDetail(OrderDto order) {
        List<SaleDetailDto> lstSaleDetailDto = new ArrayList<>();
        List<ProductDto> lstProducts = order.getProducts();
        if (!lstProducts.isEmpty()) {
            lstProducts.forEach(product -> {
                SaleDetailDto saleDetailDto = SaleDetailDto.builder()
                        .product(ProductApi.builder()
                                .id(product.getId())
                                .barcode(product.getBarcode())
                                .description(product.getDescription())
                                .vatType(product.getVatType())
                                .build())
                        .amount(product.getAmount())
                        .unitPrice(product.getPrice())
                        .subTotal(product.getTotalValue())
                        .totalVat(getTotalIvaSale(product))
                        .build();
                lstSaleDetailDto.add(saleDetailDto);
            });
        }
        return lstSaleDetailDto;
    }

    private BigDecimal getTotalIvaSale(ProductDto product) {
        if (product.getVatType().name().equalsIgnoreCase(EVat.TARIFA_REDUCIDA.name())) {
            return ivaSale(product, EVat.TARIFA_REDUCIDA);
        }

        if (product.getVatType().name().equalsIgnoreCase(EVat.TARIFA_GENERAL.name())) {
            return ivaSale(product, EVat.TARIFA_GENERAL);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal ivaSale(ProductDto product, EVat vatValue) {
        if (product.getVatType().name().equalsIgnoreCase(vatValue.name())) {
            ProductVatTypeDto productIva = productVatTypeService.findByTipoIva(vatValue);
            BigDecimal iva = productIva.getPercentage().divide(new BigDecimal("100"));

            BigDecimal valorTotal = product.getTotalValue();
            return valorTotal.multiply(iva);
        }
        return BigDecimal.ZERO;
    }


}
