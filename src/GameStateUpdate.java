// GameStateUpdate.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class GameStateUpdate - Đối tượng chứa toàn bộ trạng thái game để gửi giữa Client và Server
 * Sử dụng để đồng bộ hóa dữ liệu game giữa tất cả người chơi qua mạng
 */
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

    /**
     * Constructor - Tạo đối tượng cập nhật trạng thái game
     * @param playersNames Danh sách tên người chơi
     * @param playersHands Danh sách lá bài của từng người chơi
     * @param playersSums Danh sách điểm số của từng người chơi
     * @param playersResults Danh sách kết quả của từng người chơi
     * @param playersMoneys Danh sách số tiền còn lại của từng người chơi
     * @param playersBets Danh sách số tiền cược của từng người chơi
     * @param dealerHand Lá bài của dealer
     * @param dealerSum Điểm số của dealer
     * @param gameState Trạng thái hiện tại của game
     * @param currentPlayerTurn Lượt của người chơi nào
     * @param myPlayerId ID của người chơi hiện tại
     */
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

    /** @return Danh sách tên người chơi */
    public List<String> getPlayersNames() { 
        return playersNames;
    }

    /** @return Danh sách lá bài của từng người chơi */
    public List<ArrayList<Card>> getPlayersHands() {
        return playersHands;
    }

    /** @return Danh sách điểm số của từng người chơi */
    public List<Integer> getPlayersSums() {
        return playersSums;
    }

    /** @return Danh sách kết quả của từng người chơi */
    public List<String> getPlayersResults() {
        return playersResults;
    }

    /** @return Danh sách số tiền còn lại của từng người chơi */
    public List<Integer> getPlayersMoneys() {
        return playersMoneys;
    }

    /** @return Danh sách số tiền cược của từng người chơi */
    public List<Integer> getPlayersBets() {
        return playersBets;
    }

    /** @return Lá bài của dealer */
    public ArrayList<Card> getDealerHand() {
        return dealerHand;
    }

    /** @return Điểm số của dealer */
    public int getDealerSum() {
        return dealerSum;
    }

    /** @return Trạng thái hiện tại của game */
    public Blackjack.GameState getGameState() {
        return gameState;
    }

    /** @return Lượt của người chơi nào (index) */
    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    /** @return ID của người chơi hiện tại */
    public int getMyPlayerId() {
        return myPlayerId;
    }
}