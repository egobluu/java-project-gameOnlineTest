import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private GamePanel gamePanel;
    private MainMenuPanel mainMenuPanel;
    private CharacterSelectionPanel characterSelectionPanel;

    private Thread receiverThread;

    private String playerName;
    private String characterId;
    private String serverIp = "localhost";  // ✅ จำค่า IP ล่าสุด
    private boolean connected = false;

    public Client() {
        setTitle("PvP Fighting Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        mainMenuPanel = new MainMenuPanel(this);
        add(mainMenuPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ============================================================
    // ✅ ส่วนควบคุมหน้าจอ
    // ============================================================
    public void showCharacterSelection(String name) {
        this.playerName = name;
        getContentPane().removeAll();
        characterSelectionPanel = new CharacterSelectionPanel(this, playerName);
        add(characterSelectionPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showGamePanel(String name, String characterId) {
        getContentPane().removeAll();
        gamePanel = new GamePanel();
        gamePanel.setClient(this);
        gamePanel.setLocalPlayer(name, "/assets/" + characterId + "/");
        add(gamePanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showGameOverScreen(List<String> rankings, Map<String, String> characterMap) {
        getContentPane().removeAll();
        GameOverPanel gameOverPanel = new GameOverPanel(rankings, this, characterMap);
        add(gameOverPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ✅ กลับไปเลือกตัวละครทันที (ไม่ต้องกรอก IP ใหม่)
    public void backToMainMenu() {
        if (connected && socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }

        getContentPane().removeAll();
        characterSelectionPanel = new CharacterSelectionPanel(this, playerName != null ? playerName : "Player");
        add(characterSelectionPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ============================================================
    // ✅ การเชื่อมต่อเซิร์ฟเวอร์
    // ============================================================
    public void attemptLogin(String name, String characterId) {
        this.playerName = name;
        this.characterId = characterId;

        new Thread(() -> {
            try {
                System.out.println("🔌 Connecting to server at " + serverIp + "...");
                socket = new Socket(serverIp, 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                // ส่งข้อมูลตัวละคร
                out.println("SELECT:" + playerName + ":" + characterId);
                // ===== หลังส่ง SELECT ไป server =====
                String response = in.readLine();

                if (response.startsWith("ASSIGNED_NAME:")) {
                    playerName = response.substring("ASSIGNED_NAME:".length());
                    System.out.println("✅ Assigned name: " + playerName);

                    // เริ่มรับข้อความอื่น ๆ จาก server ต่อ
                    receiverThread = new Thread(this::receiveMessages);
                    receiverThread.start();

                    SwingUtilities.invokeLater(() -> showGamePanel(playerName, characterId));

                } else if ("ERROR:GAME_ALREADY_STARTED".equals(response)) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot join. The game has already started!",
                            "Game in Progress", JOptionPane.WARNING_MESSAGE);
                    socket.close();
                    return;

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to join server. Please try again.",
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    socket.close();
                    return;
                }

                System.out.println("✅ Joined server successfully as " + playerName + " (" + characterId + ")");
                SwingUtilities.invokeLater(() -> showGamePanel(playerName, characterId));

                receiverThread = new Thread(this::receiveMessages);
                receiverThread.start();

            } catch (IOException e) {
                System.err.println("❌ Failed to connect: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Unable to connect to server: " + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void receiveMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (gamePanel != null) {
                    gamePanel.processServerMessage(line);
                }
            }
        } catch (IOException e) {
            System.err.println("📴 Disconnected from server: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    public void sendMessage(String msg) {
        if (out != null && connected) {
            out.println(msg);
        }
    }

    // ============================================================
    // ✅ getter / setter ต่าง ๆ
    // ============================================================
    public void setServerIp(String ip) {
        this.serverIp = ip;
    }

    public String getServerIp() {
        return serverIp;
    }

    public boolean isConnected() {
        return connected;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
