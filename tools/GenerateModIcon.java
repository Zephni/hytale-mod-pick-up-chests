import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class GenerateModIcon {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java tools\\GenerateModIcon.java <output.png>");
        }
        var image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.scale(400.0 / 512, 400.0 / 512);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x17262D));
        g.fillRect(0, 0, 512, 512);
        g.setColor(new Color(0x263A40));
        g.fillRoundRect(20, 20, 472, 472, 72, 72);
        g.setColor(new Color(0x1D3036));
        g.fillRoundRect(26, 26, 460, 460, 64, 64);

        polygon(g, 0x132329, 78, 392, 304, 459, 453, 371, 225, 321);

        // Flat, deliberately limited colors keep the silhouette readable at listing size.
        polygon(g, 0x100F13, 68, 205, 201, 139, 445, 190,
                445, 362, 316, 441, 68, 373);
        polygon(g, 0x70402B, 79, 211, 313, 264, 313, 428, 79, 365);
        polygon(g, 0x4D2B24, 313, 264, 435, 195, 435, 355, 313, 428);
        polygon(g, 0xA96437, 79, 211, 203, 150, 435, 195, 313, 264);

        polygon(g, 0x8E4D2D, 102, 219, 291, 262, 291, 283, 102, 237);
        polygon(g, 0x542F25, 102, 309, 291, 360, 291, 368, 102, 317);
        polygon(g, 0x542F25, 102, 350, 291, 401, 291, 409, 102, 358);
        polygon(g, 0x392322, 335, 305, 414, 259, 414, 267, 335, 314);
        polygon(g, 0x392322, 335, 350, 414, 302, 414, 310, 335, 359);
        polygon(g, 0x70402B, 121, 202, 210, 159, 219, 161, 131, 205);
        polygon(g, 0x70402B, 192, 219, 279, 174, 289, 176, 202, 221);
        polygon(g, 0x70402B, 262, 235, 349, 188, 359, 190, 273, 238);

        polygon(g, 0xF6CF72, 79, 211, 203, 150, 225, 154, 101, 216);
        polygon(g, 0xE7AE49, 79, 211, 101, 216, 101, 371, 79, 365);
        polygon(g, 0xF6CF72, 79, 211, 313, 264, 313, 284, 79, 231);
        polygon(g, 0xC48A35, 313, 264, 435, 195, 435, 215, 313, 284);
        polygon(g, 0xFFE19A, 79, 211, 203, 150, 435, 195, 424, 201,
                204, 160, 92, 214);
        polygon(g, 0xF4C669, 291, 259, 313, 264, 313, 428, 291, 422);
        polygon(g, 0xAC762D, 313, 264, 335, 251, 335, 414, 313, 428);
        polygon(g, 0xD59A3B, 414, 207, 435, 195, 435, 355, 414, 368);
        polygon(g, 0xE9B553, 79, 345, 313, 407, 313, 428, 79, 365);
        polygon(g, 0xBC8431, 313, 407, 435, 335, 435, 355, 313, 428);
        polygon(g, 0xFFE19A, 79, 345, 313, 407, 313, 412, 79, 350);

        polygon(g, 0xDC9E3E, 133, 184, 154, 174, 385, 223, 363, 236);
        polygon(g, 0xFFE19A, 133, 184, 154, 174, 385, 223, 380, 226,
                154, 180, 140, 187);
        polygon(g, 0xD59A3B, 133, 224, 156, 229, 156, 386, 133, 380);
        polygon(g, 0xF4C669, 133, 224, 139, 225, 139, 381, 133, 380);
        polygon(g, 0xB47A2D, 363, 236, 385, 223, 385, 384, 363, 398);

        polygon(g, 0x3D2922, 174, 252, 212, 260, 223, 277, 223, 319,
                210, 331, 174, 321, 163, 306, 163, 266);
        polygon(g, 0xF8CF70, 177, 261, 207, 268, 214, 279, 214, 315,
                207, 322, 177, 314, 171, 304, 171, 269);
        polygon(g, 0xB67B2D, 177, 304, 207, 312, 214, 305, 214, 315,
                207, 322, 177, 314, 171, 304, 171, 294);
        g.setColor(new Color(0x493024));
        g.fillOval(186, 279, 13, 15);
        polygon(g, 0x493024, 190, 288, 196, 289, 199, 304, 187, 301);

        polygon(g, 0xFFE19A, 86, 242, 93, 244, 93, 251, 86, 249);
        polygon(g, 0xFFE19A, 297, 295, 304, 297, 304, 304, 297, 302);
        polygon(g, 0xFFE19A, 86, 355, 93, 357, 93, 364, 86, 362);
        polygon(g, 0xFFE19A, 297, 412, 304, 414, 304, 421, 297, 419);

        var arrow = shape(256, 51, 310, 105, 280, 105, 280, 158,
                232, 158, 232, 105, 202, 105);
        g.setStroke(new BasicStroke(12, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(new Color(0x14252B));
        g.draw(arrow);
        g.setColor(new Color(0xF3EBD2));
        g.fill(arrow);
        polygon(g, 0xC6C7AD, 268, 105, 280, 105, 280, 158, 232, 158,
                232, 148, 268, 148);
        g.dispose();

        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output.getParent());
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IllegalStateException("PNG writer is unavailable");
        }
    }

    private static void polygon(Graphics2D g, int color, double... points) {
        g.setColor(new Color(color));
        g.fill(shape(points));
    }

    private static Path2D shape(double... points) {
        var path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        path.closePath();
        return path;
    }
}
