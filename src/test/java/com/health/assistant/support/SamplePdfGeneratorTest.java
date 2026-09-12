package com.health.assistant.support;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SamplePdfGeneratorTest {

    @Test
    void generateSampleReportPdf() throws IOException {
        Path output = Path.of("target", "sample-report.pdf");
        Files.createDirectories(output.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = loadChineseFont(document);
            assumeTrue(font != null, "本机没有可用的中文字体，跳过样本 PDF 生成");

            String[] lines = {
                    "体检报告",
                    "姓名:张建国  性别:男  年龄:59",
                    "体检日期:2026-08-20  体检机构:市第一人民医院",
                    "谷丙转氨酶(ALT)    52    U/L     9-50",
                    "谷草转氨酶(AST)    35    U/L     15-40",
                    "空腹血糖(GLU)      5.4   mmol/L  3.9-6.1",
                    "总胆固醇(TC)       5.6   mmol/L  2.8-5.2",
                    "甘油三酯(TG)       1.2   mmol/L  0.45-1.7",
                    "低密度脂蛋白(LDL-C) 3.8  mmol/L  1.5-3.4",
                    "白细胞计数(WBC)    6.1   10^9/L  3.5-9.5",
                    "血红蛋白(HGB)      145   g/L     130-175",
                    "尿蛋白(PRO)        阴性  阴性"
            };
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.setLeading(18f);
                contentStream.newLineAtOffset(50, 780);
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                contentStream.endText();
            }
            document.save(output.toFile());
        }
        assertThat(output).exists();
    }

    private PDFont loadChineseFont(PDDocument document) throws IOException {
        Path simhei = Path.of("C:\\Windows\\Fonts\\simhei.ttf");
        if (Files.exists(simhei)) {
            return PDType0Font.load(document, simhei.toFile());
        }
        Path msyh = Path.of("C:\\Windows\\Fonts\\msyh.ttc");
        if (Files.exists(msyh)) {
            try (TrueTypeCollection collection = new TrueTypeCollection(msyh.toFile())) {
                TrueTypeFont font = collection.getFontByName("Microsoft YaHei");
                if (font != null) {
                    return PDType0Font.load(document, font, true);
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        Path simsun = Path.of("C:\\Windows\\Fonts\\simsun.ttc");
        if (Files.exists(simsun)) {
            try (TrueTypeCollection collection = new TrueTypeCollection(simsun.toFile())) {
                TrueTypeFont font = collection.getFontByName("SimSun");
                if (font != null) {
                    return PDType0Font.load(document, font, true);
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
