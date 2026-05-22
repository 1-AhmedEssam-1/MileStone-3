package game.controller;

import java.net.URL;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;

public class GameBackground{
	private static MediaPlayer backgroundMusic;
	private static String currentTrackAddress = "";

    public GameBackground(){//----- 
    	switchTrack("/assets/StartMenuTrack.mp3");
    	backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
        
    }

    public static void switchTrack(String address) {
        if (currentTrackAddress.equals(address)) return; // Already playing this track
        
        try {
            // Cut off any existing song immediately
            if (backgroundMusic != null) {
                backgroundMusic.stop();
                backgroundMusic.dispose(); // Wipes old media pointers from memory
            }

            URL resource = GameBackground.class.getResource(address);
            if (resource != null) {
                String musicFile = resource.toExternalForm();
                Media media = new Media(musicFile);
                backgroundMusic = new MediaPlayer(media);
                backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE); // Infinite loop
                backgroundMusic.setVolume(0.4);
                backgroundMusic.play();
                
                currentTrackAddress = address;
            } else {
                System.err.println("Could not find music track file at: " + address);
            }
        } catch (Exception e) {
            System.out.println("Music Switch Error: " + e.getMessage());
        }
    }
    //still at back with instructions popUp
//    private void setupVolumeControl() {//with options
//
//        StackPane gearContainer = new StackPane();
//
//        Text gearIcon = new Text("⚙");
//        gearIcon.getStyleClass().add("gear-icon");
//
//        gearContainer.getChildren().add(gearIcon);
//        gearContainer.setPadding(new Insets(20));
//
//        Slider volumeSlider = new Slider(0, 1, 0.5);
//        volumeSlider.setMaxWidth(150);
//        volumeSlider.setVisible(false);
//        volumeSlider.getStyleClass().add("volume-slider");
//
//        gearContainer.setOnMouseClicked(e ->
//                volumeSlider.setVisible(!volumeSlider.isVisible())
//        );
//
//        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
//            if (backgroundMusic != null)
//            	backgroundMusic.setVolume(newVal.doubleValue());
//        });
//
//        VBox volumeBox = new VBox(10, gearContainer, volumeSlider);
//        volumeBox.setAlignment(Pos.TOP_RIGHT);
//        volumeBox.setPickOnBounds(false);
//
//        StackPane.setAlignment(volumeBox, Pos.TOP_RIGHT);
//        //root.getChildren().add(volumeBox);
//    }
//    
    public void pauseMusic(){
    	backgroundMusic.pause();
    }
    
    public void playMusic(){
    	backgroundMusic.play();
    }
    
    public void volume(double volume){
    	backgroundMusic.setVolume(volume);
    }
}
