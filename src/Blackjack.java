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
    Card hiddenCard;
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
            if (round == 0) {
                // Lá đầu tiên của dealer bị ẩn
                hiddenCard = dealerCard;
            }
            dealerHand.add(dealerCard);
            dealerSum += dealerCard.getValue();
            if (dealerCard.isAce()) {
                dealerAceCount++;
            }
        }
        
        // Reduce aces nếu cần
        for (int i = 0; i < numberOfPlayers; i++) {
            playersSums.set(i, reducePlayerAce(playersSums.get(i), playersAceCounts.get(i)));
        }
        dealerSum = reduceDealerAce();
        
        currentGameState = GameState.PLAYER_TURN;
        System.out.println("Đã chia bài xong! Chuyển sang PLAYER_TURN");
    }

    public void playerHit() {
        if (currentGameState != GameState.PLAYER_TURN || currentPlayerIndex >= numberOfPlayers) {
            return;
        }

        Card card = deck.remove(deck.size() - 1);
        playersHands.get(currentPlayerIndex).add(card);
        
        int newSum = playersSums.get(currentPlayerIndex) + card.getValue();
        int newAceCount = playersAceCounts.get(currentPlayerIndex);
        if (card.isAce()) {
            newAceCount++;
        }
        
        newSum = reducePlayerAce(newSum, newAceCount);
        playersSums.set(currentPlayerIndex, newSum);
        playersAceCounts.set(currentPlayerIndex, newAceCount);

        if (newSum > 21) {
            // Player bị bust, chuyển sang người tiếp theo
            playerStand();
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
        // Lật lá ẩn của dealer
        System.out.println("Dealer bắt đầu chơi...");
        
        // Dealer rút bài cho đến khi >= 17
        while (dealerSum < 17) {
            Card card = deck.remove(deck.size() - 1);
            dealerHand.add(card);
            dealerSum += card.getValue();
            if (card.isAce()) {
                dealerAceCount++;
            }
            dealerSum = reduceDealerAce();
        }
        
        // Kết thúc game và tính kết quả
        currentGameState = GameState.GAME_OVER;
        determineFinalResults();
    }

    private void determineFinalResults() {
        for (int i = 0; i < numberOfPlayers; i++) {
            int playerSum = playersSums.get(i);
            int betAmount = playersBets.get(i);
            int currentMoney = playersMoneys.get(i);
            
            if (playerSum > 21) {
                // Player bust
                playersResults.set(i, "Thua (Bust)");
                // Tiền cược đã bị trừ khi đặt cược
            } else if (dealerSum > 21) {
                // Dealer bust, player thắng
                playersResults.set(i, "Thắng (Dealer Bust)");
                playersMoneys.set(i, currentMoney + betAmount * 2); // Hoàn tiền + thắng
            } else if (playerSum > dealerSum) {
                // Player có điểm cao hơn
                playersResults.set(i, "Thắng");
                playersMoneys.set(i, currentMoney + betAmount * 2);
            } else if (playerSum == dealerSum) {
                // Hòa
                playersResults.set(i, "Hòa");
                playersMoneys.set(i, currentMoney + betAmount); // Hoàn tiền cược
            } else {
                // Player thua
                playersResults.set(i, "Thua");
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

    public int reducePlayerAce(int playerSum, int playerAceCount) {
        while (playerSum > 21 && playerAceCount > 0) {
            playerSum -= 10;
            playerAceCount--;
        }
        return playerSum;
    }

    public int reduceDealerAce() {
        while (dealerSum > 21 && dealerAceCount > 0) {
            dealerSum -= 10;
            dealerAceCount--;
        }
        return dealerSum;
    }

    // Getters
    public List<String> getPlayersResults() { return playersResults; }
    public List<ArrayList<Card>> getPlayersHands() { return playersHands; }
    public List<Integer> getPlayersSums() { return playersSums; }
    public ArrayList<Card> getDealerHand() { return dealerHand; }
    public int getDealerSum() { return dealerSum; }
    public GameState getCurrentGameState() { return currentGameState; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
}