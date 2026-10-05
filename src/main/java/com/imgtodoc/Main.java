package com.imgtodoc;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.*;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "image-to-doc",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    description = "Create a DOCX document with images, one per page.",
    header = "Image to DOCX Converter",
    footer = "Example: image-to-doc -o output.docx image1.png image2.jpg"
)
public class Main implements Callable<Integer> {

  @Parameters(index = "0..*", arity = "1..*", paramLabel = "IMAGE_FILE",
      description = "Input image files (supported formats: PNG, JPEG, BMP, GIF, TIFF)")
  private List<String> imageFiles;

  @Option(names = {"-o", "--output"}, required = true, paramLabel = "FILE",
      description = "Output DOCX file path")
  private File outputFile;

  @Option(names = {"-w", "--width"}, paramLabel = "VALUE",
      description = "Image width (default: 18.6cm). Supports cm, mm, in, pt, px (e.g., 18.6cm, 186mm, 7.3in)")
  private String width = "18.6cm";

  @Option(names = {"-h", "--height"}, paramLabel = "VALUE",
      description = "Image height (default: 27.5cm). Supports cm, mm, in, pt, px (e.g., 27.8cm, 278mm, 10.9in)")
  private String height = "27.5cm";

  @Option(names = {"--paper-size"}, paramLabel = "SIZE",
      description = "Paper size (default: A4). Options: A4, A3, A5, LETTER, LEGAL")
  private PaperSize paperSize = PaperSize.A4;

  @Option(names = {"--lock-aspect-ratio"}, paramLabel = "BOOL",
      description = "Lock aspect ratio (default: true)")
  private boolean lockAspectRatio = true;

  @Option(names = {"--no-page-break"}, paramLabel = "BOOL",
      description = "Don't add page break after last image (default: false)")
  private boolean noPageBreakAfterLast = false;

  @Option(names = {"--margin"}, paramLabel = "VALUE",
      description = "Page margin (default: 1cm). Supports cm, mm, in, pt (e.g., 1cm, 10mm, 0.4in)")
  private String margin = "1cm";

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    validateInputs();

    try (XWPFDocument document = new XWPFDocument()) {
      configurePageSetup(document);

      for (int i = 0; i < imageFiles.size(); i++) {
        addImageToPage(document, imageFiles.get(i), i);
      }

      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        document.write(fos);
      }

