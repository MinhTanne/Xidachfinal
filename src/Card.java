// Card.java
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class Card - Đại diện cho một lá bài trong game Blackjack
 * Chứa thông tin về giá trị và chất của lá bài
 */
public class Card implements Serializable {
    private static final long serialVersionUID = 1L; // Cần thiết cho Serializable
    public String value;
    public String type;

    /**
     * Constructor - Tạo một lá bài mới
     * @param value Giá trị của lá bài (2-10, J, Q, K, A)
     * @param type Chất của lá bài (H, D, C, S)
     */
    public Card(String value, String type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Override phương thức toString
     * @return Chuỗi biểu diễn lá bài dạng "giá_trị-chất" (ví dụ: "A-H")
     */
    @Override
    public String toString() {
        return value + "-" + type;
    }

    /**
     * Lấy giá trị điểm của lá bài theo luật Blackjack
     * @return Điểm số của lá bài:
     *         - A = 11 điểm (sẽ được điều chỉnh thành 1 nếu cần)
     *         - J, Q, K = 10 điểm
     *         - Các số khác = chính giá trị của nó
     */
    public int getValue() {
        if ("AJQK".contains(value)) {
            if (value.equals("A")) {
                return 11;
            }
            return 10;
        }
        return Integer.parseInt(value);
    }

    /**
     * Kiểm tra xem lá bài có phải là Ace không
     * @return true nếu là Ace, false nếu không
     */
    public boolean isAce() {
        return value.equals("A");
    }

    /**
     * Lấy đường dẫn đến file hình ảnh của lá bài
     * @return Đường dẫn đến file PNG của lá bài
     */
    public String getImagePath() {
        return "resources/cards/" + toString() + ".png";
    }
    
    /**
     * Lấy URL của file hình ảnh lá bài
     * @return URL object trỏ đến file hình ảnh
     * @throws MalformedURLException nếu đường dẫn không hợp lệ
     */
    public URL getImageURL() throws MalformedURLException {
        File cardFile = new File(getImagePath());
        return cardFile.toURI().toURL();
    }
}