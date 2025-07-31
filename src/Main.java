import javax.swing.*;

/**
 * Class Main - Điểm khởi đầu của ứng dụng game Blackjack
 * Chịu trách nhiệm khởi tạo giao diện và chạy HomeScreen
 */
public class Main {
    /**
     * Phương thức main - Entry point của chương trình
     * Chức năng:
     * - Thiết lập Look and Feel cho giao diện Swing
     * - Khởi chạy HomeScreen trên Event Dispatch Thread
     * - Đảm bảo thread-safe cho Swing components
     * 
     * @param args Tham số dòng lệnh (không sử dụng)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Đặt Look and Feel của hệ thống
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Khởi chạy trang chủ
            new HomeScreen();
        });
    }
}