import javax.swing.*;

public class GameClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PvP Game Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // ใช้ GamePanel เป็นหน้าหลัก
            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
