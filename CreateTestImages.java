import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CreateTestImages {
    public static void main(String[] args) throws IOException {
        File dir = new File("test_images");
        dir.mkdirs();

        int[][] configs = {
            {1920, 1080, 0xFF0000},  // Red landscape
            {1080, 1920, 0x0000FF},  // Blue portrait
            {1920, 1920, 0x00FF00},  // Green square
            {800, 600, 0xFFFF00},    // Yellow small
            {600, 800, 0xFF00FF},    // Magenta tall
        };

        for (int i = 0; i < configs.length; i++) {
            int w = configs[i][0];
            int h = configs[i][1];
            int color = configs[i][2];

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(color));
            g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("Test Image " + (i+1), 50, 80);
            g.drawString(w + "x" + h, 50, 140);
            g.dispose();

            File out = new File(dir, "test_" + (i+1) + ".png");
            ImageIO.write(img, "PNG", out);
            System.out.println("Created: " + out.getAbsolutePath() + " (" + w + "x" + h + ")");
        }
        System.out.println("Done!");
    }
}