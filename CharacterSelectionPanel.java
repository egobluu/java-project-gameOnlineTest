import javax.swing.*;
import java.awt.*;

public class CharacterSelectionPanel extends JPanel {
    private final Client client;
    private final String playerName; // ใช้ชื่อจาก Client โดยตรง

    public CharacterSelectionPanel(Client client, String playerName) {
        this.client = client;
        this.playerName = playerName;

        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        // ===== TITLE =====
        JLabel titleLabel = new JLabel("CHOOSE YOUR FIGHTER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // ===== DISPLAY PLAYER NAME =====
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel("You are: " + playerName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 22));
        nameLabel.setForeground(Color.CYAN);
        namePanel.add(nameLabel);

        add(namePanel, BorderLayout.BEFORE_FIRST_LINE);

        // ===== CHARACTER GRID =====
        JPanel selectionGrid = new JPanel(new GridLayout(1, 3, 20, 20));
        selectionGrid.setOpaque(false);
        selectionGrid.setBorder(BorderFactory.createEmptyBorder(80, 80, 50, 80));

        selectionGrid.add(createCharacterButton("Character 1", "/assets/boy1/preview.png", "boy1"));
        selectionGrid.add(createCharacterButton("Character 2", "/assets/boy2/preview.png", "boy2"));
        selectionGrid.add(createCharacterButton("Character 3", "/assets/boy3/preview.png", "boy3"));
        add(selectionGrid, BorderLayout.CENTER);

        // ===== BOTTOM BUTTONS =====
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.addActionListener(e -> client.backToMainMenu());

        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createCharacterButton(String name, String iconPath, String characterId) {
        ImageIcon icon = null;
        try {
            var url = getClass().getResource(iconPath);
            if (url != null) {
                icon = new ImageIcon(
                        new ImageIcon(url).getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH)
                );
            }
        } catch (Exception e) {
            System.err.println("❌ Could not load icon: " + iconPath);
        }

        JButton button = new JButton(name, icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        button.setActionCommand(characterId);

        // 🔹 เมื่อคลิกเลือกตัวละคร
        button.addActionListener(e -> {
            // ใช้ชื่อที่เว็ตมาจาก Client โดยตรง
            client.attemptLogin(playerName, e.getActionCommand());
        });

        return button;
    }
}
