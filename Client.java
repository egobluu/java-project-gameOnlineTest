import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.BorderLayout;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GamePanel gamePanel;
    private MainMenuPanel mainMenuPanel;
    private CharacterSelectionPanel characterSelectionPanel;
    private Thread receiverThread;
    private String playerName;
    private Map<String, String> characterMap;
    private String serverIp = "localhost";

    public Client() {
        setTitle("PvP Fighting Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        mainMenuPanel = new MainMenuPanel(this);
        setLayout(new BorderLayout());
        add(mainMenuPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

     // ค่า default

    // setter
    public void setServerIp(String ip) {
        this.serverIp = ip;
    }

    public void showCharacterSelection(String name) {
        this.playerName = name;
        getContentPane().removeAll();
        characterSelectionPanel = new CharacterSelectionPanel(this, playerName);
        add(characterSelectionPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void attemptLogin(String name, String characterId) {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("SELECT:" + name + ":" + characterId);

                String response = in.readLine();

                if ("SUCCESS".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        getContentPane().removeAll();
                        gamePanel = new GamePanel();
                        gamePanel.setClient(this);

                        String spritePath = "/assets/" + characterId + "/";
                        gamePanel.setLocalPlayer(name, spritePath);

                        add(gamePanel, BorderLayout.CENTER);
                        revalidate();
                        repaint();
                        setTitle("PvP Client - " + name);
                        startReceiverThread();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "This character is already taken!\nPlease choose another one.",
                                "Selection Error", JOptionPane.ERROR_MESSAGE);
                    });
                    socket.close();
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Cannot connect to server!\nPlease make sure the server is running.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    final String m = msg;
                    System.out.println("📩 Received from server: " + m);
                    if(gamePanel != null) {
                        SwingUtilities.invokeLater(() -> gamePanel.processServerMessage(m));
                    }
                }
            } catch (IOException e) {
                System.out.println("Receiver stopped: " + e.getMessage());
            }
        }, "ReceiverThread");
        receiverThread.start();
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) out.println(message);
    }

    public void showGameOverScreen(List<String> rankings, Map<String, String> characterMap) {
        this.characterMap = characterMap;
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        GameOverPanel gameOverPanel = new GameOverPanel(rankings, this, characterMap);
        add(gameOverPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void backToMainMenu() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
                receiverThread.join(500);
            }
        } catch (Exception ignored) {}

        getContentPane().removeAll();
        setLayout(new BorderLayout());
        mainMenuPanel = new MainMenuPanel(this);
        add(mainMenuPanel, BorderLayout.CENTER);
        setTitle("PvP Fighting Game");
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
