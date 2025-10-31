import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MainMenuPanel extends JPanel {
    private Image background;
    private final Client client;

    public MainMenuPanel(Client client) {
        this.client = client;
        setLayout(null);
        setPreferredSize(new Dimension(800, 600));

        try {
            background = new ImageIcon(getClass().getResource("/assets/pixel-art.png")).getImage();
        } catch (Exception e) {
            setBackground(Color.GRAY);
        }

        JButton join = styled("JOIN");
        join.setBounds(325, 250, 150, 50);
        join.addActionListener(e -> onJoin());
        add(join);

        JButton set = styled("SETTING");
        set.setBounds(325, 320, 150, 50);
        set.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Sound: ON\nMusic: ON\nDifficulty: Normal", "Settings", JOptionPane.INFORMATION_MESSAGE));
        add(set);

        JButton cred = styled("CREDIT");
        cred.setBounds(325, 390, 150, 50);
        cred.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "PvP Fighting Game\nDeveloped by: You!\n© 2025", "Credits", JOptionPane.INFORMATION_MESSAGE));
        add(cred);
    }

    private JButton styled(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("Arial", Font.BOLD, 18));
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(Color.LIGHT_GRAY); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(Color.WHITE); }
        });
        return b;
    }

    private void onJoin() {
        String serverIp = JOptionPane.showInputDialog(this,
                "Enter Server IP (default: localhost):", "localhost");
        if (serverIp == null || serverIp.isEmpty()) serverIp = "localhost";

        client.setServerIp(serverIp);
        String defaultName = client.getPlayerName() != null ? client.getPlayerName() : "Player";
        client.showCharacterSelection(defaultName);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null)
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
}
