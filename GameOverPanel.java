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

                // เส้นพื้น
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(0, baseY + 1, w, 4);

                g2.setFont(new Font("Arial", Font.BOLD, 28));

                // 🥈 2nd
                if (rankings.size() >= 2) {
                    String player2 = rankings.get(1);
                    int xLeft = w / 2 - podiumWidth - podiumSpacing / 2;
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRoundRect(xLeft, baseY - 120, podiumWidth, 120, 16, 16);
                    drawCharacter(g2, player2, xLeft + podiumWidth / 2 - 50, baseY - 120 - 110);
                    g2.setColor(Color.WHITE);
                    drawCenteredString(g2, "" + player2, xLeft + podiumWidth / 2, baseY - 130);
                }

                // 🥇 1st
                if (rankings.size() >= 1) {
                    String player1 = rankings.get(0);
                    int xCenter = w / 2 - podiumWidth / 2;
                    g2.setColor(new Color(255, 215, 0));
                    g2.fillRoundRect(xCenter, baseY - 180, podiumWidth, 180, 16, 16);
                    drawCharacter(g2, player1, xCenter + podiumWidth / 2 - 50, baseY - 180 - 120);
                    g2.setColor(Color.WHITE);
                    drawCenteredString(g2, "" + player1, xCenter + podiumWidth / 2, baseY - 190);
                }

                // 🥉 3rd
                if (rankings.size() >= 3) {
                    String player3 = rankings.get(2);
                    int xRight = w / 2 + podiumSpacing / 2;
                    g2.setColor(new Color(205, 127, 50));
                    g2.fillRoundRect(xRight, baseY - 90, podiumWidth, 90, 16, 16);
                    drawCharacter(g2, player3, xRight + podiumWidth / 2 - 50, baseY - 90 - 110);
                    g2.setColor(Color.WHITE);
                    drawCenteredString(g2, "" + player3, xRight + podiumWidth / 2, baseY - 100);
                }

                // กรณีมีอันดับน้อยกว่า 3 ให้ช่วยบอกผู้ใช้
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
