import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

public class Player {
    public enum State { IDLE, WALKING, ATTACKING }
    private State currentState = State.IDLE;

    private String name;
    private int x, y, hp;
    private final boolean isLocalPlayer;
    private enum Direction { LEFT, RIGHT, UP, DOWN }
    private Direction facing = Direction.RIGHT;

    private Map<String, BufferedImage[]> animations = new HashMap<>();
    private int currentFrame = 0;
    private int animationTick = 0;
    private int animationSpeed = 5;

    private int speed = 4;
    private boolean movingLeft, movingRight, movingUp, movingDown;

    private int drawWidth = 96, drawHeight = 96;
    private int targetX, targetY;
    private float smoothX, smoothY;
    private static final float LERP_SPEED = 0.3f;
    private boolean hasSword = false;
    private boolean isAlive = true;
    private boolean isReady = false;

    public Player(String name, String spriteBasePath, boolean isLocalPlayer) {
        this.name = name;
        this.x = 100; this.y = 400; this.hp = 100;
        this.isLocalPlayer = isLocalPlayer;
        this.targetX = x; this.targetY = y; this.smoothX = x; this.smoothY = y;
        loadAnimations(spriteBasePath);
    }

    private void loadAnimations(String basePath) {
        String[][] animData = {
                {"Idle_Right",   "boy_Right",       "1"},
                {"Idle_Left",    "boy_Left",        "1"},
                {"Idle_Up",      "boy_up",          "1"},
                {"Idle_Down",    "boy_down",        "1"},
                {"Walk_Right",   "boy_Right",       "8"},
                {"Walk_Left",    "boy_Left",        "8"},
                {"Walk_Up",      "boy_up",          "2"},
                {"Walk_Down",    "boy_down",        "2"},
                {"Attack_Right", "boy_Fight_Right", "6"},
                {"Attack_Left",  "boy_Fight_Left",  "6"}
        };

        for (String[] data : animData) {
            String animName = data[0];
            String folder = data[1];
            int frameCount = Integer.parseInt(data[2]);
            BufferedImage[] frames = new BufferedImage[frameCount];

            for (int i = 0; i < frameCount; i++) {
                String path = basePath + folder + "/" + folder + "_" + i + ".png";

                try {
                    var stream = getClass().getResourceAsStream(path);
                    if (stream != null) {
                        frames[i] = ImageIO.read(stream);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error loading sprite: " + path);
                }
            }
            animations.put(animName, frames);
        }
    }

    public void update() {
        if (!isLocalPlayer) {
            smoothX += (targetX - smoothX) * LERP_SPEED;
            smoothY += (targetY - smoothY) * LERP_SPEED;
            this.x = Math.round(smoothX);
            this.y = Math.round(smoothY);
        }

        if (isLocalPlayer) {
            if (currentState == State.ATTACKING) {
            } else if (movingLeft || movingRight || movingUp || movingDown) {
                currentState = State.WALKING;
            } else {
                currentState = State.IDLE;
            }
        }

        animationTick++;
        if (animationTick > animationSpeed) {
            animationTick = 0;
            currentFrame++;

            BufferedImage[] currentAnimation = animations.get(getCurrentAnimationKey());
            if (currentAnimation == null) return;

            if (currentFrame >= currentAnimation.length) {
                if (currentState == State.ATTACKING) {
                    currentState = State.IDLE;
                }
                currentFrame = 0;
            }
        }
    }

    public void draw(Graphics g) {
        if (!isAlive) return;
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        BufferedImage[] currentAnimation = animations.get(getCurrentAnimationKey());

        if (currentAnimation != null && currentFrame < currentAnimation.length && currentAnimation[currentFrame] != null) {
            g2d.drawImage(currentAnimation[currentFrame], x, y, drawWidth, drawHeight, null);
        }

        g2d.dispose();
        drawUI(g);
    }

    private String getCurrentAnimationKey() {
        String key;
        switch (currentState) {
            case WALKING:   key = "Walk_";   break;
            case ATTACKING: key = "Attack_"; break;
            default:        key = "Idle_";   break;
        }

        String directionKey = facing.toString().substring(0, 1) + facing.toString().substring(1).toLowerCase();
        key += directionKey;

        if (!animations.containsKey(key)) {
            String fallbackKey = "Idle_" + directionKey;
            return fallbackKey;
        }
        return key;
    }

    public void attack() {
        if (currentState != State.ATTACKING) {
            this.currentState = State.ATTACKING;
            this.currentFrame = 0;
            this.animationTick = 0;
        }
    }

    public void syncFromServer(int x, int y, int hp, boolean hasSword, boolean isAlive, boolean isReady, String stateStr, String facingStr) {
        this.hp = hp;
        this.hasSword = hasSword;
        this.isAlive = isAlive;
        this.isReady = isReady;

        if (!isLocalPlayer) {
            this.targetX = x;
            this.targetY = y;

            try {
                this.facing = Direction.valueOf(facingStr.toUpperCase());

                State receivedState = State.valueOf(stateStr.toUpperCase());
                if (this.currentState != State.ATTACKING || receivedState != State.WALKING) {
                    this.setState(receivedState);
                }
            } catch (IllegalArgumentException e) {
                this.facing = Direction.RIGHT;
                this.setState(State.IDLE);
            }
        }
    }

    public void setState(State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.currentFrame = 0;
            this.animationTick = 0;
        }
    }

    public boolean isMoving() {
        return movingLeft || movingRight || movingUp || movingDown;
    }

    public void updateMovement(int screenWidth, int screenHeight) {
        if (!isLocalPlayer || !isAlive) return;
        if (movingLeft)  { x -= speed; }
        if (movingRight) { x += speed; }
        if (movingUp)    { y -= speed; }
        if (movingDown)  { y += speed; }
    }

    public void clampToGround(int groundTopY, int groundBottomY, int screenWidth) {
        if (x < 0) x = 0;
        if (x + drawWidth > screenWidth) x = screenWidth - drawWidth;
        if (y < groundTopY - 32) y = groundTopY - 32;
        if (y + drawHeight > groundBottomY + 32) y = groundBottomY + 32 - drawHeight;
    }

    public void drawUI(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(x + 16, y - 12, 64, 9);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 18, y - 10, 60, 5);
        g.setColor(hp > 30 ? Color.GREEN : (hp > 15 ? Color.YELLOW : Color.RED));
        int hpBarWidth = (int) (60 * (hp / 100.0));
        g.fillRect(x + 18, y - 10, Math.max(0, hpBarWidth), 5);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(name);
        g.drawString(name, x + (drawWidth - w)/2, y - 15);
    }

    public String getFacingDirection() {
        return this.facing.toString();
    }

    public boolean isAlive() { return isAlive; }
    public boolean isReady() { return isReady; }
    public void setMovingLeft(boolean v)  { this.movingLeft = v; if(v) this.facing = Direction.LEFT; }
    public void setMovingRight(boolean v) { this.movingRight = v; if(v) this.facing = Direction.RIGHT; }
    public void setMovingUp(boolean v)    { this.movingUp = v; if(v) this.facing = Direction.UP; }
    public void setMovingDown(boolean v)  { this.movingDown = v; if(v) this.facing = Direction.DOWN; }
    public boolean hasSword() { return hasSword; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getName() { return name; }
}