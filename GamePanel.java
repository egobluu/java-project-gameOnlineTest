import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;

public class GamePanel extends JPanel implements KeyListener {
    private Image background;
    private JButton backButton;
    private Client client;

    private Player localPlayer;
    private final Map<String, Player> otherPlayers = new HashMap<>();

    private Timer gameTimer;
    private Timer networkTimer;

    private int lastSentX = -1, lastSentY = -1;
    private final int groundTopY = 200;
    private final int groundBottomY = 520;

    private Sword sword = new Sword(380, 350);
    private boolean swordPickedUp = false;

    private final java.util.List<String> rankings = new ArrayList<>();
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private boolean isSpectator = false;

    public GamePanel() {
        var url = getClass().getResource("/assets/background.png");
        if (url != null) background = new ImageIcon(url).getImage();

        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!gameOver && gameStarted && !isSpectator &&
                        localPlayer != null && localPlayer.hasSword()) {
                    client.sendMessage("ATTACK");
                }
            }
        });

        gameTimer = new Timer(1000/60, e -> {
            if (gameStarted && !gameOver) {
                updateGame();
                repaint();
            }
        });
        gameTimer.start();

        networkTimer = new Timer(50, e -> {
            if (gameStarted && !gameOver && !isSpectator) sendMovementToServer();
        });
        networkTimer.start();
    }

    public void setClient(Client client) {
        this.client = client;
        backButton = new JButton("← Back");
        backButton.setBounds(10, 10, 100, 35);
        backButton.addActionListener(e -> {
            gameTimer.stop();
            networkTimer.stop();
            client.backToMainMenu();
        });
        add(backButton);
    }

    public void setLocalPlayer(String name, String spritePath) {
        localPlayer = new Player(name, spritePath, true);
        requestFocusInWindow();
    }

    private void updateGame() {
        if (localPlayer != null && !isSpectator) {
            localPlayer.updateMovement(getWidth(), getHeight());
            localPlayer.clampToGround(groundTopY, groundBottomY, getWidth());
            if (!swordPickedUp && sword.isInRange(localPlayer.getX(), localPlayer.getY())) {
                client.sendMessage("PICKUP_SWORD");
            }
        }
        for (Player p : otherPlayers.values()) p.updateAnimation();
    }

    private void sendMovementToServer() {
        if (client == null || localPlayer == null) return;
        int x = localPlayer.getX(), y = localPlayer.getY();
        if (x != lastSentX || y != lastSentY || localPlayer.isMoving()) {
            client.sendMessage("MOVE:" + x + ":" + y + ":" + localPlayer.getMovementData());
            lastSentX = x; lastSentY = y;
        }
    }

    // ===== รับข้อความจากเซิร์ฟเวอร์ =====
    public void processServerMessage(String message) {
        if (message.startsWith("STATE")) {
            String[] parts = message.split(":");
            Set<String> active = new HashSet<>();
            for (int i = 1; i + 4 < parts.length; i += 5) {
                try {
                    String name = parts[i];
                    int x = Integer.parseInt(parts[i+1]);
                    int y = Integer.parseInt(parts[i+2]);
                    int hp = Integer.parseInt(parts[i+3]);
                    boolean hasSword = Boolean.parseBoolean(parts[i+4]);
                    active.add(name);

                    if (localPlayer != null && name.equals(localPlayer.getName())) {
                        localPlayer.syncFromServer(x, y, hp, hasSword);
                        if (hp <= 0) isSpectator = true;
                    } else {
                        otherPlayers.computeIfAbsent(name, n -> new Player(n, "/assets/boy1/", false));
                        otherPlayers.get(name).syncFromServer(x, y, hp, hasSword);
                    }
                } catch (Exception ignored) {}
            }
            // ลบคนที่ไม่อยู่ใน STATE แล้ว (ออกเกม)
            otherPlayers.keySet().retainAll(active);
            repaint();
        }
        else if (message.startsWith("SWORD_PICKED:")) {
            String who = message.split(":")[1];
            swordPickedUp = true;
            sword.pickup();
            if (localPlayer != null && localPlayer.getName().equals(who)) {
                localPlayer.pickupSword();
            }
            // ให้ฝั่งอื่นที่ไม่ใช่ผู้เก็บ ถือดาบตาม STATE ครั้งถัดไป
            repaint();
        }
        else if (message.startsWith("DEAD:")) {
            String dead = message.split(":")[1];
            if (!rankings.contains(dead)) rankings.add(dead);
            if (localPlayer != null && dead.equals(localPlayer.getName())) {
                isSpectator = true;
            }
        }
        else if (message.startsWith("WINNER:")) {
            String winner = message.split(":")[1];
            if (!rankings.contains(winner)) rankings.add(winner);
            gameOver = true;
            showGameOver();
        }
        else if (message.startsWith("START_GAME")) {
            gameStarted = true;
            swordPickedUp = false; // เริ่มรอบใหม่ให้ดาบโผล่
            repaint();
        }
        else if (message.startsWith("WAITING:")) {
            // optional: แจ้งบน console เฉยๆ
            System.out.println("Waiting players: " + message.split(":")[1] + "/" + 3);
        }
    }

    private void showGameOver() {
        SwingUtilities.invokeLater(() -> {
            removeAll();
            add(new GameOverPanel(rankings, client));
            revalidate();
            repaint();
            System.out.println("✅ GameOverPanel shown");
        });
    }

    // ===== Rendering =====
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);

        if (!swordPickedUp) sword.draw(g);
        for (Player p : otherPlayers.values()) p.draw(g);
        if (localPlayer != null) localPlayer.draw(g);

        if (!gameStarted && !gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("Waiting for players (3 required)", 200, 300);
        }
        if (isSpectator && !gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("You are DEAD - Spectating...", 260, 60);
        }
    }

    // ===== Input =====
    @Override public void keyPressed(KeyEvent e) {
        if (localPlayer == null || !gameStarted || gameOver || isSpectator) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> localPlayer.setMovingLeft(true);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> localPlayer.setMovingRight(true);
            case KeyEvent.VK_W, KeyEvent.VK_UP -> localPlayer.setMovingUp(true);
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> localPlayer.setMovingDown(true);
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        if (localPlayer == null || !gameStarted || gameOver || isSpectator) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> localPlayer.setMovingLeft(false);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> localPlayer.setMovingRight(false);
            case KeyEvent.VK_W, KeyEvent.VK_UP -> localPlayer.setMovingUp(false);
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> localPlayer.setMovingDown(false);
        }
    }
    @Override public void keyTyped(KeyEvent e) {}
}
