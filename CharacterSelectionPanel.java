import javax.swing.*;
import java.awt.*;

public class CharacterSelectionPanel extends JPanel {
    private Client client;
    private String playerName;

    public CharacterSelectionPanel(Client client, String playerName) {
        this.client = client;
        this.playerName = playerName;
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        JLabel titleLabel = new JLabel("CHOOSE YOUR FIGHTER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel selectionGrid = new JPanel(new GridLayout(1, 3, 20, 20));
        selectionGrid.setOpaque(false);
        selectionGrid.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        selectionGrid.add(createCharacterButton("Character 1", "/assets/boy1/preview.png", "boy1"));
        selectionGrid.add(createCharacterButton("Character 2", "/assets/boy2/preview.png", "boy2"));
        selectionGrid.add(createCharacterButton("Character 3", "/assets/boy3/preview.png", "boy3"));
        add(selectionGrid, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        JButton backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> client.backToMainMenu());
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createCharacterButton(String name, String iconPath, String characterId) {
        ImageIcon icon = null;
        try {
            var url = getClass().getResource(iconPath);
            if (url != null) {
                icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath);
        }

        JButton button = new JButton(name, icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setActionCommand(characterId);
        button.addActionListener(e -> client.attemptLogin(playerName, e.getActionCommand()));
        return button;
    }
}