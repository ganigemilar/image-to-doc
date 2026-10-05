# Image to DOCX Converter

A command-line tool to create Microsoft Word documents (`.docx`) from images, with one image per page. Built with **Picocli** and **Apache POI**.

## Features

- 📄 **One image per page** — Each input image gets its own page
- 📁 **Directory input** — Process all images in a folder with `--directory` / `-d`
- 🔤 **Sort options** — Sort by name (default), date, or size with `--sort`
- 🔒 **Lock aspect ratio** (default: `true`) — Preserves image proportions automatically
- 📏 **Configurable dimensions** — Width/height in `cm`, `mm`, `in`, `pt`, `px`
- 📐 **Multiple paper sizes** — A4 (default), A3, A5, Letter, Legal
- 📦 **Configurable margins** — Default 1cm
- 📑 **Page breaks** — Automatic between pages (optional after last)
- 🎯 **Matches your template** — Default 18.6cm × 27.8cm, centered, A4

## Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/image-to-doc-1.0.0.jar -o output.docx image1.png image2.jpg image3.tiff
```

## Usage

```bash
image-to-doc [OPTIONS] -o <output.docx> [<image-files...>]
```

### Required

| Option | Description |
|--------|-------------|
| `-o, --output <FILE>` | Output DOCX file path |
| `<IMAGE_FILE>...` | Input images (PNG, JPEG, BMP, GIF, TIFF) — optional if `-d/--directory` is used |

### Optional

| Option | Default | Description |
|--------|---------|-------------|
| `-d, --directory <DIR>` | — | Input directory containing images (processes all supported images non-recursively) |
| `--sort <MODE>` | `name` | Sort order for directory images: `name`, `date`, `size` |
| `-w, --width <VALUE>` | `18.6cm` | Image width (cm, mm, in, pt, px) |
| `-h, --height <VALUE>` | `27.8cm` | Image height (cm, mm, in, pt, px) |
| `--paper-size <SIZE>` | `A4` | Paper size: `A4`, `A3`, `A5`, `LETTER`, `LEGAL` |
| `--lock-aspect-ratio <BOOL>` | `true` | Lock aspect ratio (`true`/`false`) |
| `--margin <VALUE>` | `1cm` | Page margin (cm, mm, in, pt) |
| `--no-page-break` | `false` | Skip page break after last image |

### Unit Examples

```bash
# Centimeters (default)
--width 18.6cm --height 27.8cm

# Millimeters
--width 186mm --height 278mm

# Inches
--width 7.3in --height 10.9in

# Points
--width 527pt --height 789pt

# Pixels (at 96 DPI)
--width 1786px --height 2670px
```

## Examples

```bash
# Basic usage with defaults
java -jar image-to-doc-1.0.0.jar -o photos.docx *.png

# Custom size, A3 paper
java -jar image-to-doc-1.0.0.jar -o output.docx \
  --width 20cm --height 25cm \
  --paper-size A3 \
  photo1.jpg photo2.jpg

# Force exact size (ignore aspect ratio)
java -jar image-to-doc-1.0.0.jar -o output.docx \
  --width 10cm --height 10cm \
  --lock-aspect-ratio=false \
  image.png

# No page break after last image
java -jar image-to-doc-1.0.0.jar -o output.docx \
  --no-page-break \
  page1.png page2.png page3.png
```

## Output Structure

The generated DOCX matches your reference template (`hpic-master.docx`):

- ✅ Centered images
- ✅ Page break after each image
- ✅ Locked aspect ratio (`preferRelativeResize="false"`)
- ✅ Default: 18.6cm × 27.8cm (6702263 × 10015528 EMU)
- ✅ A4 paper (11906 × 16838 twips)
- ✅ 1cm margins (567 twips)

## Requirements

- Java 21+
- Maven 3.8+ (for building)

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Picocli | 4.7.6 | CLI parsing |
| Apache POI | 5.3.0 | DOCX manipulation |
| OOXML Schemas | 1.4 | Low-level XML types |

## Building

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package executable JAR
mvn package

# Output: target/image-to-doc-1.0.0.jar (~33 MB with all deps)
```

## Project Structure

```
image-to-doc/
├── pom.xml
├── src/
│   ├── main/java/com/imgtodoc/Main.java
│   └── test/
│       └── sample/hpic-master.docx    # Reference template
└── target/
    └── image-to-doc-1.0.0.jar         # Executable JAR
```

## License

MIT License