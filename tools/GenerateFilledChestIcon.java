import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class GenerateFilledChestIcon {
    public static void main(String[] args) throws Exception {
        var image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(18, 24, 31, 90));
        g.fillOval(12, 102, 104, 16);
        g.setColor(new Color(47, 30, 22));
        g.fillRoundRect(12, 28, 104, 81, 12, 12);
        g.setColor(new Color(153, 91, 43));
        g.fillRoundRect(16, 32, 96, 73, 8, 8);
        g.setColor(new Color(194, 128, 62));
        g.fillRoundRect(16, 32, 96, 29, 8, 8);
        g.setColor(new Color(79, 47, 30));
        g.fillRect(16, 59, 96, 6);
        g.setStroke(new BasicStroke(3));
        g.drawLine(24, 78, 104, 78);
        g.drawLine(24, 94, 104, 94);
        g.setColor(new Color(244, 195, 89));
        g.fillRect(28, 32, 10, 73);
        g.fillRect(90, 32, 10, 73);
        g.fillRoundRect(53, 54, 23, 27, 5, 5);
        g.setColor(new Color(80, 53, 30));
        g.fillOval(61, 61, 7, 8);
        g.fillRect(63, 67, 3, 7);
        // Original artwork: a high-contrast contents badge, independent of game textures.
        g.setColor(new Color(17, 36, 32));
        g.fillRoundRect(74, 76, 50, 44, 12, 12);
        g.setColor(new Color(71, 220, 157));
        g.fillRoundRect(78, 80, 42, 36, 9, 9);
        g.setColor(new Color(14, 65, 49));
        for (int y = 87; y <= 103; y += 8) {
            g.fillRect(85, y, 5, 4);
            g.fillRect(94, y, 18, 4);
        }
        g.dispose();
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IllegalStateException("PNG writer is unavailable");
        }
    }
}
