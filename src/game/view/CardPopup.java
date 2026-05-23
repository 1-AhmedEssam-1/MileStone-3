//package game.view;
//
//import game.engine.cards.Card;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.layout.VBox;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//
//public class CardPopup {
//
//    public static void show(Stage ownerStage, Card drawnCard, Runnable onCloseAction) {
//        Stage popupStage = new Stage();
//        popupStage.initModality(Modality.WINDOW_MODAL);
//        popupStage.initOwner(ownerStage);
//        popupStage.initStyle(StageStyle.UTILITY); // Ensures the window frame renders reliably across OS environments
//        popupStage.setTitle("🃏 Card Drawn!");
//
//        VBox contentBox = new VBox(20);
//        contentBox.setAlignment(Pos.CENTER);
//        contentBox.setPadding(new Insets(30));
//
//        // Match the deep dark space theme layout styles of your board dashboard panel
//        contentBox.setStyle("-fx-background-color: #0d1117; -fx-border-color: #d97706; -fx-border-width: 2px;");
//
//        Label headerLabel = new Label("YOU DREW A CARD!");
//        headerLabel.setStyle("-fx-text-fill: #d97706; -fx-font-size: 20px; -fx-font-weight: bold;");
//
//        // Display the specific card data loaded from your milestone structures
//        Label nameLabel = new Label(drawnCard.getName().toUpperCase());
//        nameLabel.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 16px; -fx-font-weight: bold;");
//
//        Label descLabel = new Label(drawnCard.getDescription());
//        descLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px; -fx-alignment: center; -fx-text-alignment: center;");
//        descLabel.setWrapText(true);
//        descLabel.setMaxWidth(360);
//
//        Button btnAction = new Button("EXECUTE CARD EFFECT");
//        btnAction.setStyle("-fx-background-color: linear-gradient(to bottom, #1d6eea, #1558c0); -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6;");
//
//        btnAction.setOnAction(e -> {
//            // 1. Safe check to ensure we only close if it's currently open
//            if (popupStage.isShowing()) {
//                popupStage.close();
//            }
//            
//            // 2. Defer the heavy game logic/callback until the loop finishes tearing down
//            if (onCloseAction != null) {
//                javafx.application.Platform.runLater(() -> onCloseAction.run());
//            }
//        });
//        contentBox.getChildren().addAll(headerLabel, nameLabel, descLabel, btnAction);
//
//        Scene scene = new Scene(contentBox, 420, 280);
//        popupStage.setScene(scene);
//        popupStage.setResizable(false);
//        popupStage.centerOnScreen();
//
//        // This blocks game interaction in the background until the player clicks the button
//        popupStage.showAndWait(); 
//    }
//}








//////////////////////////
package game.view;

import java.net.URL;

import game.engine.cards.Card;
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

        // ── 2. The Playing Card Body Container ──
        VBox cardBody = new VBox(20);
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setPadding(new Insets(30, 24, 30, 24));
        cardBody.setPrefWidth(320);
        cardBody.setPrefHeight(460);
        
        cardBody.setStyle(
            "-fx-background-color: #0b1a30; " +                          
            "-fx-border-color: #e5d1b1, #e9b949; " +                     
            "-fx-border-width: 10px, 2px; " +                            
            "-fx-border-insets: 0, 8px; " +                              
            "-fx-border-radius: 16px, 10px; " +                          
            "-fx-background-radius: 24px; " +                            
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.65), 20, 0, 0, 8);" 
        );

        // ── 3. Internal Content ──
        Label nameLabel = new Label(drawnCard.getName().toUpperCase());
        nameLabel.setStyle("-fx-text-fill: #e5d1b1; -fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(260);

        StackPane frameDivider = new StackPane();
        frameDivider.setPrefHeight(2);
        frameDivider.setMaxWidth(140);
        frameDivider.setStyle("-fx-background-color: #e9b949;");

        Label descLabel = new Label(drawnCard.getDescription());
        descLabel.setStyle("-fx-text-fill: #a2b7d4; -fx-font-size: 14px; -fx-alignment: center; -fx-text-alignment: center; -fx-line-spacing: 5px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(250);
        VBox.setVgrow(descLabel, javafx.scene.layout.Priority.ALWAYS); 

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

        cardBody.getChildren().addAll(nameLabel, frameDivider, descLabel, btnAction);

        // ── 4. Main Scene Root Container ──
        VBox rootLayout = new VBox(20);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(25));
        rootLayout.setStyle("-fx-background-color: transparent;"); 
        rootLayout.getChildren().addAll(headerLabel, cardBody);

        Scene scene = new Scene(rootLayout, 420, 580);
        scene.setFill(Color.TRANSPARENT); 
        
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.centerOnScreen();

        // ── 🆕 5. PREMIUM DRAG & SLIDE ANIMATION TIMELINE ──
        
        // Target Coordinates mapping your right deck asset position
        double startFromX = 600;  
        double startFromY = 400;  

        // Duration of the drag movement (Increased slightly for weight and smoothness)
        Duration animationDuration = Duration.millis(750);

        // 🆕 Custom Interpolator Spline: Cubic Bézier Curve (0.25, 0.1, 0.25, 1.0)
        // This makes the card start with weight, accelerate fast, and glides smoothly into focus.
        Interpolator dragInterpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        // 1. Path Slide Translation
        TranslateTransition flyIn = new TranslateTransition(animationDuration, rootLayout);
        flyIn.setFromX(startFromX);
        flyIn.setFromY(startFromY);
        flyIn.setToX(0);
        flyIn.setToY(0);
        flyIn.setInterpolator(dragInterpolator); 

        // 2. Growth Scaling
        ScaleTransition grow = new ScaleTransition(animationDuration, rootLayout);
        grow.setFromX(0.15); // Started at 0.15 so the card width feels present at the origin point
        grow.setFromY(0.15);
        grow.setToX(1.0);
        grow.setToY(1.0);
        grow.setInterpolator(dragInterpolator);

        // 3. 🆕 Smooth Opacity Fade-In (Hides the initial pixel spawn pop)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootLayout);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Combine all timelines to fire together seamlessly
        ParallelTransition dragAndDropAnimation = new ParallelTransition(flyIn, grow, fadeIn);
        
        
        try {
            // Looks inside src/main/resources/assets/ or your project's bin assets folder
            URL soundUrl = CardPopup.class.getResource("/assets/Deepwoken Talent Card Flip Sound Effect - ukhf.mp3");
            if (soundUrl != null) {
                AudioClip drawSound = new AudioClip(soundUrl.toExternalForm());
                drawSound.setVolume(0.7); // Set comfortable volume scaling (0.0 to 1.0)
                drawSound.play();         // Fires instantly on draw!
            } else {
                System.out.println("⚠️ Sound file 'card_draw.mp3' not found in assets folder.");
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