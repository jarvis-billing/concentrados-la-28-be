package com.co.jarvis.util.reports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

@Component
public class ReportExporter {

    public byte[] exportToPdf(Object data, String reportName) throws JRException, FileNotFoundException {
        ReportGenerator<?> reportGenerator = new ReportGenerator<>(reportName);
        JasperPrint report;
        if (data instanceof List) {
            report = reportGenerator.getReport((List<?>) data);
        } else if (data instanceof Map) {
            report = reportGenerator.getReport((Map<?, ?>) data);
        } else {
            report = reportGenerator.getReport(data);
        }
        return JasperExportManager.exportReportToPdf(report);
    }
}
