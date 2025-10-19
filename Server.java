import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;

    // สถานะผู้เล่น
    private static final Map<String, PlayerState> players = new ConcurrentHashMap<>();
    // สำหรับ broadcast
    private static final Set<PrintWriter> clientWriters = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // การเริ่มเกม
    private static final int REQUIRED_PLAYERS = 3;
    private static volatile boolean gameStarted = false;

    // ดาบกลางแมพ (global)
    private static final int SWORD_X = 380, SWORD_Y = 350, SWORD_RANGE = 50;
    private static volatile boolean swordPickedUp = false;
    private static volatile String swordOwner = null;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Battle Server running on port " + PORT);

        // Thread ส่ง STATE ประจำ
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(60);
                    broadcastAllStates();
                    // เผื่อกรณี disconnect แล้วเหลือ 1 คน แต่ยังไม่ถูกตรวจ
                    if (gameStarted) checkWinner();
                }
            } catch (InterruptedException ignored) {}
        }, "StateBroadcaster").start();

        while (true) {
            new ClientHandler(serverSocket.accept()).start();
        }
    }

    // ===== Client Handler =====
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                clientWriters.add(out);

                // รับชื่อ
                playerName = in.readLine();
                // spawn สุ่มหน่อยๆ
                Random r = new Random();
                int sx = 120 + r.nextInt(520);
                int sy = 320 + r.nextInt(120);
                players.put(playerName, new PlayerState(sx, sy, 100, false, true));

                System.out.println("JOIN: " + playerName + " (" + players.size() + ")");
                // เริ่มเกมเมื่อครบคนและยังไม่เริ่ม
                if (!gameStarted && countAlive() >= REQUIRED_PLAYERS) {
                    gameStarted = true;
                    swordPickedUp = false;
                    swordOwner = null;
                    broadcast("START_GAME");
                } else {
                    broadcast("WAITING:" + players.size());
                }

                // main loop
                String msg;
                while ((msg = in.readLine()) != null) {
                    handleAction(playerName, msg);
                }
            } catch (IOException e) {
                System.out.println("Disconnected: " + playerName);
            } finally {
                // mark ตาย/ออกเกม -> นับเป็น “ตาย”
                if (playerName != null) {
                    PlayerState ps = players.remove(playerName);
                    if (ps != null && ps.isAlive) {
                        ps.isAlive = false;
                        broadcast("DEAD:" + playerName);
                    }
                    // ถ้าเป็นคนถือดาบ -> ดาบวางกลางแมพใหม่ (รอบนี้ไม่มี respawn ระหว่างเกม)
                    if (playerName.equals(swordOwner)) {
                        swordOwner = null;
                        swordPickedUp = false; // ปลด เพื่อให้คนอื่นเก็บใหม่ได้ (ถ้าอยากไม่ให้ เก็บ false ออก)
                    }
                }
                clientWriters.remove(out);
                try { socket.close(); } catch (IOException ignored) {}
                if (gameStarted) checkWinner();
            }
        }
    }

    // ===== Logic ฝั่งเซิร์ฟเวอร์ =====
    private static void handleAction(String name, String action) {
        PlayerState p = players.get(name);
        if (p == null || !p.isAlive) return;

        if (action.startsWith("MOVE:")) {
            // MOVE:x:y:LRUD(อาจจะมีหรือไม่ก็ได้)
            String[] parts = action.split(":");
            if (parts.length >= 3) {
                try {
                    p.x = Integer.parseInt(parts[1]);
                    p.y = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {}
            }
        }
        else if (action.equals("PICKUP_SWORD")) {
            if (!swordPickedUp) {
                // check ระยะ (center ~ center)
                int px = p.x + 32, py = p.y + 32;
                int cx = SWORD_X + 20, cy = SWORD_Y + 20;
                double dist = Math.hypot(px - cx, py - cy);
                if (dist <= SWORD_RANGE) {
                    p.hasSword = true;
                    swordPickedUp = true;
                    swordOwner = name;
                    broadcast("SWORD_PICKED:" + name);
                }
            }
        }
        else if (action.equals("ATTACK")) {
            if (p.hasSword && p.hp > 0) {
                // หาเป้าหมายในระยะ
                for (Map.Entry<String, PlayerState> e : players.entrySet()) {
                    String otherName = e.getKey();
                    PlayerState q = e.getValue();
                    if (otherName.equals(name) || !q.isAlive || q.hp <= 0) continue;

                    if (Math.abs(p.x - q.x) < 50 && Math.abs(p.y - q.y) < 50) {
                        q.hp -= 10;
                        if (q.hp <= 0) {
                            q.hp = 0;
                            q.isAlive = false;
                            broadcast("DEAD:" + otherName);
                        }
                    }
                }
                checkWinner();
            }
        }
        // การส่ง STATE จะถูกส่งจาก StateBroadcaster อยู่แล้ว
    }

    private static void broadcastAllStates() {
        if (players.isEmpty()) return;
        StringBuilder sb = new StringBuilder("STATE");
        for (Map.Entry<String, PlayerState> e : players.entrySet()) {
            PlayerState ps = e.getValue();
            sb.append(":").append(e.getKey())
                    .append(":").append(ps.x)
                    .append(":").append(ps.y)
                    .append(":").append(ps.hp)
                    .append(":").append(ps.hasSword);
        }
        broadcast(sb.toString());
    }

    private static void broadcast(String msg) {
        for (PrintWriter w : clientWriters) w.println(msg);
    }

    private static int countAlive() {
        int c = 0;
        for (PlayerState ps : players.values()) if (ps.isAlive && ps.hp > 0) c++;
        return c;
    }

    private static void checkWinner() {
        int aliveCount = countAlive();
        if (!gameStarted || aliveCount > 1) return;
        if (aliveCount == 0) return; // กรณีสุดโต่ง

        // หา winner
        String winner = null;
        for (Map.Entry<String, PlayerState> e : players.entrySet()) {
            PlayerState ps = e.getValue();
            if (ps.isAlive && ps.hp > 0) { winner = e.getKey(); break; }
        }
        if (winner != null) {
            broadcast("WINNER:" + winner);
            // จบเกม -> ล็อคสถานะให้ทุกคนตายเพื่อหยุด input
            for (PlayerState ps : players.values()) ps.isAlive = false;
            gameStarted = false; // ปิดรอบนี้
        }
    }
}

class PlayerState {
    int x, y, hp;
    boolean hasSword;
    boolean isAlive;

    PlayerState(int x, int y, int hp, boolean hasSword, boolean isAlive) {
        this.x = x; this.y = y; this.hp = hp;
        this.hasSword = hasSword; this.isAlive = isAlive;
    }
}
