import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;

public class HomeScreen extends JFrame {
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    private static final Color BG_COLOR = new Color(0, 100, 0);
    private static final Color FELT_COLOR = new Color(34, 139, 34);
    
    private JSlider volumeSlider;
    private JLabel volumeLabel;
    private JButton startGameButton;
    private JButton exitButton;
    private JTextField playerNameField;
    private JTextField serverIPField;
    private JTextField serverPortField;
    private String playerName;

    public HomeScreen() {
        initializeHomeScreen();
    }

    private void initializeHomeScreen() {
        setTitle("♠ BLACKJACK - TRANG CHỦ ♥");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Tạo main panel với custom painting
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBackground(g);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);

        // Tạo center panel cho các components chính
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Tạo bottom panel cho volume control
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private void drawBackground(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gradient background
        GradientPaint gradient = new GradientPaint(
            0, 0, BG_COLOR,
            FRAME_WIDTH, FRAME_HEIGHT, FELT_COLOR
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);

        // Vẽ viền gỗ
        g2d.setColor(new Color(139, 69, 19));
        g2d.setStroke(new BasicStroke(8));
        g2d.drawRoundRect(10, 10, FRAME_WIDTH - 20, FRAME_HEIGHT - 20, 20, 20);

        // Vẽ oval trang trí
        g2d.setColor(new Color(0, 80, 0, 100));
        g2d.fillOval(50, 50, FRAME_WIDTH - 100, FRAME_HEIGHT - 100);

        // Vẽ các icon thẻ bài trang trí
        drawDecorativeCards(g2d);
    }

