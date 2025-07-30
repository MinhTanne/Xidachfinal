// SoundManager.java
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class SoundManager {

    private static SoundManager instance;
    private Clip musicClip;
    private float volume = 0.75f; // Âm lượng mặc định 75%

    // Sử dụng Singleton Pattern để chỉ có một đối tượng quản lý âm thanh
    private SoundManager() {}

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // Phát nhạc nền
    public void playBackgroundMusic(String filePath) {
        // Dừng nhạc cũ nếu đang phát
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
        }
        
        try {
            File soundFile = new File(filePath);
            if (!soundFile.exists()) {
                System.err.println("File âm thanh không tồn tại: " + filePath);
                return;
            }
            
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            musicClip = AudioSystem.getClip();
            musicClip.open(audioIn);
            setClipVolume(musicClip); // Đặt âm lượng ban đầu
            musicClip.loop(Clip.LOOP_CONTINUOUSLY); // Lặp lại vô hạn
            System.out.println("Phát nhạc nền thành công: " + filePath);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Không thể phát nhạc nền: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Thêm method để phát nhạc nền cho trang chủ
    public void playBackgroundMusicLoop(String filePath) {
        playBackgroundMusic(filePath);
    }

    // Dừng nhạc nền
    public void stopBackgroundMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
        }
    }

    // Phát hiệu ứng âm thanh (mỗi lần phát là một clip mới để có thể chồng lên nhau)
    public void playSoundEffect(String filePath) {
        new Thread(() -> {
            try {
                File soundFile = new File(filePath);
                if (!soundFile.exists()) {
                    System.err.println("File âm thanh không tồn tại: " + filePath);
                    return;
                }
                
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip effectClip = AudioSystem.getClip();
                effectClip.open(audioIn);
                setClipVolume(effectClip); // Đặt âm lượng
                effectClip.start();
                System.out.println("Phát hiệu ứng âm thanh: " + filePath);
                
                // Listener để giải phóng tài nguyên khi clip phát xong
                effectClip.addLineListener(event -> {
                    if (LineEvent.Type.STOP.equals(event.getType())) {
                        effectClip.close();
                        try {
                            audioIn.close();
                        } catch (IOException e) {
                            System.err.println("Lỗi khi đóng AudioInputStream: " + e.getMessage());
                        }
                    }
                });
                
                // Đợi cho đến khi clip phát xong
                while (effectClip.isRunning()) {
                    Thread.sleep(10);
                }
                
            } catch (Exception e) {
                System.err.println("Không thể phát hiệu ứng âm thanh: " + filePath + " - " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Đặt âm lượng (từ 0.0f đến 1.0f)
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume)); // Đảm bảo âm lượng trong khoảng 0-1
        System.out.println("Đặt âm lượng: " + (this.volume * 100) + "%");
        
        // Cập nhật âm lượng của nhạc nền đang phát
        if (musicClip != null) {
            setClipVolume(musicClip);
        }
    }

    // Hàm nội bộ để đặt gain (âm lượng) cho một Clip
    private void setClipVolume(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                
                // In thông tin về range của gainControl
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                System.out.println("Volume range: " + min + " to " + max + " dB");
                
                // Chuyển đổi âm lượng tuyến tính (0-1) sang thang đo decibel (dB)
                if (volume == 0.0f) {
                    gainControl.setValue(min);
                } else {
                    // Cải thiện công thức chuyển đổi
                    float dB = (float) (Math.log10(volume) * 20.0);
                    // Đảm bảo giá trị trong phạm vi hợp lệ
                    dB = Math.max(min, Math.min(max, dB));
                    gainControl.setValue(dB);
                    System.out.println("Đặt gain: " + dB + " dB cho volume " + (volume * 100) + "%");
                }
            } else {
                System.out.println("MASTER_GAIN không được hỗ trợ, thử VOLUME control");
                if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                    FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                    volumeControl.setValue(volume * volumeControl.getMaximum());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đặt âm lượng: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Thêm method để kiểm tra trạng thái
    public void checkAudioSystem() {
        System.out.println("=== THÔNG TIN HỆ THỐNG ÂM THANH ===");
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            System.out.println("Số lượng mixer có sẵn: " + mixers.length);
            for (Mixer.Info mixerInfo : mixers) {
                System.out.println("- " + mixerInfo.getName());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi kiểm tra hệ thống âm thanh: " + e.getMessage());
        }
    }

    public float getVolume() {
        return this.volume;
    }
}