import javax.swing.*;

public class Main {
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