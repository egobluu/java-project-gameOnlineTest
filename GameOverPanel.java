import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameOverPanel extends JPanel {
    public GameOverPanel(List<String> rankings, Client client) {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JLabel title = new JLabel("🏆 GAME OVER", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.YELLOW);
        add(title, BorderLayout.NORTH);

        // === ส่วนแท่นอันดับ ===
        JPanel podiumPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.BLACK);

                Graphics2D g2 = (Graphics2D) g;
                g2.setFont(new Font("Arial", Font.BOLD, 22));

                int baseY = getHeight() - 100;

                // Podium 2nd
                if (rankings.size() >= 2) {
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.fillRect(150, baseY - 100, 100, 100);
                    g2.setColor(Color.WHITE);
                    g2.drawString("🥈 " + rankings.get(1), 155, baseY - 110);
                }

                // Podium 1st
                if (rankings.size() >= 1) {
                    g2.setColor(Color.YELLOW);
                    g2.fillRect(300, baseY - 150, 100, 150);
                    g2.setColor(Color.BLACK);
                    g2.drawString("🥇 " + rankings.get(0), 305, baseY - 160);
                }

                // Podium 3rd
                if (rankings.size() >= 3) {
                    g2.setColor(new Color(205, 127, 50));
                    g2.fillRect(450, baseY - 70, 100, 70);
                    g2.setColor(Color.WHITE);
                    g2.drawString("🥉 " + rankings.get(2), 455, baseY - 80);
                }
            }
        };
        podiumPanel.setPreferredSize(new Dimension(800, 300));
        add(podiumPanel, BorderLayout.CENTER);

        // === ถ้ามีมากกว่า 3 → แสดงด้านล่าง ===
        if (rankings.size() > 3) {
            JTextArea others = new JTextArea();
            others.setEditable(false);
            others.setBackground(Color.BLACK);
            others.setForeground(Color.WHITE);
            others.setFont(new Font("Monospaced", Font.PLAIN, 16));

            StringBuilder sb = new StringBuilder("Others:\n");
            for (int i = 3; i < rankings.size(); i++) {
                sb.append((i + 1)).append(". ").append(rankings.get(i)).append("\n");
            }
            others.setText(sb.toString());

            add(new JScrollPane(others), BorderLayout.SOUTH);
        }

        // ปุ่ม Back
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.BLACK);
        JButton backBtn = new JButton("Back to Menu");
        backBtn.addActionListener(e -> client.backToMainMenu());
        btnPanel.add(backBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
