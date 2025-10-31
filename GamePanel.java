import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener {
    private Image background;
    private JButton backButton;
    private Client client;
    private Player localPlayer;
    private final Map<String, Player> allPlayers = new LinkedHashMap<>();
    private final List<Sword> swords = new ArrayList<>();
    private Timer gameTimer;
    private Timer networkTimer;
    private int lastSentX = -1, lastSentY = -1;
    private String lastSentFacing = "";
    private boolean lastSentMoving = false;
    private final int groundTopY = 200;
    private final int groundBottomY = 520;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean isSpectator = false;
    private final List<String> rankings = new ArrayList<>();
    private final Map<String, String> characterMap = new HashMap<>(); // playerName -> characterId
    private JButton readyButton;
    private final List<Point> graves = new ArrayList<>();
    private Image graveImage;

    public GamePanel() {
        var url = getClass().getResource("/assets/background.png");
        if (url != null) background = new ImageIcon(url).getImage();

        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setDoubleBuffered(true);

        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);

        try {
            var graveUrl = getClass().getResource("/assets/player/Grave.png");
            if (graveUrl != null) {
                graveImage = new ImageIcon(graveUrl).getImage();
            }
        } catch (Exception e) {
            System.err.println("Grave image not found!");
        }

        readyButton = new JButton("READY");
        readyButton.setBounds(325, 500, 150, 40);
        readyButton.setFont(new Font("Arial", Font.BOLD, 20));
        readyButton.setFocusable(false);
        readyButton.addActionListener(e -> {
            if (client != null) client.sendMessage("READY");
        });
        add(readyButton);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameStarted && !gameOver && !isSpectator && localPlayer != null && localPlayer.hasSword()) {
                    if (client != null) client.sendMessage("ATTACK");
                    localPlayer.attack();
                }
            }
        });
        addHierarchyListener(e -> {
            if (isShowing()) {
                requestFocusInWindow();
            }
        });


        gameTimer = new Timer(1000 / 60, e -> {
            updateLocalPlayerMovement();
            for (Player p : allPlayers.values()) p.update();
            repaint();
        });
        gameTimer.start();

        networkTimer = new Timer(50, e -> sendMovementToServer());
        networkTimer.start();
    }

    public void setClient(Client c) {
        this.client = c;
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
        this.localPlayer = new Player(name, spritePath, true);
        allPlayers.put(name, this.localPlayer);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void updateLocalPlayerMovement() {
        if (localPlayer == null) {
            System.out.println("⚠ localPlayer == null (ไม่พบผู้เล่นควบคุม)");
            return;
        }

        if (!gameStarted) {
            System.out.println("⚠ เกมยังไม่เริ่ม (gameStarted=false)");
            return;
        }

        if (localPlayer != null && gameStarted && !gameOver && !isSpectator) {
            localPlayer.updateMovement(getWidth(), getHeight());
            localPlayer.clampToGround(groundTopY, groundBottomY, getWidth());

            if (!localPlayer.hasSword()) {
                for (int i = 0; i < swords.size(); i++) {
                    Sword sword = swords.get(i);
                    if (!sword.isPickedUp() && sword.isInRange(localPlayer.getX(), localPlayer.getY())) {
                        if (client != null) client.sendMessage("PICKUP_SWORD:" + i);
                        break;
                    }
                }
            }
        }
    }

    private void sendMovementToServer() {
        if (client == null || localPlayer == null || !gameStarted || gameOver || isSpectator) return;

        int x = localPlayer.getX();
        int y = localPlayer.getY();
        String facing = localPlayer.getFacingDirection();
        boolean isMoving = localPlayer.isMoving();

        if (x != lastSentX || y != lastSentY || !facing.equals(lastSentFacing) || isMoving != lastSentMoving) {
            client.sendMessage("MOVE:" + x + ":" + y + ":" + facing + ":" + isMoving);
            lastSentX = x;
            lastSentY = y;
            lastSentFacing = facing;
            lastSentMoving = isMoving;
        }
    }

    public void processServerMessage(String message) {
        try {
            if (message.startsWith("STATE")) {
                if (gameOver) return;
                String[] stateParts = message.split("\\|");
                String playerData = stateParts[0];

                String[] playerTokens = playerData.split(":");
                Set<String> activePlayerNames = new HashSet<>();
                for (int i = 1; i < playerTokens.length; i++) {
                    String[] pData = playerTokens[i].split(",");
                    if (pData.length < 10) continue; // guard

                    String name = pData[0];
                    activePlayerNames.add(name);

                    String charId = pData[5];
                    characterMap.put(name, charId);

                    Player p = allPlayers.computeIfAbsent(
                            name,
                            n -> new Player(n, "/assets/" + charId + "/", localPlayer != null && n.equals(localPlayer.getName()))
                    );

                    p.syncFromServer(
                            Integer.parseInt(pData[1]),
                            Integer.parseInt(pData[2]),
                            Integer.parseInt(pData[3]),
                            Boolean.parseBoolean(pData[4]),
                            Boolean.parseBoolean(pData[6]),
                            Boolean.parseBoolean(pData[7]),
                            pData[8], // actionState
                            pData[9]  // facingDirection
                    );
                }
                allPlayers.keySet().retainAll(activePlayerNames);
                repaint();

                swords.clear();
                if (stateParts.length > 1 && stateParts[1].startsWith("SWORDS")) {
                    String[] swordTokens = stateParts[1].split(":");
                    for (int i = 1; i < swordTokens.length; i++) {
                        String[] sData = swordTokens[i].split(",");
                        if (sData.length < 3) continue;
                        Sword s = new Sword(Integer.parseInt(sData[0]), Integer.parseInt(sData[1]));
                        if (Boolean.parseBoolean(sData[2])) s.pickup();
                        swords.add(s);
                    }
                }

                graves.clear();
                if (stateParts.length > 2 && stateParts[2].startsWith("GRAVES")) {
                    String[] graveTokens = stateParts[2].split(":");
                    for (int i = 1; i < graveTokens.length; i++) {
                        String[] gData = graveTokens[i].split(",");
                        if (gData.length < 2) continue;
                        int x = Integer.parseInt(gData[0]);
                        int y = Integer.parseInt(gData[1]);
                        graves.add(new Point(x, y));
                    }
                }
            } else if (message.equals("START_GAME")) {
                gameStarted = true;
                gameOver = false;
                isSpectator = false;
                rankings.clear();
                repaint();
            } else if (message.startsWith("WINNER:")) {
                String[] parts = message.split(":");
                String winnerName = parts.length >= 2 ? parts[1] : "NO ONE";

                rankings.clear();
                if (parts.length > 2 && parts[2] != null && !parts[2].isEmpty()) {
                    rankings.addAll(Arrays.asList(parts[2].split(",")));
                } else if (!winnerName.equalsIgnoreCase("NO ONE")) {
                    rankings.add(winnerName);
                }

                gameOver = true;
                gameStarted = false;

                client.showGameOverScreen(new ArrayList<>(rankings), new HashMap<>(characterMap));
            }
        } catch (Exception ex) {
            System.err.println("Error processing message: " + message);
            ex.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        if (!gameStarted) {
            drawLobby(g);
        } else {
            readyButton.setVisible(false);
            if (backButton != null) backButton.setVisible(true);

            if (graveImage != null) {
                for (Point p : graves) g.drawImage(graveImage, p.x, p.y, 64, 64, null);
            }
            for (Sword s : swords) s.draw(g);
            for (Player p : allPlayers.values()) if (p.isAlive()) p.draw(g);
        }
    }

    private void drawLobby(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(150, 100, 500, 380);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("LOBBY", 350, 140);

        int yPos = 180;
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        if (localPlayer != null && allPlayers.get(localPlayer.getName()) != null) {
            for (Player p : allPlayers.values()) {
                String status = p.isReady() ? " [READY]" : " [Not Ready]";
                g.setColor(p.isReady() ? Color.GREEN : Color.YELLOW);
                g.drawString(p.getName() + status, 200, yPos);
                yPos += 40;
            }

            if (localPlayer.isReady()) {
                readyButton.setText("CANCEL");
                readyButton.setBackground(Color.ORANGE);
            } else {
                readyButton.setText("READY");
                readyButton.setBackground(Color.CYAN);
            }
        }
        readyButton.setVisible(true);
        if (backButton != null) backButton.setVisible(false);
    }

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
