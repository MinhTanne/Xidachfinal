// BlackjackClient.java (with Debugging for Sound)
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;

/**
 * Class BlackjackClient - Client game Blackjack với giao diện đồ họa
 * Kết nối đến server, hiển thị game UI, xử lý input từ người chơi
 * Bao gồm: animation lá bài, âm thanh, multiplayer networking
 */
public class BlackjackClient {
    
    /**
     * Inner Class AnimatingCard - Quản lý hiệu ứng di chuyển của lá bài
     * Tạo animation mượt mà khi chia bài từ deck đến vị trí người chơi
     */
    private class AnimatingCard {
        Card card;
        double startX, startY, destX, destY;
        double currentX, currentY;
        int totalSteps = 30;
        int currentStep = 0;

        /**
         * Constructor tạo animation cho lá bài
         * @param card Lá bài cần animate
         * @param startX Tọa độ X bắt đầu
         * @param startY Tọa độ Y bắt đầu  
         * @param destX Tọa độ X đích
         * @param destY Tọa độ Y đích
         */
        AnimatingCard(Card card, int startX, int startY, int destX, int destY) {
            this.card = card;
            this.startX = startX;
            this.startY = startY;
            this.destX = destX;
            this.destY = destY;
            this.currentX = startX;
            this.currentY = startY;
        }

        public boolean update() {
            currentStep++;
            if (currentStep >= totalSteps) {
                currentX = destX;
                currentY = destY;
                return true;
            }
            double progress = (double) currentStep / totalSteps;
            double easedProgress = 1 - Math.pow(1 - progress, 3);
            currentX = startX + (destX - startX) * easedProgress;
            currentY = startY + (destY - startY) * easedProgress;
            return false;
        }

        public void draw(Graphics g) {
            Image img = getCardImage(this.card);
            if (img != null) {
                g.drawImage(img, (int)currentX, (int)currentY, 110, 154, null);
            }
        }
    }

    // --- Các hằng số và thuộc tính ---
    private String serverHost = "localhost";
    private int serverPort = 12345;
    // Thay đổi vị trí deck về gần sát bên phải
    private static final int DECK_X = 1250, DECK_Y = 50; 
    private static final int FRAME_WIDTH = 1400;
    private static final int FRAME_HEIGHT = 900;


    
    private final String INITIAL_DEAL_SOUND = "resource/sounds/chiabai.wav";
    private final String HIT_SOUND = "resource/sounds/rutbai.wav";
    private final String CLICK_SOUND = "resource/sounds/click.wav";
    private final String WIN_SOUND = "resource/sounds/win.wav";
    private final String LOSE_SOUND = "resource/sounds/lose.wav";

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JFrame frame = new JFrame("Blackjack");
    private JPanel gamePanel;
    private JPanel buttonPanel = new JPanel();
    private JButton hitButton = new JButton("Rút");
    private JButton standButton = new JButton("Dừng");
    private JButton newGameButton = new JButton("Ván mới");
    private JButton acceptButton = new JButton("Đồng ý");
    private JButton declineButton = new JButton("Từ chối");
    private JButton disconnectButton = new JButton("Ngắt kết nối");
    private JSlider volumeSlider;
    private JLabel statusLabel = new JLabel("Điền tên của bạn để bắt đầu.");
    private JButton bet10Button = new JButton("$10");
    private JButton bet20Button = new JButton("$20"); 
    private JButton bet50Button = new JButton("$50");
    private JButton bet100Button = new JButton("$100");
    private JLabel moneyLabel = new JLabel("Tiền: $1000");
    private JLabel betLabel = new JLabel("Cược: $0");
    
    // --- Các biến trạng thái ---
    private String playerName;
    private List<String> playersNames = new ArrayList<>();
    private List<ArrayList<Card>> playersHands = new ArrayList<>();
    private List<Integer> playersSums = new ArrayList<>();
    private List<String> playersResults = new ArrayList<>();
    private ArrayList<Card> dealerHand = new ArrayList<>();
    private int dealerSum;
    private Blackjack.GameState currentGameState;
    private Blackjack.GameState prevGameState = null;
    private int currentPlayerTurn;
    private int myPlayerId = -1;
    private Timer animationTimer;
    private List<AnimatingCard> animatingCards = new CopyOnWriteArrayList<>();
    private GameStateUpdate pendingUpdate = null;

    private List<Integer> playersMoneys = new ArrayList<>();
    private List<Integer> playersBets = new ArrayList<>();

