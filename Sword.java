import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Sword {
    private int x, y;
    private boolean pickedUp = false;
    private BufferedImage swordImage;

    public Sword(int x, int y) {
        this.x = x; this.y = y;
        loadImage();
    }

    private void loadImage() {
        try {
            var stream = getClass().getResourceAsStream("/assets/player/8.png");
            if (stream != null) swordImage = ImageIO.read(stream);
        } catch (Exception e) { swordImage = null; }
    }

    public void draw(Graphics g) {
        if (pickedUp) return;
        if (swordImage != null) g.drawImage(swordImage, x, y, 40, 40, null);
        else {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, 40, 40);
        }
    }

    public boolean isInRange(int px, int py) {
        if (pickedUp) return false;
        int dx = (x+20) - (px+32);
        int dy = (y+20) - (py+32);
        return Math.sqrt(dx*dx+dy*dy) <= 50;
    }

    public void pickup() { pickedUp = true; }
    public boolean isPickedUp() { return pickedUp; }
}
