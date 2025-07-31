// Blackjack.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Blackjack implements Serializable {
    private static final long serialVersionUID = 2L;

    ArrayList<Card> deck;
    Random random = new Random();

    // Dealer
    ArrayList<Card> dealerHand;
    int dealerSum;
    int dealerAceCount;

    // Quản lý nhiều người chơi
    private final int numberOfPlayers;
    List<ArrayList<Card>> playersHands;
    List<Integer> playersSums;
    List<Integer> playersAceCounts;
    List<String> playersResults;
    
    // THÊM: Hệ thống cược tiền
    List<Integer> playersMoneys;   // Số tiền của mỗi người
    List<Integer> playersBets;     // Số tiền cược trong ván này
    private static final int STARTING_MONEY = 1000; // Tiền khởi tạo
    
    private int currentPlayerIndex;

    public enum GameState {
        WAITING_FOR_PLAYERS,
        BETTING,           // THÊM: Trạng thái đặt cược
        DEALING,
        PLAYER_TURN,
        DEALER_TURN,
        GAME_OVER
    }
    private GameState currentGameState;

    public Blackjack(int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
        this.playersHands = new ArrayList<>();
        this.playersSums = new ArrayList<>();
        this.playersAceCounts = new ArrayList<>();
        this.playersResults = new ArrayList<>();
        this.playersMoneys = new ArrayList<>();
        this.playersBets = new ArrayList<>();
        
        // Khởi tạo dữ liệu cho số người chơi
        for (int i = 0; i < numberOfPlayers; i++) {
            playersHands.add(new ArrayList<>());
            playersSums.add(0);
            playersAceCounts.add(0);
            playersResults.add("");
            playersMoneys.add(STARTING_MONEY); // Mỗi người bắt đầu với 1000$
            playersBets.add(0);
        }

        dealerHand = new ArrayList<>();
        dealerSum = 0;
        dealerAceCount = 0;
        currentPlayerIndex = 0;
        currentGameState = GameState.WAITING_FOR_PLAYERS;

        buildDeck();
        shuffleDeck();
    }

    public void startGame() {
        // Reset cho ván mới
        for (int i = 0; i < numberOfPlayers; i++) {
            playersHands.get(i).clear();
            playersSums.set(i, 0);
            playersAceCounts.set(i, 0);
            playersResults.set(i, "");
            playersBets.set(i, 0); // Reset cược
        }

        dealerHand.clear();
        dealerSum = 0;
        dealerAceCount = 0;
        currentPlayerIndex = 0;

        // Chuyển sang trạng thái đặt cược
        currentGameState = GameState.BETTING;
        System.out.println("Game bắt đầu! Trạng thái: BETTING");
    }

    // THÊM: Phương thức đặt cược
    public boolean placeBet(int playerId, int betAmount) {
        if (currentGameState != GameState.BETTING) {
            return false;
        }
        
        if (playerId < 0 || playerId >= numberOfPlayers) {
            return false;
        }
        
        int playerMoney = playersMoneys.get(playerId);
        if (betAmount <= 0 || betAmount > playerMoney) {
            return false;
        }
        
        // Đặt cược
        playersBets.set(playerId, betAmount);
        playersMoneys.set(playerId, playerMoney - betAmount);
        
        System.out.println("Player " + playerId + " đã cược " + betAmount + "$");
        
        // Kiểm tra xem tất cả người chơi đã đặt cược chưa
        boolean allPlayersBet = true;
        for (int i = 0; i < numberOfPlayers; i++) {
            if (playersBets.get(i) == 0) {
                allPlayersBet = false;
                break;
            }
        }
        
        if (allPlayersBet) {
            // Chuyển sang chia bài
            currentGameState = GameState.DEALING;
            dealInitialCards();
        }
        
        return true;
    }

    // THÊM: Chia bài ban đầu
    private void dealInitialCards() {
        // Chia 2 lá cho mỗi người chơi và dealer
        for (int round = 0; round < 2; round++) {
            // Chia cho người chơi trước
            for (int i = 0; i < numberOfPlayers; i++) {
                Card card = deck.remove(deck.size() - 1);
                playersHands.get(i).add(card);
                playersSums.set(i, playersSums.get(i) + card.getValue());
                if (card.isAce()) {
                    playersAceCounts.set(i, playersAceCounts.get(i) + 1);
                }
            }
            
            // Chia cho dealer
            Card dealerCard = deck.remove(deck.size() - 1);
            dealerHand.add(dealerCard);
            
            // Chỉ tính điểm cho lá thứ 2 của dealer (lá đầu ẩn)
            if (round == 1) {
                dealerSum += dealerCard.getValue();
                if (dealerCard.isAce()) {
                    dealerAceCount++;
                }
            }
        }
        
        // Tính điểm chính xác cho tất cả players từ đầu
        for (int i = 0; i < numberOfPlayers; i++) {
            int totalSum = 0;
            int aceCount = 0;
            
            // Tính tổng điểm thô từ tất cả lá bài
            for (Card c : playersHands.get(i)) {
                totalSum += c.getValue();
                if (c.isAce()) {
                    aceCount++;
                }
            }
            
            // Áp dụng logic A tối ưu và cập nhật
            int optimizedSum = calculateOptimalScore(totalSum, aceCount);
            playersSums.set(i, optimizedSum);
            playersAceCounts.set(i, aceCount);
        }
        
        // Kiểm tra Blackjack tự nhiên
        checkNaturalBlackjacks();
        
        // Nếu không có ai có Blackjack, bắt đầu lượt chơi
        if (currentGameState == GameState.DEALING) {
            currentGameState = GameState.PLAYER_TURN;
            currentPlayerIndex = 0;
            System.out.println("Đã chia bài xong! Chuyển sang PLAYER_TURN");
        }
    }
    
    // THÊM: Kiểm tra Blackjack tự nhiên (21 điểm với 2 lá đầu)
    private void checkNaturalBlackjacks() {
        boolean anyPlayerHasBlackjack = false;
        boolean dealerHasBlackjack = false;
        
        // Kiểm tra dealer có blackjack không (cần tính cả lá ẩn)
        Card dealerHiddenCard = dealerHand.get(0);
        Card visibleCard = dealerHand.get(1);
        int dealerTotal = dealerHiddenCard.getValue() + visibleCard.getValue();
        int dealerAces = (dealerHiddenCard.isAce() ? 1 : 0) + (visibleCard.isAce() ? 1 : 0);
        dealerTotal = calculateOptimalScore(dealerTotal, dealerAces);
        
        if (dealerTotal == 21) {
            dealerHasBlackjack = true;
        }
        
        // Kiểm tra players có blackjack không
        for (int i = 0; i < numberOfPlayers; i++) {
            if (playersSums.get(i) == 21) {
                anyPlayerHasBlackjack = true;
                playersResults.set(i, "Blackjack!");
            }
        }
        
        // Nếu có Blackjack, xử lý ngay
        if (anyPlayerHasBlackjack || dealerHasBlackjack) {
            // Lật lá ẩn của dealer
            dealerSum = dealerTotal;
            dealerAceCount = dealerAces;
            
            currentGameState = GameState.GAME_OVER;
            determineBlackjackResults(dealerHasBlackjack);
        }
    }
    
    // THÊM: Xử lý kết quả khi có Blackjack
    private void determineBlackjackResults(boolean dealerHasBlackjack) {
        for (int i = 0; i < numberOfPlayers; i++) {
            int playerSum = playersSums.get(i);
            int betAmount = playersBets.get(i);
            int currentMoney = playersMoneys.get(i);
            
            if (playerSum == 21 && dealerSum == 21) {
                // Cả hai đều có Blackjack = Hòa
                playersResults.set(i, "Hòa!");
                playersMoneys.set(i, currentMoney + betAmount); // Hoàn tiền
            } else if (playerSum == 21) {
                // Chỉ player có Blackjack = Thắng 3:2
                playersResults.set(i, "Blackjack!");
                playersMoneys.set(i, currentMoney + betAmount + (betAmount * 3 / 2)); // Hoàn tiền + thắng 1.5x
            } else if (dealerSum == 21) {
                // Chỉ dealer có Blackjack = Player thua
                playersResults.set(i, "Thua!");
                // Tiền đã bị trừ khi đặt cược
            } else {
                // Không ai có Blackjack, tiếp tục game bình thường
                playersResults.set(i, "");
            }
        }
    }

    public void playerHit() {
        if (currentGameState != GameState.PLAYER_TURN || currentPlayerIndex >= numberOfPlayers) {
            return;
        }

        Card card = deck.remove(deck.size() - 1);
        playersHands.get(currentPlayerIndex).add(card);
        
        // Tính lại toàn bộ điểm từ đầu để đảm bảo chính xác
        int totalSum = 0;
        int aceCount = 0;
        
        for (Card c : playersHands.get(currentPlayerIndex)) {
            totalSum += c.getValue();
            if (c.isAce()) {
                aceCount++;
            }
        }
        
        // Áp dụng logic tính A tối ưu
        int finalSum = calculateOptimalScore(totalSum, aceCount);
        
        // Cập nhật điểm và số A
        playersSums.set(currentPlayerIndex, finalSum);
        playersAceCounts.set(currentPlayerIndex, aceCount);

        // Kiểm tra nếu quá 21 điểm
        if (finalSum > 21) {
            // Player bị bust - thua ngay lập tức
            playersResults.set(currentPlayerIndex, "Thua!");
            System.out.println("Player " + currentPlayerIndex + " bị bust với " + finalSum + " điểm!");
            playerStand(); // Chuyển lượt cho người tiếp theo
        }
    }

    public void playerStand() {
        if (currentGameState != GameState.PLAYER_TURN) {
            return;
        }

        currentPlayerIndex++;
        if (currentPlayerIndex >= numberOfPlayers) {
            // Tất cả người chơi đã chơi xong
            currentGameState = GameState.DEALER_TURN;
            dealerPlay();
        }
    }
    
    private void dealerPlay() {
        // Lật lá ẩn của dealer và tính lại điểm từ đầu
        System.out.println("Dealer bắt đầu chơi...");
        
        // Tính lại điểm dealer từ tất cả lá bài hiện có
        dealerSum = 0;
        dealerAceCount = 0;
        for (Card card : dealerHand) {
            dealerSum += card.getValue();
            if (card.isAce()) {
                dealerAceCount++;
            }
        }
        dealerSum = calculateOptimalScore(dealerSum, dealerAceCount);
        
        System.out.println("Điểm dealer ban đầu: " + dealerSum);
        
        // Dealer rút bài cho đến khi >= 17
        while (dealerSum < 17) {
            Card card = deck.remove(deck.size() - 1);
            dealerHand.add(card);
            
            // Tính lại toàn bộ điểm từ đầu
            dealerSum = 0;
            dealerAceCount = 0;
            for (Card c : dealerHand) {
                dealerSum += c.getValue();
                if (c.isAce()) {
                    dealerAceCount++;
                }
            }
            dealerSum = calculateOptimalScore(dealerSum, dealerAceCount);
            
            System.out.println("Dealer rút " + card + ", tổng điểm: " + dealerSum);
        }
        
        System.out.println("Dealer kết thúc với " + dealerSum + " điểm");
        
        // Kết thúc game và tính kết quả
        currentGameState = GameState.GAME_OVER;
        determineFinalResults();
    }

    private void determineFinalResults() {
        for (int i = 0; i < numberOfPlayers; i++) {
            int playerSum = playersSums.get(i);
            int betAmount = playersBets.get(i);
            int currentMoney = playersMoneys.get(i);
            
            // Kiểm tra nếu player đã bị đánh dấu thua trước đó (quá 21)
            String currentResult = playersResults.get(i);
            if (currentResult != null && currentResult.contains("Thua")) {
                // Player đã thua từ trước (quá 21), kiểm tra dealer
                if (dealerSum > 21) {
                    // Cả hai đều quá 21 -> Hòa
                    playersResults.set(i, "Hòa!");
                    playersMoneys.set(i, currentMoney + betAmount); // Hoàn tiền cược
                }
                // Nếu dealer không quá 21, player vẫn thua (giữ nguyên kết quả)
                continue;
            }
            
            // Xử lý các trường hợp khác
            if (playerSum > 21) {
                // Player bust (trường hợp này không nên xảy ra vì đã xử lý ở trên)
                if (dealerSum > 21) {
                    playersResults.set(i, "Hòa!");
                    playersMoneys.set(i, currentMoney + betAmount);
                } else {
                    playersResults.set(i, "Thua!");
                }
            } else if (dealerSum > 21) {
                // Dealer bust, player thắng
                playersResults.set(i, "Thắng!");
                playersMoneys.set(i, currentMoney + betAmount * 2); // Hoàn tiền + thắng 1:1
            } else if (playerSum > dealerSum) {
                // Player có điểm cao hơn
                playersResults.set(i, "Thắng!");
                playersMoneys.set(i, currentMoney + betAmount * 2);
            } else if (playerSum == dealerSum) {
                // Hòa
                playersResults.set(i, "Hòa!");
                playersMoneys.set(i, currentMoney + betAmount); // Hoàn tiền cược
            } else {
                // Player thua
                playersResults.set(i, "Thua!");
                // Tiền cược đã bị trừ
            }
        }
    }
    
    // Getters cho hệ thống cược
    public List<Integer> getPlayersMoneys() { return playersMoneys; }
    public List<Integer> getPlayersBets() { return playersBets; }
    
    // Các phương thức khác giữ nguyên...
    public void buildDeck() {
        deck = new ArrayList<>();
        String[] values = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        String[] types = {"C", "D", "H", "S"};

        for (String type : types) {
            for (String value : values) {
                Card card = new Card(value, type);
                deck.add(card);
            }
        }
    }

    public void shuffleDeck() {
        Collections.shuffle(deck, random);
    }

    // Phương thức tính điểm tối ưu cho con A (11, 10, hoặc 1)
    private int calculateOptimalScore(int sum, int aceCount) {
        if (aceCount == 0) return sum;
        
        // Nếu điểm hiện tại <= 21, không cần điều chỉnh
        if (sum <= 21) return sum;
        
        // Thử giảm A từ 11 xuống 1 (giảm 10 điểm mỗi lần)
        int adjustedSum = sum;
        int acesToReduce = aceCount;
        
        while (adjustedSum > 21 && acesToReduce > 0) {
            adjustedSum -= 10; // Chuyển A từ 11 thành 1
            acesToReduce--;
        }
        
        return adjustedSum;
    }

    public int reducePlayerAce(int playerSum, int playerAceCount) {
        return calculateOptimalScore(playerSum, playerAceCount);
    }

    public int reduceDealerAce() {
        dealerSum = calculateOptimalScore(dealerSum, dealerAceCount);
        return dealerSum;
    }

    // Getters
    public List<String> getPlayersResults() { return playersResults; }
    public List<ArrayList<Card>> getPlayersHands() { return playersHands; }
    
    // Getter cho điểm đã được tính toán chính xác
    public List<Integer> getPlayersSums() { 
        // Đảm bảo tất cả điểm đều được tính chính xác với A
        List<Integer> correctSums = new ArrayList<>();
        for (int i = 0; i < numberOfPlayers; i++) {
            int rawSum = playersSums.get(i);
            int aceCount = playersAceCounts.get(i);
            correctSums.add(calculateOptimalScore(rawSum, aceCount));
        }
        return correctSums;
    }
    
    // Getter cho điểm của một player cụ thể
    public int getPlayerSum(int playerIndex) {
        if (playerIndex >= 0 && playerIndex < numberOfPlayers) {
            int rawSum = playersSums.get(playerIndex);
            int aceCount = playersAceCounts.get(playerIndex);
            return calculateOptimalScore(rawSum, aceCount);
        }
        return 0;
    }
    
    public ArrayList<Card> getDealerHand() { return dealerHand; }
    
    // Getter cho điểm dealer được tính chính xác
    public int getDealerSum() { 
        // Tính lại điểm dealer từ tất cả lá bài để đảm bảo chính xác
        if (dealerHand == null || dealerHand.isEmpty()) return 0;
        
        int sum = 0;
        int aceCount = 0;
        for (Card card : dealerHand) {
            sum += card.getValue();
            if (card.isAce()) {
                aceCount++;
            }
        }
        
        return calculateOptimalScore(sum, aceCount);
    }
    
    public GameState getCurrentGameState() { return currentGameState; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
}