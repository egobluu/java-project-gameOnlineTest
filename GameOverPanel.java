import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class GameOverPanel extends JPanel {
    private final List<String> rankings;              // [Winner, 2nd, 3rd, ...]
    private final Client client;
    private final Map<String, String> characterMap;   // playerName -> characterId
    private final Map<String, BufferedImage> spriteCache = new HashMap<>();

    public GameOverPanel(List<String> rankings, Client client, Map<String, String> characterMap) {
        this.rankings = rankings != null ? rankings : new ArrayList<>();
        this.client = client;
        this.characterMap = characterMap != null ? characterMap : new HashMap<>();

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        JLabel title = new JLabel(" GAME OVER ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 60));
        title.setForeground(new Color(255, 223, 0));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel podiumPanel = createPodiumPanel();
        podiumPanel.setPreferredSize(new Dimension(800, 420));
        add(podiumPanel, BorderLayout.CENTER);

        // แสดงอันดับที่เกิน 3 ลงไป (ถ้ามี)
        if (rankings.size() > 3) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (int i = 3; i < rankings.size(); i++) {
                model.addElement((i + 1) + ". " + rankings.get(i));
            }
            JList<String> list = new JList<>(model);
            list.setFont(new Font("Arial", Font.PLAIN, 16));
            JScrollPane sp = new JScrollPane(list);
            sp.setBorder(BorderFactory.createTitledBorder("Other Placements"));
            add(sp, BorderLayout.EAST);
        }

        JButton backButton = new JButton("Back to Main Menu");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.setBackground(new Color(100, 149, 237));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        backButton.addActionListener(e -> {
            rankings.clear();
            client.backToMainMenu();
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.BLACK);
        bottomPanel.add(backButton);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createPodiumPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.BLACK);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int baseY = h - 80;
                int podiumWidth = 140;
                int podiumSpacing = 80;

                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(0, baseY + 1, w, 4);

                g2.setFont(new Font("Arial", Font.BOLD, 28));

                // 🥇🥈🥉 แสดง podium สูงสุด 3 คนเท่านั้น
                int podiumCount = Math.min(rankings.size(), 3);

                for (int i = 0; i < podiumCount; i++) {
                    String player = rankings.get(i);

                    int x, height;
                    Color color;

                    switch (i) {
                        case 0 -> { // 🥇 Center
                            x = w / 2 - podiumWidth / 2;
                            height = 180;
                            color = new Color(255, 215, 0);
                        }
                        case 1 -> { // 🥈 Left
                            x = w / 2 - podiumWidth - podiumSpacing / 2;
                            height = 120;
                            color = new Color(192, 192, 192);
                        }
                        case 2 -> { // 🥉 Right
                            x = w / 2 + podiumSpacing / 2;
                            height = 90;
                            color = new Color(205, 127, 50);
                        }
                        default -> throw new IllegalStateException("Unexpected rank: " + i);
                    }

                    g2.setColor(color);
                    g2.fillRoundRect(x, baseY - height, podiumWidth, height, 16, 16);
                    drawCharacter(g2, player, x + podiumWidth / 2 - 50, baseY - height - 110);
                    g2.setColor(Color.WHITE);
                    drawCenteredString(g2, player, x + podiumWidth / 2, baseY - height - 100);
                }

                // ถ้าไม่มีข้อมูล
                if (rankings.isEmpty()) {
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.setFont(new Font("Arial", Font.PLAIN, 22));
                    drawCenteredString(g2, "No results to display", w / 2, h / 2);
                }

                g2.dispose();
            }
        };
    }

    private void drawCharacter(Graphics2D g2, String playerName, int x, int y) {
        String charId = characterMap.getOrDefault(playerName, "boy1");
        BufferedImage sprite = spriteCache.get(charId);

        if (sprite == null) {
            try {
                String path = "/assets/" + charId + "/boy_down/boy_down_0.png";  // idle frame
                var stream = getClass().getResourceAsStream(path);
                if (stream != null) {
                    sprite = ImageIO.read(stream);
                    spriteCache.put(charId, sprite);
                } else {
                    System.err.println("❌ Sprite not found: " + path + " for " + playerName);
                }
            } catch (Exception e) {
                System.err.println("❌ Error loading sprite for " + playerName + " (" + charId + ")");
            }
        }

        if (sprite != null) {
            g2.drawImage(sprite, x, y, 100, 100, null);
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(x + 10, y + 10, 80, 80);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, x - textWidth / 2, y);
    }
}
