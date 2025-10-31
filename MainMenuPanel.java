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
            System.err.println("MainMenu background not found.");
            setBackground(Color.GRAY);
        }

        // ช่องกรอก IP
        JLabel ipLabel = new JLabel("Server IP:");
        ipLabel.setBounds(280, 170, 100, 30);
        ipLabel.setFont(new Font("Arial", Font.BOLD, 16));
        ipLabel.setForeground(Color.WHITE);

        JButton join = styled("JOIN");
        join.setBounds(325, 220, 150, 40);
        JButton set  = styled("Setting");
        set.setBounds(325, 270, 150, 40);
        JButton cred = styled("Credit");
        cred.setBounds(325, 320, 150, 40);

        join.addActionListener(e -> onJoin());

        set.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Settings\n\nSound: ON\nMusic: ON\nDifficulty: Normal",
                "Settings", JOptionPane.INFORMATION_MESSAGE));

        cred.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "PvP Fighting Game\n\nDeveloped by: You!\nVersion: 1.0\n\n© 2025",
                "Credits", JOptionPane.INFORMATION_MESSAGE));

        add(join);
        add(set);
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
        String name = JOptionPane.showInputDialog(this, "Enter your fighter name:");
        if (name != null && !name.trim().isEmpty()) {

            String serverIp = JOptionPane.showInputDialog(this,
                    "Enter Server IP (default: localhost):", "localhost");
            if (serverIp == null || serverIp.isEmpty()) {
                serverIp = "localhost";
            }

            client.setServerIp(serverIp);
            client.showCharacterSelection(name.trim());
        }
    }


    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
