package com.imgtodoc;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class MainIntegrationTest {

    private static final Path SAMPLE_DIR = Path.of("src/test/sample");
    private static final Path TEST_IMG_PNG = SAMPLE_DIR.resolve("test_img.png");
    private static final Path TEST_IMG_JPG = SAMPLE_DIR.resolve("test_img2.jpg");
    private Path outputFile;
    private CommandLine commandLine;

    @BeforeEach
    void setUp() throws IOException {
        outputFile = Files.createTempFile("image-to-doc-test-", ".docx");
        outputFile.toFile().deleteOnExit();
        commandLine = new CommandLine(new Main());
    }

    @AfterEach
    void tearDown() {
        if (outputFile != null) {
            outputFile.toFile().delete();
        }
    }

    @Test
    void shouldConvertSinglePngToDocx() throws Exception {
        int exitCode = runMain("-o", outputFile.toString(), TEST_IMG_PNG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();
        assertThat(outputFile.toFile().length()).isGreaterThan(0);

        // Verify document structure
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            assertThat(paragraphs).hasSize(1);

            XWPFParagraph paragraph = paragraphs.get(0);
            assertThat(paragraph.getAlignment()).isEqualTo(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);

            List<XWPFRun> runs = paragraph.getRuns();
            assertThat(runs).hasSize(1);
            assertThat(runs.get(0).getEmbeddedPictures()).hasSize(1);
        }
    }

    @Test
    void shouldConvertSingleJpgToDocx() throws Exception {
        int exitCode = runMain("-o", outputFile.toString(), TEST_IMG_JPG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();
        assertThat(outputFile.toFile().length()).isGreaterThan(0);

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            assertThat(doc.getParagraphs()).hasSize(1);
            assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
        }
    }

    @Test
    void shouldConvertMultipleImagesToDocx() throws Exception {
        int exitCode = runMain("-o", outputFile.toString(), TEST_IMG_PNG.toString(), TEST_IMG_JPG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            assertThat(paragraphs).hasSize(2);

            // First image - no page break before
            assertThat(paragraphs.get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);

            // Second image - should have page break
            assertThat(paragraphs.get(1).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
        }
    }

    @Test
    void shouldConvertDirectoryOfImages() throws Exception {
        int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            // Should have 2 images (png and jpg)
            assertThat(doc.getParagraphs()).hasSize(2);
        }
    }

    @Test
    void shouldConvertDirectoryWithSortByName() throws Exception {
        int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "NAME");

        assertThat(exitCode).isZero();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            assertThat(doc.getParagraphs()).hasSize(2);
        }
    }

    @Test
    void shouldConvertDirectoryWithSortBySize() throws Exception {
        int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "SIZE");

        assertThat(exitCode).isZero();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            assertThat(doc.getParagraphs()).hasSize(2);
        }
    }

    @Test
    void shouldConvertDirectoryWithSortByDate() throws Exception {
        int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "DATE");

        assertThat(exitCode).isZero();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            assertThat(doc.getParagraphs()).hasSize(2);
        }
    }

    @Test
    void shouldCombineDirectoryAndIndividualFiles() throws Exception {
        int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), TEST_IMG_PNG.toString());

        assertThat(exitCode).isZero();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            // 2 from directory + 1 individual = 3
            assertThat(doc.getParagraphs()).hasSize(3);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"A4", "A3", "A5", "LETTER", "LEGAL"})
    void shouldSupportDifferentPaperSizes(String paperSize) throws Exception {
        int exitCode = runMain("--paper-size", paperSize, "-o", outputFile.toString(), TEST_IMG_PNG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();
    }

    @Test
    void shouldFailWhenNoInputProvided() {
        int exitCode = runMain("-o", outputFile.toString());

        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldFailWhenOutputNotProvided() {
        int exitCode = runMain(TEST_IMG_PNG.toString());

        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldFailWhenInputDirectoryNotExists() {
        int exitCode = runMain("-d", "/nonexistent/dir", "-o", outputFile.toString());

        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldFailWhenInputFileNotExists() {
        int exitCode = runMain("-o", outputFile.toString(), "/nonexistent/image.png");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldSupportCustomWidthAndHeight() throws Exception {
        int exitCode = runMain("-w", "10cm", "-h", "15cm", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();
    }

    @Test
    void shouldSupportNoPageBreakAfterLast() throws Exception {
        int exitCode = runMain("--no-page-break", "-o", outputFile.toString(), TEST_IMG_PNG.toString(), TEST_IMG_JPG.toString());

        assertThat(exitCode).isZero();

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            assertThat(paragraphs).hasSize(2);
            // Both paragraphs should not have page break when --no-page-break is used for the last
            // Actually, only the last one should not have page break
        }
    }

    @Test
    void shouldSupportCustomMargin() throws Exception {
        int exitCode = runMain("--margin", "2cm", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();
    }

    private int runMain(String... args) {
        return commandLine.execute(args);
    }
}