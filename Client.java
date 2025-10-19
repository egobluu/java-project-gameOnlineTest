import java.io.*;
import java.net.*;
import java.util.Random;
import javax.swing.*;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GamePanel gamePanel;
    private MainMenuPanel mainMenuPanel;
    private Thread receiverThread;
    private String playerName;

    public Client() {
        setTitle("PvP Fighting Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        mainMenuPanel = new MainMenuPanel(this);
        add(mainMenuPanel);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void connectToServer(String name) {
        this.playerName = name;
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(name);

            remove(mainMenuPanel);
            gamePanel = new GamePanel();
            gamePanel.setClient(this);

            // ✅ สุ่มสกิน
            Random random = new Random();
            int number = random.nextInt(3) + 1;
            String pathIm = "/assets/boy" + number + "/";
            gamePanel.setLocalPlayer(name, pathIm);

            add(gamePanel);
            revalidate();
            repaint();
            setTitle("PvP Client - " + name);

            receiverThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        final String message = msg;
                        SwingUtilities.invokeLater(() -> gamePanel.processServerMessage(message));
                    }
                } catch (IOException ignored) {}
            }, "ReceiverThread");
            receiverThread.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot connect to server!\nPlease make sure the server is running.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) out.println(message);
    }

    public void backToMainMenu() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (receiverThread != null && receiverThread.isAlive()) receiverThread.interrupt();
        } catch (Exception ignored) {}

        if (gamePanel != null) remove(gamePanel);
        mainMenuPanel = new MainMenuPanel(this);
        add(mainMenuPanel);
        setTitle("PvP Fighting Game");
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
