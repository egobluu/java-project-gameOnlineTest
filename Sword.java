import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Sword {
    private int x, y;
    private boolean pickedUp = false;
    private BufferedImage img;
    private final int W = 40, H = 40;
    private final int PICKUP_RANGE = 50;

    public Sword(int x, int y) {
        this.x = x; this.y = y;
        try {
            // ===== FIXED ===== เพิ่ม /assets/ เข้าไปใน path
            var s = getClass().getResourceAsStream("/assets/player/Sword.png");
            if (s != null) {
                img = ImageIO.read(s);
            }
        } catch (Exception ignored) {}
    }

    public void draw(Graphics g) {
        if (pickedUp) return;
        if (img != null) {
            g.drawImage(img, x, y, W, H, null);
        } else {
            g.setColor(new Color(255, 215, 0));
            int[] xs = {x+W/2, x+W, x+W/2, x};
            int[] ys = {y, y+H/2, y+H, y+H/2};
            g.fillPolygon(xs, ys, 4);
            g.setColor(new Color(255,255,0,80));
            g.fillOval(x-6, y-6, W+12, H+12);
        }
    }

    public boolean isInRange(int px, int py) {
        if (pickedUp) return false;
        int cx = x + W/2, cy = y + H/2;
        int pcx = px + 32, pcy = py + 32;
        double d = Math.hypot(cx - pcx, cy - pcy);
        return d <= PICKUP_RANGE;
    }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPickedUp(boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    public void pickup() { pickedUp = true; }
    public void reset()  { pickedUp = false; }
    public boolean isPickedUp() { return pickedUp; }
}