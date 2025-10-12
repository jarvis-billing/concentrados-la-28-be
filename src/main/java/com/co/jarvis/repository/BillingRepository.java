package com.co.jarvis.repository;

import com.co.jarvis.dto.ProductSalesSummary;
import com.co.jarvis.entity.Billing;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface BillingRepository extends MongoRepository<Billing, String> {

    Billing findByBillNumber(String billingNumber);

    Billing findFirstByOrderByDateTimeRecordDesc();

    @Aggregation(pipeline = {
            "{ '$match': { 'dateTimeRecord': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$unwind': '$saleDetails' }",
            "{ '$match': { 'saleDetails.product.barcode': { '$exists': true, '$ne': null } } }",
            "{ '$group': { " +
                    "  '_id': '$saleDetails.product.barcode', " +
                    "  'description': { '$first': '$saleDetails.product.description' }, " +

                    // Total Amount con conversión segura
                    "  'totalAmount': { '$sum': { " +
                    "    '$cond': [ " +
                    "      { '$and': [ " +
                    "        { '$ne': ['$saleDetails.amount', null] }, " +
                    "        { '$ne': ['$saleDetails.amount', ''] } " +
                    "      ] }, " +
                    "      { '$toDecimal': '$saleDetails.amount' }, 0 " +
                    "    ] " +
                    "  } }, " +

                    // Unit Price promedio
                    "  'unitPrice': { '$avg': { " +
                    "    '$cond': [ " +
                    "      { '$and': [ " +
                    "        { '$ne': ['$saleDetails.unitPrice', null] }, " +
                    "        { '$ne': ['$saleDetails.unitPrice', ''] } " +
                    "      ] }, " +
                    "      { '$toDecimal': '$saleDetails.unitPrice' }, 0 " +
                    "    ] " +
                    "  } }, " +

                    // Total Subtotal
                    "  'totalSubTotal': { '$sum': { " +
                    "    '$cond': [ " +
                    "      { '$and': [ " +
                    "        { '$ne': ['$saleDetails.subTotal', null] }, " +
                    "        { '$ne': ['$saleDetails.subTotal', ''] } " +
                    "      ] }, " +
                    "      { '$toDecimal': '$saleDetails.subTotal' }, 0 " +
                    "    ] " +
                    "  } }, " +

                    // Total VAT
                    "  'totalVat': { '$sum': { " +
                    "    '$cond': [ " +
                    "      { '$and': [ " +
                    "        { '$ne': ['$saleDetails.totalVat', null] }, " +
                    "        { '$ne': ['$saleDetails.totalVat', ''] } " +
                    "      ] }, " +
                    "      { '$toDecimal': '$saleDetails.totalVat' }, 0 " +
                    "    ] " +
                    "  } } " +

                    "} }",
            "{ '$sort': { 'totalAmount': -1 } }"
    })
    List<ProductSalesSummary> getProductSalesSummaryByDate(OffsetDateTime from, OffsetDateTime to);




    @Aggregation(pipeline = {
            "{ '$match': { 'dateTimeRecord': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$unwind': '$saleDetails' }",
            "{ '$match': { 'saleDetails.product.barcode': ?2 } }",
            "{ '$group': { " +
                    "    '_id': '$saleDetails.product.barcode', " +
                    "    'totalAmount': { '$sum': '$saleDetails.amount' }, " +
                    "    'description': { '$first': '$saleDetails.product.description' }, " +
                    "    'avgUnitPrice': { '$avg': '$saleDetails.unitPrice' }, " +
                    "    'totalSubTotal': { '$sum': '$saleDetails.subTotal' }, " +
                    "    'totalVat': { '$sum': '$saleDetails.totalVat' } " +
                    "} }",
            "{ '$sort': { 'totalAmount': -1 } }"
    })
    List<ProductSalesSummary> getProductSalesSummaryByDateAndProduct(OffsetDateTime from, OffsetDateTime to, String productBarcode);

}
