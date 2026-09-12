package com.health.assistant.support;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor {

    public record Extraction(String text, boolean scanned) {
    }

    public Extraction extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new BizException(ErrorCode.PDF_PARSE_FAILED, "PDF 已加密，无法解析");
            }
            int pages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append(stripper.getText(document)).append('\n');
            }
            String content = text.toString();
            String compact = content.replaceAll("\\s", "");
            boolean scanned = compact.length() < 50 || (pages > 0 && compact.length() / pages < 30);
            return new Extraction(content, scanned);
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.PDF_PARSE_FAILED, "PDF 解析失败: " + e.getMessage());
        }
    }
}
