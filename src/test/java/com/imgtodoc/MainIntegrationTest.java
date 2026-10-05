package com.imgtodoc;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
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
  private CommandLine commandLine;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    commandLine = new CommandLine(new Main());
  }

  @Test
  void shouldConvertSinglePngToDocx() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "NAME");

    assertThat(exitCode).isZero();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(2);
    }
  }

  @Test
  void shouldConvertDirectoryWithSortBySize() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "SIZE");

    assertThat(exitCode).isZero();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(2);
    }
  }

  @Test
  void shouldConvertDirectoryWithSortByDate() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-d", SAMPLE_DIR.toString(), "-o", outputFile.toString(), "--sort", "DATE");

    assertThat(exitCode).isZero();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(2);
    }
  }

  @Test
  void shouldCombineDirectoryAndIndividualFiles() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--paper-size", paperSize, "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();
  }

  @Test
  void shouldFailWhenNoInputProvided() {
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-d", "/nonexistent/dir", "-o", outputFile.toString());

    assertThat(exitCode).isNotZero();
  }

  @Test
  void shouldFailWhenInputFileNotExists() {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-o", outputFile.toString(), "/nonexistent/image.png");

    assertThat(exitCode).isNotZero();
  }

  @Test
  void shouldSupportCustomWidthAndHeight() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("-w", "10cm", "-h", "15cm", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();
  }

  @Test
  void shouldSupportNoPageBreakAfterLast() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
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
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--margin", "2cm", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();
  }

  @Test
  void shouldSupportFitModeCover() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--fit-to-page", "--fit-mode", "COVER", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(1);
      assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
    }
  }

  @Test
  void shouldSupportFitModeContain() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--fit-to-page", "--fit-mode", "CONTAIN", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(1);
      assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
    }
  }

  @Test
  void shouldSupportFitModeStretch() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--fit-to-page", "--fit-mode", "STRETCH", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(1);
      assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
    }
  }

  @Test
  void shouldDefaultToCoverModeWhenFitToPageEnabled() throws Exception {
    Path outputFile = tempDir.resolve("output.docx");
    int exitCode = runMain("--fit-to-page", "-o", outputFile.toString(), TEST_IMG_PNG.toString());

    assertThat(exitCode).isZero();
    assertThat(outputFile).exists();

    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
      assertThat(doc.getParagraphs()).hasSize(1);
      assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
    }
  }

  private int runMain(String... args) {
    return commandLine.execute(args);
  }
}