      System.out.println("Successfully created: " + outputFile.getAbsolutePath());
      System.out.println("Pages: " + imageFiles.size());
      return 0;
    }
  }

  private void validateInputs() throws Exception {
    if (imageFiles.isEmpty()) {
      throw new IllegalArgumentException("At least one image file must be provided");
    }

    for (String imageFile : imageFiles) {
      Path path = Paths.get(imageFile);
      if (!Files.exists(path)) {
        throw new FileNotFoundException("Image file not found: " + imageFile);
      }
      if (!Files.isRegularFile(path)) {
        throw new IllegalArgumentException("Not a regular file: " + imageFile);
      }
    }

    // Validate output directory exists
    File parentDir = outputFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        throw new IOException("Cannot create output directory: " + parentDir);
      }
    }
  }

  private void configurePageSetup(XWPFDocument document) {
    // Get or create section properties
    CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
        ? document.getDocument().getBody().getSectPr()
        : document.getDocument().getBody().addNewSectPr();

    // Set page size
    CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
    pageSz.setOrient(STPageOrientation.PORTRAIT);

    // Paper sizes in twips (1 twip = 1/1440 inch = 1/20 point)
    // A4: 210mm x 297mm = 11906 x 16838 twips
    // A3: 297mm x 420mm = 16838 x 23811 twips
    // A5: 148mm x 210mm = 8391 x 11906 twips
    // Letter: 8.5in x 11in = 12240 x 15840 twips
    // Legal: 8.5in x 14in = 12240 x 20160 twips
    switch (paperSize) {
      case A3:
        pageSz.setW(BigInteger.valueOf(16838));
        pageSz.setH(BigInteger.valueOf(23811));
        break;
      case A5:
        pageSz.setW(BigInteger.valueOf(8391));
        pageSz.setH(BigInteger.valueOf(11906));
        break;
      case LETTER:
        pageSz.setW(BigInteger.valueOf(12240));
        pageSz.setH(BigInteger.valueOf(15840));
        break;
      case LEGAL:
        pageSz.setW(BigInteger.valueOf(12240));
        pageSz.setH(BigInteger.valueOf(20160));
        break;
      case A4:
      default:
        pageSz.setW(BigInteger.valueOf(11906));
        pageSz.setH(BigInteger.valueOf(16838));
        break;
    }

    // Set margins (in twips)
    int marginTwips = parseLengthToTwips(margin);
    if (sectPr.isSetPgMar()) {
      sectPr.getPgMar().setTop(BigInteger.valueOf(marginTwips));
      sectPr.getPgMar().setBottom(BigInteger.valueOf(marginTwips));
      sectPr.getPgMar().setLeft(BigInteger.valueOf(marginTwips));
      sectPr.getPgMar().setRight(BigInteger.valueOf(marginTwips));
    } else {
      org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pgMar = sectPr.addNewPgMar();
      pgMar.setTop(BigInteger.valueOf(marginTwips));
      pgMar.setBottom(BigInteger.valueOf(marginTwips));
      pgMar.setLeft(BigInteger.valueOf(marginTwips));
      pgMar.setRight(BigInteger.valueOf(marginTwips));
    }
  }

  private void addImageToPage(XWPFDocument document, String imagePath, int index) throws Exception {
    File file = new File(imagePath);
    BufferedImage image = ImageIO.read(file);
    if (image == null) {
      throw new IOException("Cannot read image: " + imagePath);
    }

    long widthEmu = parseLengthToEmu(width);
    long heightEmu = parseLengthToEmu(height);

    if (lockAspectRatio) {
      double imageAspectRatio = (double) image.getWidth() / image.getHeight();
      double targetAspectRatio = (double) widthEmu / heightEmu;
      if (imageAspectRatio > targetAspectRatio) {
        heightEmu = Math.round(widthEmu / imageAspectRatio);
      } else {
        widthEmu = Math.round(heightEmu * imageAspectRatio);
      }
    }

    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    paragraph.setSpacingBefore(0);
    paragraph.setSpacingAfter(0);
    if (index > 0) {
      paragraph.setPageBreak(true); // start each image on a new page, no trailing blank page
    }

    XWPFRun run = paragraph.createRun();
    try (InputStream is = new FileInputStream(file)) {
      run.addPicture(is, getPOIImageType(imagePath), file.getName(),
          (int) widthEmu, (int) heightEmu);
    }
  }

  private int getPOIImageType(String imagePath) {
    String lower = imagePath.toLowerCase();
    if (lower.endsWith(".png")) return XWPFDocument.PICTURE_TYPE_PNG;
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return XWPFDocument.PICTURE_TYPE_JPEG;
    if (lower.endsWith(".bmp")) return XWPFDocument.PICTURE_TYPE_BMP;
    if (lower.endsWith(".gif")) return XWPFDocument.PICTURE_TYPE_GIF;
    if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return XWPFDocument.PICTURE_TYPE_TIFF;
    return XWPFDocument.PICTURE_TYPE_PNG; // default
  }

  /**
   * Parse a length string (e.g., "18.6cm", "186mm", "7.3in") to EMUs
   * 1 inch = 914400 EMU = 72 pt = 2.54 cm = 25.4 mm = 96 px (at 96 DPI)
   */
  private long parseLengthToEmu(String value) {
    value = value.trim().toLowerCase();
    double number;
    String unit;

    if (value.endsWith("cm")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (long) (number * 360000); // 1 cm = 360000 EMU
    } else if (value.endsWith("mm")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (long) (number * 36000); // 1 mm = 36000 EMU
    } else if (value.endsWith("in")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (long) (number * 914400); // 1 inch = 914400 EMU
    } else if (value.endsWith("pt")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (long) (number * 12700); // 1 pt = 12700 EMU
    } else if (value.endsWith("px")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (long) (number * 9525); // 1 px at 96 DPI = 9525 EMU
    } else {
      // Assume cm if no unit specified
      number = Double.parseDouble(value);
      return (long) (number * 360000);
    }
  }

  /**
   * Parse a length string to twips (1 twip = 1/20 point = 1/1440 inch)
   */
  private int parseLengthToTwips(String value) {
    value = value.trim().toLowerCase();
    double number;

    if (value.endsWith("cm")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (int) Math.round(number * 567); // 1 cm = 567 twips
    } else if (value.endsWith("mm")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (int) Math.round(number * 56.7); // 1 mm = 56.7 twips
    } else if (value.endsWith("in")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (int) Math.round(number * 1440); // 1 inch = 1440 twips
    } else if (value.endsWith("pt")) {
      number = Double.parseDouble(value.substring(0, value.length() - 2));
      return (int) Math.round(number * 20); // 1 pt = 20 twips
    } else {
      // Assume cm if no unit specified
      number = Double.parseDouble(value);
      return (int) Math.round(number * 567);
    }
  }

  enum PaperSize {
    A4, A3, A5, LETTER, LEGAL
  }
}