import javax.swing.*;
import java.awt.*;

public class
CharacterSelectionPanel extends JPanel {
    private final Client client;
    private final String playerName;

    public CharacterSelectionPanel(Client client, String playerName) {
        this.client = client;
        this.playerName = playerName;

        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        JLabel titleLabel = new JLabel("CHOOSE YOUR FIGHTER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 3, 20, 20));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(80, 80, 50, 80));

        grid.add(createButton("Character 1", "/assets/boy1/preview.png", "boy1"));
        grid.add(createButton("Character 2", "/assets/boy2/preview.png", "boy2"));
        grid.add(createButton("Character 3", "/assets/boy3/preview.png", "boy3"));

        add(grid, BorderLayout.CENTER);

        JButton back = new JButton("← Back to Menu");
        back.setFont(new Font("Arial", Font.BOLD, 18));
        back.addActionListener(e -> client.backToMainMenu());

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(back);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton createButton(String name, String path, String id) {
        ImageIcon icon = null;
        try {
            var url = getClass().getResource(path);
            if (url != null) {
                icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH));
            }
        } catch (Exception ignored) {}

        JButton b = new JButton(name, icon);
        b.setFont(new Font("Arial", Font.BOLD, 18));
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.addActionListener(e -> client.attemptLogin(playerName, id));
        return b;
    }
}
