// =============================================================================
// SCRIPT: Identificar y reasignar barcodes duplicados en presentaciones
// =============================================================================
// PASO 1: Ejecutar la sección de DIAGNÓSTICO para ver duplicados
// PASO 2: Revisar los resultados y confirmar
// PASO 3: Ejecutar la sección de REASIGNACIÓN para corregir
// =============================================================================

// =============================================
// PASO 1: DIAGNÓSTICO - Encontrar barcodes duplicados
// =============================================

// Esta query encuentra todos los barcodes que aparecen en más de una presentación
// (ya sea dentro del mismo producto o entre productos diferentes)
var pipeline = [
    { $unwind: "$presentations" },
    { $group: {
        _id: "$presentations.barcode",
        count: { $sum: 1 },
        productos: { $push: {
            productId: "$_id",
            productCode: "$product_code",
            description: "$description",
            label: "$presentations.label",
            fixedAmount: "$presentations.fixed_amount",
            isFixedAmount: "$presentations.is_fixed_amount",
            isBulk: "$presentations.is_bulk"
        }}
    }},
    { $match: { count: { $gt: 1 } } },
    { $sort: { count: -1 } }
];

print("=== BARCODES DUPLICADOS ===");
var duplicados = db.PRODUCTS.aggregate(pipeline).toArray();

if (duplicados.length === 0) {
    print("✅ No se encontraron barcodes duplicados.");
} else {
    print("⚠️  Se encontraron " + duplicados.length + " barcodes duplicados:\n");
    duplicados.forEach(function(dup) {
        print("BARCODE: " + dup._id + " (aparece " + dup.count + " veces)");
        dup.productos.forEach(function(p) {
            print("  → Producto: " + p.productCode + " - " + p.description);
            print("    Presentación: " + p.label);
            print("    fixedAmount: " + p.fixedAmount + ", isFixedAmount: " + p.isFixedAmount + ", isBulk: " + p.isBulk);
        });
        print("");
    });
}

// =============================================
// PASO 2: OBTENER EL SIGUIENTE BARCODE DISPONIBLE
// =============================================

// Encuentra el barcode numérico interno más alto (formato 4 dígitos)
var maxBarcodePipeline = [
    { $unwind: "$presentations" },
    { $match: { "presentations.barcode": { $regex: /^\d{4}$/ } } },
    { $addFields: { "barcodeNum": { $toInt: "$presentations.barcode" } } },
    { $sort: { "barcodeNum": -1 } },
    { $limit: 1 },
    { $project: { barcode: "$presentations.barcode", barcodeNum: 1 } }
];

var maxResult = db.PRODUCTS.aggregate(maxBarcodePipeline).toArray();
var nextBarcode = 1000; // Default

if (maxResult.length > 0) {
    nextBarcode = maxResult[0].barcodeNum + 1;
}

print("\n=== INFORMACIÓN DE BARCODES ===");
print("Mayor barcode actual: " + (maxResult.length > 0 ? maxResult[0].barcode : "N/A"));
print("Próximo barcode disponible: " + String(nextBarcode).padStart(4, '0'));


// =============================================
// PASO 3: REASIGNACIÓN AUTOMÁTICA
// =============================================
// ⚠️  DESCOMENTA ESTA SECCIÓN SOLO DESPUÉS DE REVISAR EL DIAGNÓSTICO
// =============================================


print("\n=== REASIGNANDO BARCODES DUPLICADOS ===\n");

var nextAvailable = nextBarcode;

duplicados.forEach(function(dup) {
    var barcode = dup._id;
    var ocurrencias = dup.productos;

    // Mantener la PRIMERA ocurrencia con el barcode original, reasignar las demás
    print("Procesando barcode duplicado: " + barcode);

    for (var i = 1; i < ocurrencias.length; i++) {
        var p = ocurrencias[i];
        var newBarcode = String(nextAvailable).padStart(4, '0');

        print("  Reasignando: " + p.label);
        print("    Producto: " + p.productCode + " (" + p.productId + ")");
        print("    Barcode: " + barcode + " → " + newBarcode);

        // Actualizar en la base de datos
        // Usamos arrayFilters para apuntar a la presentación exacta por barcode + label
        var result = db.PRODUCTS.updateOne(
            { _id: p.productId },
            { $set: { "presentations.$[elem].barcode": newBarcode } },
            { arrayFilters: [ { "elem.barcode": barcode, "elem.label": p.label } ] }
        );

        if (result.modifiedCount > 0) {
            print("    ✅ Actualizado correctamente");
        } else {
            print("    ❌ No se pudo actualizar - verificar manualmente");
        }

        nextAvailable++;
    }
    print("");
});

print("=== REASIGNACIÓN COMPLETADA ===");
print("Nuevo próximo barcode disponible: " + String(nextAvailable).padStart(4, '0'));

// Verificación final
print("\n=== VERIFICACIÓN POST-REASIGNACIÓN ===");
var postCheck = db.PRODUCTS.aggregate(pipeline).toArray();
if (postCheck.length === 0) {
    print("✅ No quedan barcodes duplicados. Todo correcto.");
} else {
    print("⚠️  Aún quedan " + postCheck.length + " barcodes duplicados:");
    postCheck.forEach(function(d) {
        print("  BARCODE: " + d._id + " (" + d.count + " veces)");
    });
}

