// Server.java (Fixed disconnect logic)
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static int PORT = 12345; // Cho phép thay đổi port
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final List<ClientHandler> waitingPlayers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Cho phép người dùng chọn port khác
        if (args.length > 0) {
            try {
                PORT = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Port không hợp lệ, sử dụng port mặc định: " + PORT);
            }
        }
        
        ServerSocket serverSocket = new ServerSocket(PORT);
        
        // Hiển thị thông tin server
        System.out.println("==========================================");
        System.out.println("🎮 BLACKJACK MULTIPLAYER SERVER 🎮");
        System.out.println("==========================================");
        System.out.println("Server đang chạy trên:");
        
        try {
            // Lấy IP của máy chủ
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("📍 IP Address: " + hostAddress);
            System.out.println("🔌 Port: " + PORT);
            System.out.println("🌐 Clients có thể kết nối bằng IP: " + hostAddress);
        } catch (Exception e) {
            System.out.println("🔌 Port: " + PORT);
            System.out.println("⚠️  Không thể lấy IP address: " + e.getMessage());
        }
        
        System.out.println("==========================================");
        System.out.println("Đang chờ người chơi kết nối... (Cần 2 người)");
        System.out.println("==========================================\n");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                synchronized (waitingPlayers) {
                    waitingPlayers.add(clientHandler);
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("🎯 Player '" + clientHandler.getPlayerName() + 
                                     "' từ " + clientIP + " đã kết nối và đang chờ...");
                    System.out.println("Số người chơi đang chờ: " + waitingPlayers.size() + "/2");

                    if (waitingPlayers.size() == 2) {
                        ClientHandler player1 = waitingPlayers.remove(0);
                        ClientHandler player2 = waitingPlayers.remove(0);
                        String p1IP = player1.getClientIP();
                        String p2IP = player2.getClientIP();
                        System.out.println("\n==========================================");
                        System.out.println("🎮 GAME SESSION STARTED!");
                        System.out.println("Player 1: '" + player1.getPlayerName() + "' (" + p1IP + ")");
                        System.out.println("Player 2: '" + player2.getPlayerName() + "' (" + p2IP + ")");
                        System.out.println("==========================================\n");
                        
                        pool.execute(new GameSession(player1, player2));
                    } else {
                        System.out.println("Đang chờ thêm " + (2 - waitingPlayers.size()) + " người chơi...\n");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Lỗi khi thiết lập kết nối với client ban đầu: " + e.getMessage());
            }
        }
    }
}

