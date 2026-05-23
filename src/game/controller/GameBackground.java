package game.controller;

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

    public GameBackground(){//----- 
        openMusic("/assets/Monsters Inc theme full.mp3");
        backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
        
    }

    private void openMusic(String adress) {
        
    	try {

            String musicFile = getClass()
                    .getResource(adress)
                    .toExternalForm();

            Media media = new Media(musicFile);

            backgroundMusic = new MediaPlayer(media);
            
            backgroundMusic.setVolume(0.5);

            backgroundMusic.play();
            
        } catch (Exception e) {
            System.out.println("Music Error: " + e.getMessage());
        }
    }
    //still at back with instructions popUp
    private void setupVolumeControl() {//with options

        StackPane gearContainer = new StackPane();

        Text gearIcon = new Text("⚙");
        gearIcon.getStyleClass().add("gear-icon");

        gearContainer.getChildren().add(gearIcon);
        gearContainer.setPadding(new Insets(20));

        Slider volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setMaxWidth(150);
        volumeSlider.setVisible(false);
        volumeSlider.getStyleClass().add("volume-slider");

        gearContainer.setOnMouseClicked(e ->
                volumeSlider.setVisible(!volumeSlider.isVisible())
        );

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (backgroundMusic != null)
            	backgroundMusic.setVolume(newVal.doubleValue());
        });

        VBox volumeBox = new VBox(10, gearContainer, volumeSlider);
        volumeBox.setAlignment(Pos.TOP_RIGHT);
        volumeBox.setPickOnBounds(false);

        StackPane.setAlignment(volumeBox, Pos.TOP_RIGHT);
        //root.getChildren().add(volumeBox);
    }
    
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
