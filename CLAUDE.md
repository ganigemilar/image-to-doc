# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Image to DOCX Converter** - A CLI tool that creates Microsoft Word documents (`.docx`) from images, with one image per page. Built with **Picocli** for CLI parsing and **Apache POI** for DOCX generation.

## Commands

### Build
```bash
mvn clean compile          # Compile only
mvn clean package          # Build executable JAR (output: target/image-to-doc-1.0.0.jar)
```

### Test
```bash
mvn test                   # Run all tests (24 integration tests)
mvn test -Dtest=MainIntegrationTest#shouldConvertSinglePngToDocx  # Run single test
```

### Run
```bash
# From built JAR
java -jar target/image-to-doc-1.0.0.jar -o output.docx image1.png image2.jpg

# Direct from Maven (for development)
mvn exec:java -Dexec.mainClass=com.imgtodoc.Main -Dexec.args="-o output.docx image1.png"
```

### Help
```bash
java -jar target/image-to-doc-1.0.0.jar --help
```

## Architecture

### Single-Class Design
The entire application is in **`src/main/java/com/imgtodoc/Main.java`** - a single `Callable<Integer>` class using Picocli annotations. This is intentional for a simple CLI tool.

### Key Components

1. **CLI Options** (lines 39-85): All options defined via Picocli annotations
   - `@Parameters`: Positional image file arguments
   - `@Option`: Named options (`-o`, `-d`, `--width`, `--height`, `--paper-size`, `--fit-to-page`, `--fit-mode`, etc.)

2. **Entry Point** (lines 87-121): `call()` method orchestrates the workflow
   - Validates inputs
   - Collects images from directory + individual files
   - Creates XWPFDocument, configures page setup
   - Adds each image to a new page
   - Writes output file

3. **Page Setup** (lines 183-237): Configures document paper size (A4/A3/A5/Letter/Legal) and margins using OOXML low-level APIs (CTSectPr, CTPageSz)

4. **Image Processing** (lines 239-323): `addImageToPage()` handles:
   - Reading image via ImageIO
   - Calculating dimensions in EMUs (1/914400 inch)
   - **Fit modes**: COVER (fill, crop), CONTAIN (fit entirely), STRETCH (ignore ratio)
   - Aspect ratio locking when not using fit-to-page
   - Adding centered image to paragraph with page break

5. **Unit Parsing** (lines 339-390): `parseLengthToEmu()` and `parseLengthToTwips()` handle cm, mm, in, pt, px units

### Enums
- `PaperSize`: A4, A3, A5, LETTER, LEGAL
- `SortOrder`: NAME, DATE, SIZE (for directory processing)
- `FitMode`: COVER, CONTAIN, STRETCH (for `--fit-to-page` behavior)

## Testing

**Integration tests only** in `src/test/java/com/imgtodoc/MainIntegrationTest.java` (24 tests):
- Uses `@TempDir` for isolated output files
- Tests use sample images in `src/test/sample/` (test_img.png, test_img2.jpg)
- Each test creates a CommandLine instance and calls `execute()`
- Verifies exit codes, file existence, and DOCX structure (paragraphs, runs, embedded pictures)

### Test Patterns
```java
// Standard pattern
Path outputFile = tempDir.resolve("output.docx");
int exitCode = runMain("-o", outputFile.toString(), TEST_IMG_PNG.toString());
assertThat(exitCode).isZero();
assertThat(outputFile).exists();

// Verify DOCX content
try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(outputFile))) {
    assertThat(doc.getParagraphs()).hasSize(1);
    assertThat(doc.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()).hasSize(1);
}
```

## Dependencies (pom.xml)

| Library | Version | Purpose |
|---------|---------|---------|
| Picocli | 4.7.6 | CLI parsing |
| Apache POI | 5.3.0 | DOCX manipulation (poi-ooxml) |
| OOXML Schemas | 1.4 | Low-level XML types (CTSectPr, CTPageSz) |
| JUnit Jupiter | 5.10.2 | Testing |
| AssertJ | 3.24.2 | Fluent assertions |

**Build Plugins:**
- `maven-shade-plugin`: Creates fat JAR with all dependencies
- `maven-compiler-plugin`: Java 21 target
- `maven-surefire-plugin`: Test execution

## Key Technical Details

### Units
- **EMU** (English Metric Unit): 1 inch = 914,400 EMU - used for image dimensions in DOCX
- **Twips**: 1 inch = 1,440 twips = 20 points - used for paper size and margins in OOXML

### Fit-to-Page Logic
When `--fit-to-page` is enabled:
1. Calculates available space: paper dimensions - (2 × margins)
2. Converts to EMUs (1 twip = 635 EMU)
3. Applies selected `FitMode`:
   - **COVER**: Scale to fill entire area, crop excess (maintains aspect ratio)
   - **CONTAIN**: Scale to fit entirely within area (maintains aspect ratio)
   - **STRETCH**: Exact fill ignoring aspect ratio

### Default Behavior (no --fit-to-page)
- Uses explicit `--width`/`--height` (default 18.6cm × 27.5cm)
- If `--lock-aspect-ratio` (default true), scales to fit within bounds preserving ratio

## Sample Images
Located in `src/test/sample/`:
- `test_img.png`, `test_img2.jpg` - primary test images
- `test_images/` - additional test images
- `hpic-master.docx` - reference template

## Git
- Main branch: `master`
- Remote: `https://github.com/ganigemilar/image-to-doc.git`

## .gitignore
Excludes: `target/`, `.idea/`, `dependency-reduced-pom.xml`, IDE files