class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Blackjack game;
    private boolean player1WantsNewGame = false;
    private boolean player2WantsNewGame = false;
    
    // Cờ để đảm bảo phiên chỉ kết thúc một lần
    private volatile boolean sessionEnded = false;

    GameSession(ClientHandler p1, ClientHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.game = new Blackjack(2);
    }
    
    @Override
    public void run() {
        
        try {
            player1.setGameSession(this, 0);
            player2.setGameSession(this, 1);

            new Thread(player1).start();
            new Thread(player2).start();

            game.startGame();
            broadcastGameState();
        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng trong game session: " + e.getMessage());
            endSession();
        }
    }
    
    public synchronized void handleCommand(String command, int playerId) {
        if (sessionEnded) return; // Nếu phiên đã kết thúc, không xử lý gì thêm

        // XỬ LÝ BETTING COMMANDS - THÊM
        if (command.startsWith("BET:")) {
            try {
                int betAmount = Integer.parseInt(command.substring(4));
                if (game.placeBet(playerId, betAmount)) {
                    String playerName = (playerId == 0) ? player1.getPlayerName() : player2.getPlayerName();
                    System.out.println("Player " + playerId + " (" + playerName + ") đặt cược " + betAmount + "$");
                    broadcastGameState();
                } else {
                    // Gửi thông báo lỗi về client
                    ClientHandler player = (playerId == 0) ? player1 : player2;
                    player.sendObject("BETTING_ERROR:Không đủ tiền hoặc số cược không hợp lệ");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid bet amount: " + command);
                ClientHandler player = (playerId == 0) ? player1 : player2;
                player.sendObject("BETTING_ERROR:Số tiền không hợp lệ");
            }
            return;
        }

        // Xử lý game commands (HIT/STAND)
        if (command.equals("HIT") || command.equals("STAND")) {
            if (game.getCurrentGameState() != Blackjack.GameState.PLAYER_TURN || 
                game.getCurrentPlayerIndex() != playerId) {
                return;
            }
            if (command.equals("HIT")) {
                game.playerHit();
            } else {
                game.playerStand();
            }
            
            broadcastGameState();
            return;
        }

        // Xử lý new game commands
        switch(command) {
            case "REQUEST_NEW_GAME":
            case "ACCEPT_NEW_GAME":
                handleNewGameRequest(playerId);
                break;
            case "DECLINE_NEW_GAME":
                player1.sendObject("NEW_GAME_DECLINED");
                player2.sendObject("NEW_GAME_DECLINED");
                resetNewGameRequests();
                break;
        }
    }
    
    public void broadcastGameState() {
        if (sessionEnded) return;
        player1.sendObject(createUpdateForPlayer(0));
        player2.sendObject(createUpdateForPlayer(1));
    }
    
    private GameStateUpdate createUpdateForPlayer(int playerId) {
        // Tạo dealer hand để gửi (ẩn bài đầu nếu chưa kết thúc)
        ArrayList<Card> dealerHandToSend = new ArrayList<>();
        if (game.getCurrentGameState() == Blackjack.GameState.GAME_OVER || 
            game.getCurrentGameState() == Blackjack.GameState.DEALER_TURN) {
            // Hiển thị tất cả bài dealer khi game kết thúc hoặc đến lượt dealer
            dealerHandToSend.addAll(game.getDealerHand());
        } else {
            // Ẩn bài đầu tiên của dealer
            dealerHandToSend.add(new Card("BACK", ""));
            if (game.getDealerHand() != null && game.getDealerHand().size() > 1) {
                for (int i = 1; i < game.getDealerHand().size(); i++) {
                    dealerHandToSend.add(game.getDealerHand().get(i));
                }
            }
        }
        
        // Tính dealer sum để gửi
        int dealerSumToSend = 0;
        if (game.getCurrentGameState() == Blackjack.GameState.GAME_OVER || 
            game.getCurrentGameState() == Blackjack.GameState.DEALER_TURN) {
            dealerSumToSend = game.getDealerSum();
        } else if (game.getDealerHand() != null && game.getDealerHand().size() > 1) {
            // Chỉ tính điểm của các lá không bị ẩn
            for (int i = 1; i < game.getDealerHand().size(); i++) {
                dealerSumToSend += game.getDealerHand().get(i).getValue();
            }
        }
        
        List<String> names = List.of(player1.getPlayerName(), player2.getPlayerName());

        // SỬA: Đảm bảo không truyền null cho money và bet data
        return new GameStateUpdate(
            names,                                    // playersNames
            game.getPlayersHands(),                  // playersHands
            game.getPlayersSums(),                   // playersSums
            game.getPlayersResults(),                // playersResults
            game.getPlayersMoneys() != null ? game.getPlayersMoneys() : new ArrayList<>(), // playersMoneys - SỬA
            game.getPlayersBets() != null ? game.getPlayersBets() : new ArrayList<>(),     // playersBets - SỬA
            dealerHandToSend,                        // dealerHand
            dealerSumToSend,                         // dealerSum
            game.getCurrentGameState(),              // gameState
            game.getCurrentPlayerIndex(),            // currentPlayerTurn
            playerId                                 // myPlayerId
        );
    }
    
    // --- PHƯƠNG THỨC XỬ LÝ NGẮT KẾT NỐI ĐÃ SỬA LẠI ---
    public synchronized void removePlayer(ClientHandler disconnectedPlayer) {
        if (sessionEnded) {
            return; // Nếu đã xử lý rồi thì không làm gì nữa
        }
        System.out.println("Xử lý ngắt kết nối từ: " + disconnectedPlayer.getPlayerName());
        
        ClientHandler remainingPlayer = (disconnectedPlayer == player1) ? player2 : player1;
        if (remainingPlayer != null && remainingPlayer.isConnected()) {
            remainingPlayer.sendObject("OPPONENT_DISCONNECTED");
        }
        
        // Gọi hàm kết thúc phiên tập trung
        endSession();
    }
    
    // Hàm để kết thúc phiên và dọn dẹp
    private void endSession() {
        this.sessionEnded = true;
        player1.closeConnection();
        player2.closeConnection();
        System.out.println("Game session đã kết thúc và dọn dẹp.");
    }
    
    // --- Giữ nguyên các hàm còn lại ---
    private void handleNewGameRequest(int playerId) {
        if (playerId == 0) player1WantsNewGame = true;
        else player2WantsNewGame = true;

        if (player1WantsNewGame && player2WantsNewGame) {
            startNewGame();
        } else if (playerId == 0) {
            player2.sendObject("NEW_GAME_REQUESTED");
        } else {
            player1.sendObject("NEW_GAME_REQUESTED");
        }
    }

    private void startNewGame() {
        System.out.println("Cả hai người chơi đã đồng ý. Bắt đầu ván mới...");
        resetNewGameRequests();
        game.startGame();
        broadcastGameState();
    }

    private void resetNewGameRequests() {
        player1WantsNewGame = false;
        player2WantsNewGame = false;
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameSession session;
    private int playerId;
    private String playerName;

    ClientHandler(Socket socket) throws IOException, ClassNotFoundException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.playerName = (String) in.readObject();
    }

    public void setGameSession(GameSession session, int playerId) {
        this.session = session;
        this.playerId = playerId;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
    
    public void closeConnection() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Bỏ qua lỗi khi đóng kết nối
        }
    }

    @Override
    public void run() {
        try {
            while (isConnected()) {
                String command = (String) in.readObject();
                 if (session != null) {
                    session.handleCommand(command, this.playerId);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
             if (session != null) {
                // Chỉ gọi removePlayer nếu nó chưa được xử lý
                session.removePlayer(this);
            }
        }
    }
    
    public String getPlayerName() { return this.playerName; }
    
    // Thêm phương thức để lấy IP của client
    public String getClientIP() {
        return socket.getInetAddress().getHostAddress();
    }

    public void sendObject(Serializable object) {
        if (!isConnected()) return;
        try {
            out.writeObject(object);
            out.reset();
            out.flush();
        } catch (IOException e) {
            if (session != null) {
                session.removePlayer(this);
            }
        }
    }
}