    // Thêm các hằng số còn thiếu
    private static final Color TABLE_COLOR = new Color(0, 100, 0); // Xanh lá cây đậm
    private static final Color FELT_COLOR = new Color(34, 139, 34); // Xanh lá nhạt hơn
    private static final Font TITLE_FONT = new Font("Serif", Font.BOLD, 24);
    private static final Font PLAYER_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font CARD_COUNT_FONT = new Font("Arial", Font.PLAIN, 14);

    
    private int calculatePlayerScore(ArrayList<Card> hand) {
        if (hand == null || hand.isEmpty()) return 0;
        
        int sum = 0;
        int aceCount = 0;
        
        // Tính tổng điểm thô
        for (Card card : hand) {
            sum += card.getValue();
            if (card.isAce()) {
                aceCount++;
            }
        }
        
      
        while (sum > 21 && aceCount > 0) {
            sum -= 10; // Chuyển A từ 11 thành 1
            aceCount--;
        }
        
        return sum;
    }

    // Hàm khởi tạo client
    public BlackjackClient(String playerName, int volumeLevel, String serverIP, int serverPort) {
        this.playerName = playerName;
        this.serverHost = serverIP;
        this.serverPort = serverPort;
        initializeUI();
        initializeAnimationTimer();
        
        // Đặt âm lượng từ HomeScreen
        SoundManager.getInstance().setVolume(volumeLevel / 100.0f);
        
        new Thread(this::connectToServer).start();
    }

    // Constructor mới nhận tên từ HomeScreen (localhost mặc định)
    public BlackjackClient(String playerName, int volumeLevel) {
        this.playerName = playerName;
        initializeUI();
        initializeAnimationTimer();
        
        // Đặt âm lượng từ HomeScreen
        SoundManager.getInstance().setVolume(volumeLevel / 100.0f);
        
        new Thread(this::connectToServer).start();
    }

    // hàm khởi tạo client với tên người chơi   
    public BlackjackClient() {
        getPlayerName();
        initializeUI();
        initializeAnimationTimer();
        new Thread(this::connectToServer).start();
    }
    
    private void getPlayerName() {
        // Chỉ gọi nếu chưa có tên (từ constructor cũ)
        if (playerName == null || playerName.trim().isEmpty()) {
            while (playerName == null || playerName.trim().isEmpty()) {
                playerName = JOptionPane.showInputDialog(
                    frame, "Nhập tên của bạn:", "Chào mừng đến với Blackjack", JOptionPane.PLAIN_MESSAGE
                );
                if (playerName == null) System.exit(0); 
            }
        }
        frame.setTitle("Blackjack - " + playerName);
    }
    
    private void initializeAnimationTimer() {
        animationTimer = new Timer(15, e -> {
            animatingCards.removeIf(AnimatingCard::update);
            gamePanel.repaint();

            if (animatingCards.isEmpty()) {
                animationTimer.stop();
                if (pendingUpdate != null) {
                    processStateUpdate(pendingUpdate);
                    pendingUpdate = null;
                }
            }
        });
    }

