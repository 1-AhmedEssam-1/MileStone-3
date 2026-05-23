package game.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.net.URL;
import java.util.Random;

/**
 * A ultra-optimized, self-contained custom UI component handling all dice rendering,
 * asset loading, lag-free audio playbacks, and rolling transitions.
 */
public class DiceView extends StackPane {

    private final ImageView diceImageView;
    private final Random random = new Random();
    
    // 🆕 THE ANTI-LAG FIX: Keep a single cached player instance in memory so we never recreate it mid-turn
    private MediaPlayer cachedAudioPlayer;

    public DiceView() {
        // Enforce structural boundaries
        setPrefSize(52, 52);
        getStyleClass().add("dice-face");

        // Set up the graphic view frame
        diceImageView = new ImageView();
        diceImageView.setFitWidth(75);
        diceImageView.setFitHeight(75);
        diceImageView.setPreserveRatio(true);  
        diceImageView.setSmooth(true);         

        // Start out showing the blank/unknown initial state
        setDiceImage("dice_1.png");
        getChildren().add(diceImageView);
        
        // 🆕 Warm up and cache the audio player immediately on startup so there is zero delay on click
        preloadDiceAudio();
    }

    /** Preloads the media asset into memory ahead of time to eliminate button-press latency */
    private void preloadDiceAudio() {
        try {
            URL soundURL = getClass().getResource("/assets/dice_roll.mp3");
            if (soundURL != null) {
                Media sound = new Media(soundURL.toExternalForm());
                cachedAudioPlayer = new MediaPlayer(sound);
                cachedAudioPlayer.setVolume(0.7); // Balanced atmosphere volume
            }
        } catch (Exception e) {
            System.out.println("Dice Audio Preload skipped: " + e.getMessage());
        }
    }

    /** Sets the static state image directly without rolling (used during initialization or resets) */
    public void setDiceValue(int value) {
        String filename = (value >= 1 && value <= 6) ? "dice_" + value + ".png" : "dice_blank.png";
        setDiceImage(filename);
    }

    private void setDiceImage(String filename) {
        try {
            URL url = getClass().getResource("/assets/" + filename);
            if (url != null) {
                diceImageView.setImage(new Image(url.toExternalForm()));
            }
        } catch (Exception e) {
            System.out.println("Failed to load dice frame asset: " + filename);
        }
    }

    /** Plays rolling audio and rapidly cycles random image frames before locking onto trueRollValue */
    public void playRollAnimation(int trueRollValue, Runnable onFinished) {
        // ─── 🆕 LAG-FREE AUDIO TRIGGERS ───
        if (cachedAudioPlayer != null) {
            // Cut off any previous clip instantly and rewind back to the beginning
            cachedAudioPlayer.stop(); 
            // Play immediately without loading overhead latency
            cachedAudioPlayer.play(); 
        }

        // ─── TIMELINE ANIMATION ───
        Timeline rollTimeline = new Timeline();
        int totalFlips = 25; 

        for (int i = 0; i < totalFlips; i++) {
            KeyFrame frame = new KeyFrame(
                Duration.millis(i * 60),
                event -> {
                    int mockFaceValue = random.nextInt(6) + 1;
                    setDiceImage("dice_" + mockFaceValue + ".png");
                }
            );
            rollTimeline.getKeyFrames().add(frame);
        }

        KeyFrame finalFrame = new KeyFrame(
            Duration.millis(totalFlips * 60),
            event -> {
                setDiceImage("dice_" + trueRollValue + ".png");
                if (onFinished != null) {
                    onFinished.run();
                }
            }
        );
        rollTimeline.getKeyFrames().add(finalFrame);
        rollTimeline.play();
    }
}