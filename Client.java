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
    private Thread receiverThread;

    private GamePanel gamePanel;
    private MainMenuPanel mainMenuPanel;
    private CharacterSelectionPanel characterSelectionPanel;

    private String playerName;
    private String characterId;
    private String serverIp = "localhost";
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

    // === UI Navigation ===
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

    public void backToMainMenu() {
        getContentPane().removeAll();
        mainMenuPanel = new MainMenuPanel(this);
        add(mainMenuPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // === Connection Handling ===
    public void attemptLogin(String name, String characterId) {
        this.playerName = name;
        this.characterId = characterId;

        new Thread(() -> {
            try {
                // ✅ ปิด socket เก่าก่อน (ถ้ายังเชื่อมต่ออยู่)
                if (connected && socket != null && !socket.isClosed()) {
                    try { socket.close(); } catch (IOException ignored) {}
                    connected = false;
                    System.out.println("♻️ Reconnecting to server...");
                }

                // ✅ เปิดการเชื่อมต่อใหม่เสมอ
                socket = new Socket(serverIp, 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                // ส่งข้อมูลตัวละคร
                out.println("SELECT:" + playerName + ":" + characterId);

                // รอ response
                String response = in.readLine();

                if (response == null) {
                    JOptionPane.showMessageDialog(this, "Server did not respond.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (response.startsWith("ASSIGNED_NAME:")) {
                    playerName = response.substring("ASSIGNED_NAME:".length());
                    System.out.println("✅ Joined as " + playerName);

                    if (receiverThread != null && receiverThread.isAlive()) {
                        receiverThread.interrupt();
                    }

                    // ✅ เริ่ม thread รับข้อความ
                    receiverThread = new Thread(this::receiveMessages);
                    receiverThread.start();

                    SwingUtilities.invokeLater(() -> showGamePanel(playerName, characterId));

                } else if ("ERROR:GAME_ALREADY_STARTED".equals(response)) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot join. The game has already started!",
                            "Game in Progress", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Unexpected server response: " + response,
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }

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
            System.err.println("📴 Lost connection: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        if (out != null && connected) out.println(msg);
    }

    public void setServerIp(String ip) { this.serverIp = ip; }
    public String getServerIp() { return serverIp; }
    public String getPlayerName() { return playerName; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
