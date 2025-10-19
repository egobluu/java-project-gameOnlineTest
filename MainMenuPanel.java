import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MainMenuPanel extends JPanel {
    private Image background;
    private Image leftCharacter;
    private Image rightCharacter;
    private JButton joinButton;
    private JButton settingButton;
    private JButton creditButton;
    private Client client;

    public MainMenuPanel(Client client) {
        this.client = client;
        setLayout(null); // ใช้ absolute positioning
        setPreferredSize(new Dimension(800, 600));

        // โหลดรูปภาพ 
            // สร้างตัวละครของเรา //E:\GameOnlineJava\assets\pixel-art.png
        background = new ImageIcon("assets/pixel-art.png").getImage();

        // ตัวละคร (ใส่รูปภาพตัวละครตัวอย่าง)
        // leftCharacter = new ImageIcon("assets/character_left.png").getImage();
        // rightCharacter = new ImageIcon("assets/character_right.png").getImage();

        // สร้างปุ่ม JOIN
        joinButton = createStyledButton("JOIN");
        joinButton.setBounds(325, 220, 150, 40);
        joinButton.addActionListener(e -> onJoinClicked());
        add(joinButton);

        // สร้างปุ่ม Setting
        settingButton = createStyledButton("Setting");
        settingButton.setBounds(325, 270, 150, 40);
        settingButton.addActionListener(e -> onSettingClicked());
        add(settingButton);

        // สร้างปุ่ม Credit
        creditButton = createStyledButton("Credit");
        creditButton.setBounds(325, 320, 150, 40);
        creditButton.addActionListener(e -> onCreditClicked());
        add(creditButton);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        // เอฟเฟกต์เมื่อ hover
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.LIGHT_GRAY);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
        
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // วาด background
        g2d.drawImage(background, 0, 0, getWidth(), getHeight(), this);


        // วาดตัวละครซ้าย
        if (leftCharacter != null) {
            g2d.drawImage(leftCharacter, 50, 150, 180, 180, this);
        } else {
            // วงกลมแทนถ้าไม่มีรูป
            g2d.setColor(Color.GRAY);
            g2d.fillOval(50, 150, 180, 180);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Bg", 120, 250);
        }

        // วาดตัวละครขวา
        if (rightCharacter != null) {
            g2d.drawImage(rightCharacter, 570, 150, 180, 180, this);
        } else {
            // วงกลมแทนถ้าไม่มีรูป
            g2d.setColor(Color.GRAY);
            g2d.fillOval(570, 150, 180, 180);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Bh", 640, 250);
        }
    }

    private void onJoinClicked() {
        String name = JOptionPane.showInputDialog(this, "Enter your fighter name:");
        if (name != null && !name.isEmpty()) {
            client.connectToServer(name);
        }
    }

    private void onSettingClicked() {
        JOptionPane.showMessageDialog(this, 
            "Settings\n\nSound: ON\nMusic: ON\nDifficulty: Normal", 
            "Settings", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void onCreditClicked() {
        JOptionPane.showMessageDialog(this, 
            "PvP Fighting Game\n\nDeveloped by: Your Team\nVersion: 1.0\n\n© 2025", 
            "Credits", 
            JOptionPane.INFORMATION_MESSAGE);
    }
}