    private void initializeUI() {
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawGame(g2d);
            }
        };
        
        frame = new JFrame("Blackjack - " + (playerName != null ? playerName : ""));
        frame.setVisible(true);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(TABLE_COLOR);
        frame.add(gamePanel, BorderLayout.CENTER);

        setupButtonPanel();
        frame.add(buttonPanel, BorderLayout.SOUTH);
        
        disconnectButton.addActionListener(e -> {
            // Quay về trang chủ thay vì thoát hoàn toàn
            frame.dispose();
            SwingUtilities.invokeLater(() -> new HomeScreen());
        });

        hitButton.addActionListener(e -> {
            SoundManager.getInstance().playSoundEffect(HIT_SOUND);
            sendCommand("HIT");
        });
        standButton.addActionListener(e -> {
            SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
            sendCommand("STAND");
        });
        newGameButton.addActionListener(e -> {
            SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
            sendCommand("REQUEST_NEW_GAME");
            statusLabel.setText("Đã gửi yêu cầu. Đang chờ đối thủ...");
            newGameButton.setEnabled(false);
        });
        acceptButton.addActionListener(e -> {
            SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
            sendCommand("ACCEPT_NEW_GAME");
        });
        declineButton.addActionListener(e -> {
            SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
            sendCommand("DECLINE_NEW_GAME");
        });
        
        setButtonStateForGameplay(false);
        newGameButton.setVisible(false);
        acceptButton.setVisible(false);
        declineButton.setVisible(false);
    }
    
    private void setupButtonPanel() {
        buttonPanel.setBackground(new Color(139, 69, 19)); // Màu nâu gỗ
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Style cho các button hiện tại
        styleButton(hitButton, new Color(220, 20, 60), Color.WHITE);
        styleButton(standButton, new Color(255, 140, 0), Color.WHITE);
        styleButton(newGameButton, new Color(50, 205, 50), Color.WHITE);
        styleButton(acceptButton, new Color(30, 144, 255), Color.WHITE);
        styleButton(declineButton, new Color(220, 20, 60), Color.WHITE);
        styleButton(disconnectButton, new Color(105, 105, 105), Color.WHITE);

        // Style cho betting buttons
        styleButton(bet10Button, new Color(34, 139, 34), Color.WHITE);
        styleButton(bet20Button, new Color(34, 139, 34), Color.WHITE);
        styleButton(bet50Button, new Color(34, 139, 34), Color.WHITE);
        styleButton(bet100Button, new Color(34, 139, 34), Color.WHITE);

        disconnectButton.setText("🏠 Về trang chủ");
        disconnectButton.setPreferredSize(new Dimension(140, 35));

        // Thêm action listeners cho betting buttons
        bet10Button.addActionListener(e -> placeBet(10));
        bet20Button.addActionListener(e -> placeBet(20));
        bet50Button.addActionListener(e -> placeBet(50));
        bet100Button.addActionListener(e -> placeBet(100));

        // Thêm tất cả buttons
        buttonPanel.add(hitButton);
        buttonPanel.add(standButton);
        buttonPanel.add(bet10Button);
        buttonPanel.add(bet20Button);
        buttonPanel.add(bet50Button);
        buttonPanel.add(bet100Button);
        buttonPanel.add(newGameButton);
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);
        buttonPanel.add(disconnectButton);

        // Money và bet labels
        moneyLabel.setForeground(Color.YELLOW);
        moneyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        betLabel.setForeground(Color.ORANGE);
        betLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        buttonPanel.add(moneyLabel);
        buttonPanel.add(betLabel);

        // Volume control
        JLabel volumeLabel = new JLabel("  Âm lượng:");
        volumeLabel.setForeground(Color.WHITE);
        volumeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        buttonPanel.add(volumeLabel);
        
        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setBackground(new Color(139, 69, 19));
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.addChangeListener(e -> {
            float volume = volumeSlider.getValue() / 100.0f;
            SoundManager.getInstance().setVolume(volume);
        });
        buttonPanel.add(volumeSlider);
        
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setText("Đang kết nối tới server...");
        buttonPanel.add(statusLabel);
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100, 35));
        
        // Thêm hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    private void drawGame(Graphics2D g2d) {
        // Vẽ background với gradient
        drawTableBackground(g2d);
        
        // Vẽ logo/title
        drawGameTitle(g2d);
        
        // Vẽ deck
        drawDeck(g2d);
        
        // Vẽ dealer area
        drawDealerArea(g2d);
        
        // Vẽ player areas
        if (playersNames != null && !playersNames.isEmpty()) {
            drawPlayerArea(g2d, 0);
            if (playersNames.size() > 1) {
                drawPlayerArea(g2d, 1);
            }
        }
        
        // Vẽ animation cards
        for (AnimatingCard ac : animatingCards) {
            ac.draw(g2d);
        }
        
        // Vẽ game results
        if (currentGameState == Blackjack.GameState.GAME_OVER && playersResults != null && !playersResults.isEmpty()) {
            drawGameResults(g2d);
        }
        
        // Vẽ turn indicator
        drawTurnIndicator(g2d);
    }

    private void drawTableBackground(Graphics2D g2d) {
        // Tạo gradient background
        GradientPaint gradient = new GradientPaint(
            0, 0, TABLE_COLOR,
            FRAME_WIDTH, FRAME_HEIGHT, FELT_COLOR
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
        
        // Vẽ viền bàn
        g2d.setColor(new Color(139, 69, 19)); // Nâu gỗ
        g2d.setStroke(new BasicStroke(8));
        g2d.drawRoundRect(10, 10, FRAME_WIDTH - 20, FRAME_HEIGHT - 120, 20, 20);
        
        // Vẽ oval trung tâm (khu vực chơi)
        g2d.setColor(new Color(0, 80, 0, 100));
        g2d.fillOval(100, 100, FRAME_WIDTH - 200, FRAME_HEIGHT - 300);
    }

    private void drawGameTitle(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        g2d.setFont(TITLE_FONT);
        FontMetrics metrics = g2d.getFontMetrics();
        String title = "♠ BLACKJACK TABLE ♥";
        int titleWidth = metrics.stringWidth(title);
        g2d.drawString(title, (FRAME_WIDTH - titleWidth) / 2, 40);
    }

    private void drawDeck(Graphics2D g2d) {
        Image backImage = getCardImage(new Card("BACK", ""));
        if (backImage != null) {
            // Vẽ shadow cho deck
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(DECK_X + 5, DECK_Y + 5, 110, 154, 10, 10);
            
            // Vẽ deck chính
            g2d.drawImage(backImage, DECK_X, DECK_Y, 110, 154, null);
            
            // Vẽ label (điều chỉnh vị trí cho phù hợp)
            g2d.setColor(Color.WHITE);
            g2d.setFont(CARD_COUNT_FONT);
            FontMetrics metrics = g2d.getFontMetrics();
            String deckLabel = "DECK";
            int labelWidth = metrics.stringWidth(deckLabel);
            // Center label under the deck
            g2d.drawString(deckLabel, DECK_X + (110 - labelWidth) / 2, DECK_Y + 170);
            
            // Vẽ thêm hiệu ứng 3D cho deck (tùy chọn)
            g2d.setColor(new Color(139, 69, 19, 150)); // Màu nâu nhạt
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(DECK_X - 2, DECK_Y - 2, 114, 158, 8, 8);
        }
    }

    private void drawDealerArea(Graphics2D g2d) {
        int dealerAreaY = 80;
        int dealerCardsX = 520;
        
        // Vẽ background cho dealer area
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(dealerCardsX - 20, dealerAreaY - 10, 600, 200, 15, 15);
        
        // Vẽ dealer label
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Tính điểm dealer chính xác từ lá bài
        int correctDealerSum = dealerSum; // Mặc định dùng giá trị từ server
        if (currentGameState == Blackjack.GameState.GAME_OVER && dealerHand != null && !dealerHand.isEmpty()) {
            correctDealerSum = calculatePlayerScore(dealerHand); // Tính lại chính xác
        }
        
        String dealerText = "DEALER: " + (currentGameState == Blackjack.GameState.GAME_OVER ? correctDealerSum : "?");
        g2d.drawString(dealerText, dealerCardsX, dealerAreaY);
        
        // Vẽ dealer cards với shadow
        if (dealerHand != null) {
            for (int i = 0; i < dealerHand.size(); i++) {
                int cardX = dealerCardsX + i * 80;
                int cardY = dealerAreaY + 20;
                
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.fillRoundRect(cardX + 3, cardY + 3, 110, 154, 10, 10);
                
                // Card
                g2d.drawImage(getCardImage(dealerHand.get(i)), cardX, cardY, 110, 154, null);
            }
        }
    }

    private void drawPlayerArea(Graphics2D g2d, int playerId) {
        if (playersNames == null || playersNames.size() <= playerId || 
            playersHands == null || playersHands.size() <= playerId) return;
        
        String displayName = playersNames.get(playerId);
        int playerAreaX = 150 + playerId * 600;
        int playerAreaY = 400;
        
        // Xác định có hiển thị bài thật hay không
        boolean shouldShowRealCards = shouldShowPlayerCards(playerId);
        
        // Vẽ background cho player area
        Color playerBgColor = (playerId == myPlayerId) ? 
            new Color(255, 215, 0, 50) : new Color(255, 255, 255, 30);
        g2d.setColor(playerBgColor);
        g2d.fillRoundRect(playerAreaX - 30, playerAreaY - 50, 500, 300, 20, 20);
        
        // Vẽ player name và score
        if (playerId == myPlayerId) {
            displayName += " (BẠN)";
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.WHITE);
        }
        
        g2d.setFont(PLAYER_FONT);
        
        // Hiển thị điểm số
        String playerInfo;
        if (shouldShowRealCards) {
            int sum = (playersSums != null && playersSums.size() > playerId) ? playersSums.get(playerId) : 0;
            
            if (playersHands != null && playersHands.size() > playerId && playersHands.get(playerId) != null) {
                sum = calculatePlayerScore(playersHands.get(playerId));
            }
            
            playerInfo = displayName + ": " + sum + " điểm";
        } else {
            playerInfo = displayName + ": ???";
        }
        g2d.drawString(playerInfo, playerAreaX, playerAreaY - 20);
        
        // Hiển thị thông tin tiền và cược - SỬA với null checks chi tiết hơn
        if (playersMoneys != null && playersMoneys.size() > playerId && 
            playersBets != null && playersBets.size() > playerId) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String moneyInfo = "💰 $" + playersMoneys.get(playerId) + " | Cược: $" + playersBets.get(playerId);
            g2d.drawString(moneyInfo, playerAreaX, playerAreaY - 5);
        } else {
            // Hiển thị placeholder nếu chưa có dữ liệu money/bet
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.ITALIC, 12));
            g2d.drawString("Đang tải thông tin tài chính...", playerAreaX, playerAreaY - 5);
        }
        
        // Vẽ player cards
        ArrayList<Card> hand = playersHands.get(playerId);
        for (int i = 0; i < hand.size(); i++) {
            int cardX = playerAreaX + i * 50;
            int cardY = playerAreaY;
            
            // Shadow cho cards
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(cardX + 3, cardY + 3, 110, 154, 10, 10);
            
            // Quyết định hiển thị mặt bài hay mặt sau
            Image cardImage;
            if (shouldShowRealCards) {
                cardImage = getCardImage(hand.get(i));
            } else {
                cardImage = getCardImage(new Card("BACK", ""));
            }
            
            if (cardImage != null) {
                g2d.drawImage(cardImage, cardX, cardY, 110, 154, null);
            }
            
            // Vẽ icon ẩn cho bài úp
            if (!shouldShowRealCards && playerId != myPlayerId) {
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.setFont(new Font("Serif", Font.BOLD, 24));
                g2d.drawString("🔒", cardX + 40, cardY + 85);
            }
        }
        
        // Vẽ player status
        if (currentGameState == Blackjack.GameState.PLAYER_TURN && currentPlayerTurn == playerId) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(">>> LƯỢT CHƠI <<<", playerAreaX + 50, playerAreaY + 180);
        }
        
        // Hiển thị trạng thái bài đang bị ẩn
        if (!shouldShowRealCards && playerId != myPlayerId) {
            g2d.setColor(new Color(255, 215, 0, 180));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("🎭 Bài được ẩn", playerAreaX + 80, playerAreaY + 200);
        }
    }

    private void drawGameResults(Graphics2D g2d) {
        // Vẽ background cho results
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(FRAME_WIDTH/2 - 250, 500, 500, 200, 20, 20);
        
        // Viền vàng
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(FRAME_WIDTH/2 - 250, 500, 500, 200, 20, 20);
        
        // Title
        g2d.setColor(Color.ORANGE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics metrics = g2d.getFontMetrics();
        String title = "🏆 KẾT QUẢ 🏆";
        int titleWidth = metrics.stringWidth(title);
        g2d.drawString(title, (FRAME_WIDTH - titleWidth) / 2, 540);
        
        // Results
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        metrics = g2d.getFontMetrics();
        for(int i = 0; i < playersResults.size(); i++){
            // Tính điểm chính xác từ lá bài thay vì dùng playersSums cũ
            int correctScore = 0;
            if (playersHands != null && i < playersHands.size() && playersHands.get(i) != null) {
                correctScore = calculatePlayerScore(playersHands.get(i));
            } else if (playersSums != null && i < playersSums.size()) {
                correctScore = playersSums.get(i);
            }
            
            String resultText = playersNames.get(i) + ": " + playersResults.get(i) + " (" + correctScore + " điểm)";
            
            // Màu theo kết quả
            if (playersResults.get(i).contains("Thắng")) {
                g2d.setColor(Color.GREEN);
            } else if (playersResults.get(i).contains("Thua")) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.YELLOW);
            }
            
            int resultWidth = metrics.stringWidth(resultText);
            g2d.drawString(resultText, (FRAME_WIDTH - resultWidth) / 2, 580 + i * 35);
        }
    }

    private void drawTurnIndicator(Graphics2D g2d) {
        if (currentGameState == Blackjack.GameState.PLAYER_TURN && playersNames != null && currentPlayerTurn < playersNames.size()) {
            // Vẽ indicator cho người chơi hiện tại
            String currentPlayer = playersNames.get(currentPlayerTurn);
            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Lượt của: " + currentPlayer, 50, 50);
            
            // Vẽ arrow indicator
            int arrowX = 120 + currentPlayerTurn * 600;
            int arrowY = 350;
            drawArrow(g2d, arrowX, arrowY);
        }
    }

    private void drawArrow(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(4));
        
        // Vẽ mũi tên chỉ xuống
        int[] xPoints = {x, x + 20, x + 10, x + 10, x - 10, x - 10};
        int[] yPoints = {y, y, y + 10, y + 30, y + 30, y + 10};
        g2d.fillPolygon(xPoints, yPoints, 6);
    }

    private Image getCardImage(Card card) {
        String cardPath = "resource/cards/" + card.toString() + ".png";
        if (card.value.equals("BACK")) cardPath = "resource/cards/BACK.png";
        try {
            return new ImageIcon(new File(cardPath).toURI().toURL()).getImage();
        } catch (MalformedURLException e) {
            e.printStackTrace(); 
            return null;
        }
    }

    private void connectToServer() {
        try {
            statusLabel.setText("Đang kết nối tới " + serverHost + ":" + serverPort + "...");
            
            // Thử kết nối với timeout
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), 10000); // 10 second timeout
            
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(this.playerName);
            out.flush();
            
            statusLabel.setText("✅ Đã kết nối! Đang chờ người chơi khác...");
            System.out.println("🎯 Kết nối thành công tới " + serverHost + ":" + serverPort);
            
            listenForServerUpdates();
            
        } catch (ConnectException e) {
            String errorMsg = "❌ Không thể kết nối tới server!\n\n" +
                           "🔍 Nguyên nhân có thể:\n" +
                           "• Server chưa được khởi động\n" +
                           "• IP hoặc Port không đúng\n" +
                           "• Firewall chặn kết nối\n" +
                           "• Không cùng mạng LAN\n\n" +
                           "💡 Hãy thử:\n" +
                           "1. Kiểm tra server đã chạy chưa\n" +
                           "2. Ping IP: " + serverHost + "\n" +
                           "3. Tắt firewall tạm thời\n" +
                           "4. Dùng localhost nếu cùng máy";
            
            statusLabel.setText("❌ Không thể kết nối tới server");
            
            JOptionPane.showMessageDialog(frame, errorMsg, 
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            
            System.err.println("Connection refused to " + serverHost + ":" + serverPort);
            
        } catch (SocketTimeoutException e) {
            String errorMsg = "⏰ Timeout khi kết nối!\n\n" +
                           "Server không phản hồi trong 10 giây.\n" +
                           "Kiểm tra mạng và thử lại.";
            
            statusLabel.setText("⏰ Timeout kết nối");
            JOptionPane.showMessageDialog(frame, errorMsg, 
                "Timeout", JOptionPane.WARNING_MESSAGE);
                
        } catch (UnknownHostException e) {
            String errorMsg = "🌐 Không tìm thấy server!\n\n" +
                           "IP address không tồn tại: " + serverHost + "\n" +
                           "Kiểm tra lại địa chỉ IP.";
            
            statusLabel.setText("🌐 IP không tồn tại");
            JOptionPane.showMessageDialog(frame, errorMsg, 
                "IP không hợp lệ", JOptionPane.ERROR_MESSAGE);
                
        } catch (IOException e) {
            String errorMsg = "💥 Lỗi mạng: " + e.getMessage() + "\n\n" +
                           "Kiểm tra kết nối mạng và thử lại.";
            
            statusLabel.setText("💥 Lỗi kết nối");
            JOptionPane.showMessageDialog(frame, errorMsg, 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            
            e.printStackTrace();
        }
    }
    
    private void sendCommand(String command) {
        try {
            if (out != null) {
                out.writeObject(command);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForServerUpdates() {
        try {
            while (true) {
                Object serverMessage = in.readObject();
                if (serverMessage instanceof GameStateUpdate) {
                    GameStateUpdate update = (GameStateUpdate) serverMessage;
                    
                    if(animationTimer.isRunning()) {
                        pendingUpdate = update;
                        continue;
                    }
                    
                    if ((prevGameState == null || prevGameState == Blackjack.GameState.GAME_OVER || prevGameState == Blackjack.GameState.WAITING_FOR_PLAYERS) 
                        && update.getGameState() == Blackjack.GameState.PLAYER_TURN) {
                        SoundManager.getInstance().playSoundEffect(INITIAL_DEAL_SOUND);
                    }

                    List<AnimatingCard> newAnimations = new ArrayList<>();
                    
                    // Cập nhật animation cho player cards từ vị trí deck mới
                    for (int i = 0; i < update.getPlayersHands().size(); i++) {
                        int oldSize = (playersHands.size() > i) ? playersHands.get(i).size() : 0;
                        int newSize = update.getPlayersHands().get(i).size();
                        if (newSize > oldSize) {
                            for (int j = oldSize; j < newSize; j++) {
                                Card newCard = update.getPlayersHands().get(i).get(j);
                                int destX = 150 + i * 600 + j * 50; // Điều chỉnh để phù hợp với layout
                                int destY = 400; // Player area Y
                                newAnimations.add(new AnimatingCard(newCard, DECK_X, DECK_Y, destX, destY));
                            }
                        }
                    }
                    
                    // Cập nhật animation cho dealer cards từ vị trí deck mới
                    int oldDealerSize = dealerHand.size();
                    int newDealerSize = update.getDealerHand().size();
                    if (newDealerSize > oldDealerSize) {
                        for (int i = oldDealerSize; i < newDealerSize; i++) {
                             Card newCard = update.getDealerHand().get(i);
                             if(newCard.value.equals("BACK")) continue;
                             
                             int destX = 520 + i * 80; // Dealer cards position
                             int destY = 100; // Dealer area Y
                             newAnimations.add(new AnimatingCard(newCard, DECK_X, DECK_Y, destX, destY));
                        }
                    }
                    
                    if (!newAnimations.isEmpty()) {
                        this.pendingUpdate = update;
                        this.animatingCards.addAll(newAnimations);
                        animationTimer.start();
                    } else {
                        processStateUpdate(update);
                    }
                } else if (serverMessage instanceof String) {
                    String command = (String) serverMessage;
                    SwingUtilities.invokeLater(() -> handleServerCommand(command));
                }
            }
        } catch (Exception e) {
            if (!frame.isDisplayable()) return;
            statusLabel.setText("Mất kết nối với server.");
            setButtonStateForGameplay(false);
            disconnectButton.setEnabled(false);
        }
    }
    
    private void processStateUpdate(GameStateUpdate update) {
        boolean wasGameOver = (this.currentGameState == Blackjack.GameState.GAME_OVER);
        
        this.playersNames = update.getPlayersNames();
        this.playersHands = update.getPlayersHands();
        this.playersSums = update.getPlayersSums();
        this.playersResults = update.getPlayersResults();
        
        // SỬA: Cập nhật với null checks
        if (update.getPlayersMoneys() != null) {
            this.playersMoneys = new ArrayList<>(update.getPlayersMoneys());
        } else {
            // Khởi tạo danh sách trống nếu null
            this.playersMoneys = new ArrayList<>();
            for (int i = 0; i < (playersNames != null ? playersNames.size() : 2); i++) {
                this.playersMoneys.add(1000); // Default money
            }
        }
        
        if (update.getPlayersBets() != null) {
            this.playersBets = new ArrayList<>(update.getPlayersBets());
        } else {
            // Khởi tạo danh sách trống nếu null
            this.playersBets = new ArrayList<>();
            for (int i = 0; i < (playersNames != null ? playersNames.size() : 2); i++) {
                this.playersBets.add(0); // Default bet
            }
        }
        
        this.dealerHand = update.getDealerHand();
        this.dealerSum = update.getDealerSum();
        this.prevGameState = this.currentGameState;
        this.currentGameState = update.getGameState();
        this.currentPlayerTurn = update.getCurrentPlayerTurn();
        this.myPlayerId = update.getMyPlayerId();

        // Cập nhật UI labels với null checks
        if (myPlayerId != -1 && playersMoneys.size() > myPlayerId && playersBets.size() > myPlayerId) {
            moneyLabel.setText("Tiền: $" + playersMoneys.get(myPlayerId));
            betLabel.setText("Cược: $" + playersBets.get(myPlayerId));
        }

        // Phát âm thanh lật bài khi chuyển sang GAME_OVER
        if (!wasGameOver && this.currentGameState == Blackjack.GameState.GAME_OVER) {
            SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
            System.out.println("🎭 Tất cả bài đã được lật!");
        }

        SwingUtilities.invokeLater(() -> {
            updateUI();
            gamePanel.repaint();
        });
    }

    private void handleServerCommand(String command) {
        if (command.startsWith("BETTING_ERROR:")) {
            String errorMsg = command.substring(14);
            statusLabel.setText("Lỗi cược: " + errorMsg);
            setBettingButtonsEnabled(true); // Cho phép cược lại
            return;
        }
        
        // Xử lý các commands khác...
        switch(command) {
            case "OPPONENT_DISCONNECTED":
                statusLabel.setText("Đối thủ đã ngắt kết nối.");
                setButtonStateForGameplay(false);
                setBettingButtonsEnabled(false);
                disconnectButton.setEnabled(true);
                break;
            case "NEW_GAME_REQUESTED":
                statusLabel.setText("Đối thủ muốn chơi ván mới. Bạn có đồng ý?");
                acceptButton.setVisible(true);
                declineButton.setVisible(true);
                break;
            // ... other cases
        }
    }

    private void setButtonStateForGameplay(boolean enabled) {
        hitButton.setVisible(enabled);
        standButton.setVisible(enabled);
        hitButton.setEnabled(enabled);
        standButton.setEnabled(enabled);
    }
    
    private void updateUI() {
        setButtonStateForGameplay(false);
        setBettingButtonsEnabled(false);
        newGameButton.setVisible(false);
        acceptButton.setVisible(false);
        declineButton.setVisible(false);
        disconnectButton.setVisible(true);

        if (currentGameState == Blackjack.GameState.BETTING) {
            statusLabel.setText("Hãy đặt cược để bắt đầu!");
            setBettingButtonsEnabled(true);
            
            // Disable buttons nếu không đủ tiền
            if (myPlayerId != -1 && playersMoneys.size() > myPlayerId) {
                int myMoney = playersMoneys.get(myPlayerId);
                bet10Button.setEnabled(myMoney >= 10);
                bet20Button.setEnabled(myMoney >= 20);
                bet50Button.setEnabled(myMoney >= 50);
                bet100Button.setEnabled(myMoney >= 100);
            }
            
        } else if (currentGameState == Blackjack.GameState.PLAYER_TURN) {
            setButtonStateForGameplay(true);
            String currentTurnPlayerName = playersNames.get(currentPlayerTurn);
            if (currentPlayerTurn == myPlayerId) {
                statusLabel.setText("Đến lượt BẠN!");
                hitButton.setEnabled(true);
                standButton.setEnabled(true);
            } else {
                statusLabel.setText("Đang chờ lượt của " + currentTurnPlayerName + "...");
                hitButton.setEnabled(false);
                standButton.setEnabled(false);
            }
        } else if (currentGameState == Blackjack.GameState.DEALER_TURN) {
            statusLabel.setText("Lượt của Nhà cái...");
        } else if (currentGameState == Blackjack.GameState.GAME_OVER) {
            statusLabel.setText("Game kết thúc! Nhấn 'Ván mới' để chơi lại.");
            newGameButton.setVisible(true);
            newGameButton.setEnabled(true);
            
            // Phát âm thanh thắng/thua
            if (myPlayerId != -1 && playersResults != null && !playersResults.isEmpty() && playersResults.size() > myPlayerId) {
                String myResult = playersResults.get(myPlayerId);
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                        if (myResult.contains("Thắng")) {
                            SoundManager.getInstance().playSoundEffect(WIN_SOUND);
                        } else if (myResult.contains("Thua")) {
                            SoundManager.getInstance().playSoundEffect(LOSE_SOUND);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
        gamePanel.repaint();
    }
    
    private void placeBet(int amount) {
        SoundManager.getInstance().playSoundEffect(CLICK_SOUND);
        sendCommand("BET:" + amount);
        
        // Disable betting buttons sau khi đặt cược
        setBettingButtonsEnabled(false);
    }

    private void setBettingButtonsEnabled(boolean enabled) {
        bet10Button.setEnabled(enabled);
        bet20Button.setEnabled(enabled);
        bet50Button.setEnabled(enabled);
        bet100Button.setEnabled(enabled);
        bet10Button.setVisible(enabled);
        bet20Button.setVisible(enabled);
        bet50Button.setVisible(enabled);
        bet100Button.setVisible(enabled);
    }

    private boolean shouldShowPlayerCards(int playerId) {
        // Luôn hiển thị bài của chính mình
        if (playerId == myPlayerId) {
            return true;
        }
        
        // Chỉ hiển thị bài của người khác khi game kết thúc
        if (currentGameState == Blackjack.GameState.GAME_OVER) {
            return true;
        }
        
        // Ẩn bài trong trạng thái BETTING và các trạng thái khác
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BlackjackClient::new);
    }
}