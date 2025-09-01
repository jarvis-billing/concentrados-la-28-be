package com.co.jarvis.util.reports;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ReportGenerator<D extends Serializable> {

    public static final String EXTENSION_REPORT = ".jrxml";
    public static final String PATCH_SOURCE_REPORTS = "/reports/%s%s";
    private final String reportName;

    public ReportGenerator(String reportName) {
        this.reportName = reportName;
    }

    // Método genérico para recibir una lista
    public <D> JasperPrint getReport(List<D> list) throws FileNotFoundException, JRException {
        return getReport(new JRBeanCollectionDataSource(list));
    }

    // Método genérico para recibir un mapa de datos
    public JasperPrint getReport(Map<String, Object> map) throws FileNotFoundException, JRException {
        return getReport(new JRMapCollectionDataSource(List.of(map)));
    }

    // Método para recibir un solo objeto y encapsularlo en una lista
    public <D> JasperPrint getReport(D object) throws FileNotFoundException, JRException {
        return getReport(List.of(object));
    }

    // Método principal que genera el reporte usando cualquier JRDataSource
    private JasperPrint getReport(JRDataSource dataSource) throws FileNotFoundException, JRException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("dataSource", dataSource);
            InputStreamReader reportStream = new InputStreamReader(requireNonNull(getClass().getResourceAsStream(
                    format(PATCH_SOURCE_REPORTS, this.reportName, EXTENSION_REPORT))), StandardCharsets.UTF_8);
            InputStream reportInput = convertToInputStream(reportStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(reportInput);

            return JasperFillManager.fillReport(jasperReport, params, dataSource);

        } catch (JRException | IOException e) {
            log.error(e.getMessage());
            throw new JRException(e.getMessage());
        }

    }

    private InputStream convertToInputStream(InputStreamReader reader) throws IOException {
        // Lee el contenido de InputStreamReader y conviértelo en un String
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }

        // Convierte el String en bytes UTF-8 y crea un nuevo InputStream
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