    private void drawDecorativeCards(Graphics2D g2d) {
        // Vẽ một số thẻ bài trang trí ở góc
        Font cardFont = new Font("Serif", Font.BOLD, 40);
        g2d.setFont(cardFont);
        
        // Góc trái trên
        g2d.setColor(Color.RED);
        g2d.drawString("♥", 100, 100);
        g2d.setColor(Color.BLACK);
        g2d.drawString("♠", 130, 100);
        
        // Góc phải trên
        g2d.setColor(Color.RED);
        g2d.drawString("♦", FRAME_WIDTH - 160, 100);
        g2d.setColor(Color.BLACK);
        g2d.drawString("♣", FRAME_WIDTH - 130, 100);
        
        // Góc trái dưới
        g2d.setColor(Color.BLACK);
        g2d.drawString("♠", 100, FRAME_HEIGHT - 100);
        g2d.setColor(Color.RED);
        g2d.drawString("♥", 130, FRAME_HEIGHT - 100);
        
        // Góc phải dưới
        g2d.setColor(Color.BLACK);
        g2d.drawString("♣", FRAME_WIDTH - 160, FRAME_HEIGHT - 100);
        g2d.setColor(Color.RED);
        g2d.drawString("♦", FRAME_WIDTH - 130, FRAME_HEIGHT - 100);
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // Title
        JLabel titleLabel = new JLabel("♠ BLACKJACK ♥");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 48));
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Chào mừng đến với Casino!");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 20));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Spacing
        centerPanel.add(Box.createVerticalStrut(40));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(40));

        // Player name input
        JLabel nameLabel = new JLabel("Nhập tên của bạn:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        playerNameField = new JTextField(15);
        playerNameField.setFont(new Font("Arial", Font.PLAIN, 16));
        playerNameField.setMaximumSize(new Dimension(300, 35));
        playerNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerNameField.setHorizontalAlignment(JTextField.CENTER);

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(playerNameField);
        centerPanel.add(Box.createVerticalStrut(25));

        // Server connection section
        JLabel connectionLabel = new JLabel("══ THÔNG TIN KẾT NỐI ══");
        connectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        connectionLabel.setForeground(Color.YELLOW);
        connectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create a sub-panel for connection info
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.Y_AXIS));
        connectionPanel.setOpaque(false);
        connectionPanel.setMaximumSize(new Dimension(400, 120));

        // Server IP input
        JLabel ipLabel = new JLabel("Địa chỉ IP Server:");
        ipLabel.setFont(new Font("Arial", Font.BOLD, 12));
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        serverIPField = new JTextField("localhost");
        serverIPField.setFont(new Font("Arial", Font.PLAIN, 12));
        serverIPField.setMaximumSize(new Dimension(200, 28));
        serverIPField.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverIPField.setHorizontalAlignment(JTextField.CENTER);
        serverIPField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));

        // Server Port input
        JLabel portLabel = new JLabel("Cổng Server:");
        portLabel.setFont(new Font("Arial", Font.BOLD, 12));
        portLabel.setForeground(Color.WHITE);
        portLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        serverPortField = new JTextField("12345");
        serverPortField.setFont(new Font("Arial", Font.PLAIN, 12));
        serverPortField.setMaximumSize(new Dimension(120, 28));
        serverPortField.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverPortField.setHorizontalAlignment(JTextField.CENTER);
        serverPortField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));

        // Add to connection panel
        connectionPanel.add(ipLabel);
        connectionPanel.add(Box.createVerticalStrut(5));
        connectionPanel.add(serverIPField);
        connectionPanel.add(Box.createVerticalStrut(10));
        connectionPanel.add(portLabel);
        connectionPanel.add(Box.createVerticalStrut(5));
        connectionPanel.add(serverPortField);

        centerPanel.add(connectionLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(connectionPanel);
        centerPanel.add(Box.createVerticalStrut(25));

        // Buttons
        startGameButton = createStyledButton("🎮 BẮT ĐẦU CHƠI", new Color(50, 205, 50), Color.WHITE);
        exitButton = createStyledButton("🚪 THOÁT", new Color(220, 20, 60), Color.WHITE);

        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        centerPanel.add(startGameButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(exitButton);
        centerPanel.add(Box.createVerticalStrut(20)); // Thêm space cuối

        return centerPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        bottomPanel.setBackground(new Color(139, 69, 19));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Volume control
        volumeLabel = new JLabel("🔊 Âm lượng:");
        volumeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        volumeLabel.setForeground(Color.WHITE);

        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setBackground(new Color(139, 69, 19));
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.setPreferredSize(new Dimension(200, 40));
        volumeSlider.addChangeListener(e -> {
            float volume = volumeSlider.getValue() / 100.0f;
            SoundManager.getInstance().setVolume(volume);
            updateVolumeLabel();
        });

        // Hiển thị % âm lượng
        updateVolumeLabel();

        bottomPanel.add(volumeLabel);
        bottomPanel.add(volumeSlider);

        return bottomPanel;
    }

    private void updateVolumeLabel() {
        volumeLabel.setText("🔊 Âm lượng: " + volumeSlider.getValue() + "%");
    }

    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setPreferredSize(new Dimension(250, 50));
        button.setMaximumSize(new Dimension(250, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void startGame() {
        playerName = playerNameField.getText().trim();
        String serverIP = serverIPField.getText().trim();
        String serverPort = serverPortField.getText().trim();
        
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập tên của bạn!", 
                "Thông báo", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (serverIP.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập địa chỉ IP server!", 
                "Thông báo", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (serverPort.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập cổng server!", 
                "Thông báo", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiểm tra tính hợp lệ của cổng
        int port;
        try {
            port = Integer.parseInt(serverPort);
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this, 
                    "Cổng phải nằm trong khoảng 1024-65535!", 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Cổng phải là một số nguyên hợp lệ!", 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Phát âm thanh click
        SoundManager.getInstance().playSoundEffect("resource/sounds/click.wav");

        // Ẩn trang chủ và mở game client
        this.setVisible(false);
        
        // Tạo và hiển thị game client với IP và port đã nhập
        SwingUtilities.invokeLater(() -> {
            new BlackjackClient(playerName, volumeSlider.getValue(), serverIP, port);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new HomeScreen();
        });
    }
}