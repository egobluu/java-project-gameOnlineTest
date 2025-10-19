import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Player {
    private String name;
    private int x, y, hp;

    private BufferedImage[] walkFrames;
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int ANIMATION_SPEED = 10;
    private final int FRAME_COUNT = 8;

    private int speed = 5;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;

    private boolean isLocalPlayer = false;

    private enum Direction { LEFT, RIGHT, UP, DOWN, IDLE }
    private Direction facing = Direction.DOWN;

    private int drawWidth = 64;
    private int drawHeight = 64;

    private int targetX, targetY;
    private float smoothX, smoothY;
    private final float LERP_SPEED = 0.3f;

    private boolean hasSword = false;

    private static final String[] FRAME_FILENAMES = {
            "boy_down_1.png", "boy_down_2.png",
            "boy_left_1.png", "boy_left_2.png",
            "boy_right_1.png", "boy_right_2.png",
            "boy_up_1.png", "boy_up_2.png"
    };

    public Player(String name, String spriteBasePath) {
        this(name, spriteBasePath, false);
    }

    public Player(String name, String spriteBasePath, boolean isLocalPlayer) {
        this.name = name;
        this.x = 100;
        this.y = 400;
        this.hp = 100;
        this.isLocalPlayer = isLocalPlayer;

        this.targetX = this.x;
        this.targetY = this.y;
        this.smoothX = this.x;
        this.smoothY = this.y;

        loadFrames(spriteBasePath);
    }

    private void loadFrames(String basePath) {
        try {
            walkFrames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                String file = basePath + FRAME_FILENAMES[i];
                var stream = Player.class.getResourceAsStream(file);
                if (stream != null) {
                    walkFrames[i] = ImageIO.read(stream);
                } else {
                    walkFrames[i] = null;
                    System.err.println("❌ Cannot find: " + file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void syncFromServer(int x, int y, int hp, boolean hasSword) {
        this.hp = hp;
        this.hasSword = hasSword;
        if (!isLocalPlayer) {
            this.targetX = x;
            this.targetY = y;
        }
    }

    public void updateMovement(int screenWidth, int screenHeight) {
        if (!isLocalPlayer || isDead()) return;
        if (movingLeft) { x -= speed; facing = Direction.LEFT; }
        if (movingRight) { x += speed; facing = Direction.RIGHT; }
        if (movingUp) { y -= speed; facing = Direction.UP; }
        if (movingDown) { y += speed; facing = Direction.DOWN; }
        smoothX = x; smoothY = y;
    }

    public void clampToGround(int groundTopY, int groundBottomY, int screenWidth) {
        if (x < 0) x = 0;
        if (x + drawWidth > screenWidth) x = screenWidth - drawWidth;
        if (y < groundTopY) y = groundTopY;
        if (y + drawHeight > groundBottomY) y = groundBottomY - drawHeight;
    }

    public void updateAnimation() {
        if (!isLocalPlayer) {
            smoothX += (targetX - smoothX) * LERP_SPEED;
            smoothY += (targetY - smoothY) * LERP_SPEED;
            this.x = Math.round(smoothX);
            this.y = Math.round(smoothY);
        }
        if (isMoving()) {
            animationCounter++;
            if (animationCounter >= ANIMATION_SPEED) {
                animationCounter = 0;
                currentFrame = (currentFrame + 1) % 2;
            }
        } else {
            animationCounter = 0;
            currentFrame = 0;
        }
    }

    public boolean isMoving() { return movingLeft || movingRight || movingUp || movingDown; }
    public boolean isDead() { return hp <= 0; }

    public void setMovingLeft(boolean moving) { movingLeft = moving; if (moving) facing = Direction.LEFT; }
    public void setMovingRight(boolean moving) { movingRight = moving; if (moving) facing = Direction.RIGHT; }
    public void setMovingUp(boolean moving) { movingUp = moving; if (moving) facing = Direction.UP; }
    public void setMovingDown(boolean moving) { movingDown = moving; if (moving) facing = Direction.DOWN; }

    public String getMovementData() {
        StringBuilder sb = new StringBuilder();
        if (movingLeft) sb.append("L");
        if (movingRight) sb.append("R");
        if (movingUp) sb.append("U");
        if (movingDown) sb.append("D");
        return sb.toString();
    }

    public void draw(Graphics g) {
        if (isDead()) return; // ✅ ไม่วาดถ้าตาย
        BufferedImage frame = null;
        int walkFrameIndex = currentFrame;
        switch (facing) {
            case LEFT -> frame = (isMoving() ? walkFrames[2 + walkFrameIndex] : walkFrames[2]);
            case RIGHT -> frame = (isMoving() ? walkFrames[4 + walkFrameIndex] : walkFrames[4]);
            case DOWN -> frame = (isMoving() ? walkFrames[0 + walkFrameIndex] : walkFrames[0]);
            case UP -> frame = (isMoving() ? walkFrames[6 + walkFrameIndex] : walkFrames[6]);
            case IDLE -> frame = walkFrames[0];
        }
        if (frame != null) g.drawImage(frame, x, y, drawWidth, drawHeight, null);

        // วาด HP
        g.setColor(Color.BLACK);
        g.fillRect(x - 2, y - 12, 104, 9);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y - 10, 100, 5);
        g.setColor(hp > 30 ? Color.GREEN : (hp > 15 ? Color.YELLOW : Color.RED));
        g.fillRect(x, y - 10, hp, 5);

        // ชื่อ
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int nameWidth = fm.stringWidth(name);
        g.drawString(name, x + (drawWidth - nameWidth) / 2, y - 15);
    }

    public void pickupSword() { hasSword = true; }
    public boolean hasSword() { return hasSword; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getHp() { return hp; }
    public String getName() { return name; }
}
