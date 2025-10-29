import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.awt.Point;

class PlayerState {
    int x, y, hp;
    boolean hasSword;
    boolean isAlive;
    String characterId;
    boolean isReady;
    String actionState = "IDLE";
    String facingDirection = "RIGHT";

    PlayerState(int x, int y, int hp, boolean hasSword, boolean isAlive, String characterId) {
        this.x = x; this.y = y; this.hp = hp;
        this.hasSword = hasSword; this.isAlive = isAlive;
        this.characterId = characterId;
        this.isReady = false;
    }
}

public class Server {
    private static final int PORT = 12345;
    private static final Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private static final List<SwordState> swords = new CopyOnWriteArrayList<>();
    private static final Set<PrintWriter> clientWriters = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final int REQUIRED_PLAYERS = 3;
    private static volatile boolean gameStarted = false;
    private static final List<Point> graves = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Battle Server running on port " + PORT);

        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(50); // Broadcast state more frequently
                    if (gameStarted) {
                        checkWinner();
                    }
                    broadcastFullState();
                }
            } catch (InterruptedException ignored) {}
        }, "StateBroadcaster").start();

        // Game Logic Loop
        new Thread(() -> {
            try {
                while(true) {
                    Thread.sleep(100);
                    if (!gameStarted) {
                        checkGameStart();
                    } else {
                        updateServerLogic();
                    }
                }
            } catch(InterruptedException ignored) {}
        }, "GameLogicThread").start();

        while (true) new ClientHandler(serverSocket.accept()).start();
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;
        private String characterId;

        ClientHandler(Socket s) { this.socket = s; }

        @Override public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String selectionMessage = in.readLine();
                if (selectionMessage == null || !selectionMessage.startsWith("SELECT:")) { return; }

                String[] parts = selectionMessage.split(":");
                this.playerName = parts[1];
                this.characterId = parts[2];

                if (isCharacterTaken(characterId)) {
                    System.out.println("REJECT: " + playerName + " tried to pick " + characterId + " (taken).");
                    out.println("ERROR:TAKEN");
                    return;
                }

                out.println("SUCCESS");
                clientWriters.add(out);

                Random r = new Random();
                int sx = 120 + r.nextInt(520);
                int sy = 320 + r.nextInt(120);
                players.put(playerName, new PlayerState(sx, sy, 100, false, true, characterId));
                System.out.println("JOIN: " + playerName + " as " + characterId + " (" + players.size() + ")");

                String msg;
                while ((msg = in.readLine()) != null) {
                    handleAction(playerName, msg);
                }
            } catch (IOException ignored) {
            } finally {
                if (playerName != null) {
                    players.remove(playerName);
                    System.out.println("LEAVE: " + playerName + " (" + players.size() + ")");
                }
                clientWriters.remove(out);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static synchronized boolean isCharacterTaken(String charId) {
        if (gameStarted) return false;
        for (PlayerState p : players.values()) {
            if (p.characterId.equals(charId)) { return true; }
        }
        return false;
    }

    // ===== MODIFIED ===== รับข้อมูลใหม่จาก Client
    private static void handleAction(String name, String action) {
        PlayerState p = players.get(name);
        if (p == null) return;

        if (action.startsWith("MOVE:")) {
            if (!p.isAlive || !gameStarted) return;
            String[] parts = action.split(":");
            if (parts.length >= 5) {
                try {
                    p.x = Integer.parseInt(parts[1]);
                    p.y = Integer.parseInt(parts[2]);
                    p.facingDirection = parts[3];
                    boolean isMoving = Boolean.parseBoolean(parts[4]);

                    if (p.actionState.equals("IDLE") || p.actionState.equals("WALKING")) {
                        p.actionState = isMoving ? "WALKING" : "IDLE";
                    }

                } catch (NumberFormatException ignored) {}
            }
        } else if (action.equals("READY")) {
            if (!gameStarted) {
                p.isReady = !p.isReady;
                System.out.println("STATUS: " + name + " is now " + (p.isReady ? "Ready" : "Not Ready"));
            }
        } else if (action.startsWith("PICKUP_SWORD:")) {
            if (!p.isAlive || !gameStarted) return;
            try {
                int swordIndex = Integer.parseInt(action.split(":")[1]);
                if (!p.hasSword && swords.size() > swordIndex) {
                    SwordState sword = swords.get(swordIndex);
                    if (!sword.isPickedUp) {
                        int px = p.x + 32, py = p.y + 32;
                        int cx = sword.x + 20, cy = sword.y + 20;
                        if (Math.hypot(px - cx, py - cy) <= 50) {
                            sword.isPickedUp = true;
                            sword.ownerName = name;
                            p.hasSword = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        } else if (action.equals("ATTACK")) {
            if (p.hasSword && p.hp > 0 && p.isAlive && gameStarted) {
                p.actionState = "ATTACKING";
                boolean hitSomeone = false;
                for (Map.Entry<String, PlayerState> e : players.entrySet()) {
                    String otherName = e.getKey();
                    PlayerState otherPlayer = e.getValue();
                    if (otherName.equals(name) || !otherPlayer.isAlive) continue;

                    if (Math.abs(p.x - otherPlayer.x) < 70 && Math.abs(p.y - otherPlayer.y) < 70) {
                        otherPlayer.hp -= 25;
                        hitSomeone = true;
                        if (otherPlayer.hp <= 0) {
                            otherPlayer.hp = 0;
                            otherPlayer.isAlive = false;
                            graves.add(new Point(otherPlayer.x, otherPlayer.y));
                        }
                        break;
                    }
                }
                if (hitSomeone) {
                    p.hasSword = false;
                    for (SwordState s : swords) {
                        if (name.equals(s.ownerName)) {
                            Random r = new Random();
                            s.x = 100 + r.nextInt(600);
                            s.y = 250 + r.nextInt(200);
                            s.isPickedUp = false;
                            s.ownerName = null;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void updateServerLogic() {
        for (PlayerState p : players.values()) {
            if (p.actionState.equals("ATTACKING")) {
                p.actionState = "IDLE";
            }
        }
    }

    private static synchronized void checkGameStart() {
        if (gameStarted || players.size() < REQUIRED_PLAYERS) return;
        boolean allReady = players.values().stream().allMatch(p -> p.isReady);

        if (allReady) {
            gameStarted = true;
            graves.clear();
            for (PlayerState p : players.values()) {
                p.hp = 100;
                p.isAlive = true;
                p.hasSword = false;
            }
            swords.clear();
            Random r = new Random();
            int swordsToSpawn = Math.max(1, players.size() - 1);
            for (int i = 0; i < swordsToSpawn; i++) {
                int sx = 100 + r.nextInt(600);
                int sy = 250 + r.nextInt(200);
                swords.add(new SwordState(sx, sy));
            }
            broadcast("START_GAME");
        }
    }

    // ===== MODIFIED ===== ส่งข้อมูล State และ Direction เพิ่ม
    private static void broadcastFullState() {
        if (players.isEmpty()) return;
        StringBuilder sb = new StringBuilder("STATE");

        for (Map.Entry<String, PlayerState> e : players.entrySet()) {
            PlayerState ps = e.getValue();
            sb.append(":").append(e.getKey())
                    .append(",").append(ps.x).append(",").append(ps.y)
                    .append(",").append(ps.hp).append(",").append(ps.hasSword)
                    .append(",").append(ps.characterId).append(",").append(ps.isAlive)
                    .append(",").append(ps.isReady)
                    .append(",").append(ps.actionState)
                    .append(",").append(ps.facingDirection); // ข้อมูลใหม่
        }
        sb.append("|SWORDS");
        for (SwordState s : swords) {
            sb.append(":").append(s.x).append(",").append(s.y).append(",").append(s.isPickedUp);
        }
        sb.append("|GRAVES");
        for (Point g : graves) {
            sb.append(":").append(g.x).append(",").append(g.y);
        }
        broadcast(sb.toString());
    }

    private static void broadcast(String msg) {
        for (PrintWriter w : clientWriters) {
            w.println(msg);
        }
    }

    private static void checkWinner() {
        if (!gameStarted) return;

        // หาผู้เล่นที่ยังมีชีวิต
        List<String> alivePlayers = players.entrySet().stream()
                .filter(e -> e.getValue().isAlive)
                .map(Map.Entry::getKey)
                .toList();

        // เกมจบถ้ามีผู้รอด <= 1
        if (alivePlayers.size() <= 1 && players.size() >= REQUIRED_PLAYERS) {
            String winnerName = alivePlayers.isEmpty() ? "NO ONE" : alivePlayers.get(0);

            // ✅ จัดอันดับใหม่
            List<String> rankingList = new ArrayList<>();
            if (!winnerName.equals("NO ONE")) {
                rankingList.add(winnerName); // ใส่ผู้ชนะเป็นที่ 1
            }

            // ผู้ที่ตาย -> ตามลำดับเวลาที่ตาย (ใช้ลิสต์ graves)
            for (Point g : graves) {
                // หาชื่อคนที่ตาย ณ จุดนี้
                players.entrySet().stream()
                        .filter(e -> !e.getValue().isAlive)
                        .filter(e -> !rankingList.contains(e.getKey()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .ifPresent(rankingList::add);
            }

            // ถ้ายังมีคนตายแต่ไม่ถูกใส่ -> เติมเข้ามา (กันพลาด)
            players.entrySet().stream()
                    .filter(e -> !e.getValue().isAlive)
                    .map(Map.Entry::getKey)
                    .filter(name -> !rankingList.contains(name))
                    .forEach(rankingList::add);

            // ✅ ส่ง Winner + Rankings ครบ
            String msg = "WINNER:" + winnerName + ":" + String.join(",", rankingList);
            broadcast(msg);

            gameStarted = false;
        }
    }

}