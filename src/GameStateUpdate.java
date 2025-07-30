// GameStateUpdate.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameStateUpdate implements Serializable {
    private static final long serialVersionUID = 4L; 

    // Dữ liệu của nhiều người chơi
    public List<String> playersNames; 
    public List<ArrayList<Card>> playersHands;
    public List<Integer> playersSums;
    public List<String> playersResults;
    
    // THÊM: Dữ liệu cược tiền
    public List<Integer> playersMoneys; // Số tiền còn lại của mỗi người
    public List<Integer> playersBets;   // Số tiền cược của mỗi người trong ván này

    // Dữ liệu nhà cái
    public ArrayList<Card> dealerHand;
    public int dealerSum;

    // Trạng thái chung của game
    public Blackjack.GameState gameState;
    public int currentPlayerTurn;
    public int myPlayerId;

    // Constructor cập nhật
    public GameStateUpdate(List<String> playersNames, List<ArrayList<Card>> playersHands, 
                          List<Integer> playersSums, List<String> playersResults,
                          List<Integer> playersMoneys, List<Integer> playersBets,
                          ArrayList<Card> dealerHand, int dealerSum,
                          Blackjack.GameState gameState, int currentPlayerTurn, int myPlayerId) {
        this.playersNames = playersNames;
        this.playersHands = playersHands;
        this.playersSums = playersSums;
        this.playersResults = playersResults;
        this.playersMoneys = playersMoneys;
        this.playersBets = playersBets;
        this.dealerHand = dealerHand;
        this.dealerSum = dealerSum;
        this.gameState = gameState;
        this.currentPlayerTurn = currentPlayerTurn;
        this.myPlayerId = myPlayerId;
    }

    // --- GETTERS ---

    public List<String> getPlayersNames() { 
        return playersNames;
    }

    public List<ArrayList<Card>> getPlayersHands() {
        return playersHands;
    }

    public List<Integer> getPlayersSums() {
        return playersSums;
    }

    public List<String> getPlayersResults() {
        return playersResults;
    }

    public List<Integer> getPlayersMoneys() {
        return playersMoneys;
    }

    public List<Integer> getPlayersBets() {
        return playersBets;
    }

    public ArrayList<Card> getDealerHand() {
        return dealerHand;
    }

    public int getDealerSum() {
        return dealerSum;
    }

    public Blackjack.GameState getGameState() {
        return gameState;
    }

    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    public int getMyPlayerId() {
        return myPlayerId;
    }
}