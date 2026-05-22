package game.view;

import java.net.URL;

import game.engine.cards.Card;
import game.engine.cards.ShieldCard;
import game.engine.cards.EnergyStealCard;
import game.engine.cards.StartOverCard;
import game.engine.cards.ConfusionCard;
import game.engine.cards.SwapperCard;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class CardPopup {

    public static void show(Stage ownerStage, Card drawnCard, Runnable onCloseAction) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.WINDOW_MODAL);
        popupStage.initOwner(ownerStage);
        popupStage.initStyle(StageStyle.TRANSPARENT); 

        // ── 1. Floating Text Header ──
        Label headerLabel = new Label("YOU DREW A CARD!");
        headerLabel.setStyle(
            "-fx-text-fill: #e9b949; " +
            "-fx-font-size: 22px; " +
            "-fx-font-weight: bold; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.8), 6, 0, 0, 3);"
        );

        // ── 2. Identify Card Type via instanceof & Dynamic File Resolution ──
        ImageView cardImageView = new ImageView();
        String lookupName = "swap"; // Safe fallback

        if (drawnCard instanceof SwapperCard) {
            lookupName = "swap";
        } else if (drawnCard instanceof ShieldCard) {
            lookupName = "shield";
        } else if (drawnCard instanceof EnergyStealCard) {
            lookupName = "energysteal";
        } else if (drawnCard instanceof StartOverCard) {
            lookupName = "startover";
        } else if (drawnCard instanceof ConfusionCard) {
            lookupName = "confusion";
        }

        try {
            // Checks inside your project's src/main/resources/images/ directory
            String imagePath = "/assets/" + lookupName + ".png";
            Image cardImage = new Image(CardPopup.class.getResourceAsStream(imagePath));
            cardImageView.setImage(cardImage);
        } catch (Exception e) {
            System.out.println("⚠️ UI could not resolve graphic file asset for card class: " + lookupName);
        }

        // Configure size properties to render beautifully while maintaining native aspect ratio
        cardImageView.setFitWidth(520); 
        cardImageView.setPreserveRatio(true);
        cardImageView.setSmooth(true);

        // ── 3. The Playing Card Body Container (The Blue Box) ──
        VBox cardBody = new VBox(15); 
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setPadding(new Insets(25, 24, 25, 24));
        cardBody.setPrefWidth(340);
        cardBody.setPrefHeight(530); 
        
        cardBody.setStyle(
            "-fx-background-color: #0b1a30; " +                          
            "-fx-border-color: #e5d1b1, #e9b949; " +                     
            "-fx-border-width: 10px, 2px; " +                            
            "-fx-border-insets: 0, 8px; " +                              
            "-fx-border-radius: 16px, 10px; " +                          
            "-fx-background-radius: 24px; " +                            
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.65), 20, 0, 0, 8);" 
        );

        // ── 4. Internal Labels & Content ──
        Label nameLabel = new Label(drawnCard.getName().toUpperCase());
        nameLabel.setStyle("-fx-text-fill: #e5d1b1; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(260);

        StackPane frameDivider = new StackPane();
        frameDivider.setPrefHeight(2);
        frameDivider.setMaxWidth(140);
        frameDivider.setStyle("-fx-background-color: #e9b949;");

        Label descLabel = new Label(drawnCard.getDescription());
        descLabel.setStyle("-fx-text-fill: #a2b7d4; -fx-font-size: 13px; -fx-alignment: center; -fx-text-alignment: center; -fx-line-spacing: 4px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(250);
        VBox.setVgrow(descLabel, javafx.scene.layout.Priority.ALWAYS); 

        // ── 5. Action Close Button ──
        Button btnAction = new Button("EXECUTE CARD EFFECT");
        btnAction.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #e9b949, #b8861b); " + 
            "-fx-text-fill: #0b1a30; " +                                                                             
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 10 24; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #e5d1b1; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6;"
        );

        btnAction.setOnAction(e -> {
            if (popupStage.isShowing()) {
                popupStage.close();
            }
        });

        // Pack elements into the blue container box frame in perfect hierarchical sequence
        cardBody.getChildren().addAll(nameLabel, frameDivider, cardImageView, descLabel, btnAction);

        // ── 6. Main Transparent Stage Root Container ──
        VBox rootLayout = new VBox(20);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(25));
        rootLayout.setStyle("-fx-background-color: transparent;"); 
        rootLayout.getChildren().addAll(headerLabel, cardBody);

        Scene scene = new Scene(rootLayout);
        scene.setFill(Color.TRANSPARENT); 
        
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.centerOnScreen();

        // ── 7. ANIMATION TRANSITION TIMELINE ──
        double startFromX = 600;  
        double startFromY = 400;  
        Duration animationDuration = Duration.millis(750);
        Interpolator dragInterpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        TranslateTransition flyIn = new TranslateTransition(animationDuration, rootLayout);
        flyIn.setFromX(startFromX);
        flyIn.setFromY(startFromY);
        flyIn.setToX(0);
        flyIn.setToY(0);
        flyIn.setInterpolator(dragInterpolator); 

        ScaleTransition grow = new ScaleTransition(animationDuration, rootLayout);
        grow.setFromX(0.15); 
        grow.setFromY(0.15);
        grow.setToX(1.0);
        grow.setToY(1.0);
        grow.setInterpolator(dragInterpolator);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootLayout);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ParallelTransition dragAndDropAnimation = new ParallelTransition(flyIn, grow, fadeIn);
        
        try {
            URL soundUrl = CardPopup.class.getResource("/assets/Deepwoken Talent Card Flip Sound Effect - ukhf.mp3");
            if (soundUrl != null) {
                AudioClip drawSound = new AudioClip(soundUrl.toExternalForm());
                drawSound.setVolume(0.7); 
                drawSound.play();         
            }
        } catch (Exception ex) {
            System.err.println("Could not process audio track playback: " + ex.getMessage());
        }
        
        popupStage.setOnHidden(e -> {
            if (onCloseAction != null) {
                onCloseAction.run();
            }
        });

        popupStage.show();
        dragAndDropAnimation.play();
    }
}