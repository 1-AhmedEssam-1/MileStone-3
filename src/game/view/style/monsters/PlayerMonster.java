package game.view.style.monsters;

import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * PlayerMonster — visual token for a player on the board.
 *
 * Fixes vs previous version:
 *   - Badge label (P1/P2) is stored in a dedicated field so it survives
 *     openTheDoor() / standUp() sprite swaps.
 *   - openTheDoor() swaps only the MonsterGUI child, preserving the badge.
 *   - standUp() swaps only the MonsterGUI child, preserving the badge.
 *   - Added moveDirect() for straight-line transport animation (conveyor/sock).
 *   - centreOf() uses safe bounds calculation with fallback.
 */
public class PlayerMonster extends StackPane {
    private static final String[] list = {
        "James P. Sullivan", "Mike Wazowski", "Randall Boggs", "Celia Mae",
        "Roz", "Fungus", "Henry J. Waternoose", "Yeti"
    };

    private final MonsterGUI[] playerList;
    private MonsterGUI player;
    private final double cellSize;
    private static final double STEP_MS = 650;

    /** Badge label — persists across sprite swaps. */
    private Label badgeLabel;

    public PlayerMonster(String playerCharacter, double cellSize, String typeColor) {
        this.cellSize = cellSize;
        this.setAlignment(Pos.CENTER);

        // Preload both standard profile states (s_ = Standard, o_ = Action/Door Roar State)
        MonsterGUI sPlayer = new MonsterGUI("s_" + playerCharacter, cellSize + 3, typeColor);
        MonsterGUI oPlayer = new MonsterGUI("o_" + playerCharacter, cellSize + 3, typeColor);

        this.playerList = new MonsterGUI[]{sPlayer, oPlayer};
        this.player = playerList[0];

        // Add the primary standard layer active component
        this.getChildren().add(this.player);
        setPickOnBounds(false);
    }

    // ── Badge management ───────────────────────────────────────────────────

    /**
     * Create and attach the P1/P2 badge.  Called once by BoardGridPane
     * after construction.  The badge is stored separately so it survives
     * sprite swaps.
     */
    public void setBadge(String text, String color) {
        badgeLabel = new Label(text);
        badgeLabel.setStyle(
            "-fx-background-color:" + color + ";" +
            "-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:bold;" +
            "-fx-background-radius:4;-fx-padding:1 4;"
        );
        StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
        if (!getChildren().contains(badgeLabel)) {
            getChildren().add(badgeLabel);
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public MonsterGUI getPlayer() {
        return player;
    }

    public void setPlayer(MonsterGUI player) {
        // Remove the old player GUI but keep the badge
        getChildren().remove(this.player);
        this.player = player;
        if (!getChildren().contains(this.player)) {
            getChildren().add(0, this.player);
        }
    }

    public static String[] getList() {
        return list;
    }

    public MonsterGUI[] getPlayerList() {
        return playerList;
    }

    // ── Door animation ────────────────────────────────────────────────────

    /**
     * Swaps the graphic style into roaring/action stance when landing on a door.
     * Preserves the badge label across the swap.
     */
    public void openTheDoor() {
        MonsterGUI doorSprite = playerList[1];
        // Remove old player, add new one — badge stays
        getChildren().remove(this.player);
        this.player = doorSprite;
        getChildren().add(0, this.player);

        PauseTransition timer = new PauseTransition(Duration.millis(3200));
        timer.setOnFinished(e -> standUp());
        timer.play();
    }

    /**
     * Restores the standard sprite after door animation.
     * Preserves the badge label across the swap.
     */
    public void standUp() {
        MonsterGUI standard = playerList[0];
        if (this.player == standard) return; // already standard
        getChildren().remove(this.player);
        this.player = standard;
        getChildren().add(0, this.player);
    }

    // ── Cell-by-cell walk animation ────────────────────────────────────────

    /**
     * Handles linear node animations safely inside the board view grid
     * coordinate frames.  Walks the token through each intermediate cell.
     */
    public void moveAlongPath(List<StackPane> cellPanes,
        StackPane overlayPane,
        String destinationTag,
        Runnable onFinished) {

        if (cellPanes == null || cellPanes.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }

        // Remove clip so the token isn't cropped while walking
        player.standFromCell();

        SequentialTransition sequence = new SequentialTransition();

        for (StackPane destPane : cellPanes) {
            Timeline step = buildStepTimeline(destPane, overlayPane);
            sequence.getChildren().add(step);
        }

        sequence.setOnFinished(evt -> {
            // Re-apply cell clip at the final position
            player.setInCell();

            // Door animation if landing on a door cell
            if ("door".equals(destinationTag)) openTheDoor();

            if (onFinished != null) onFinished.run();
        });

        sequence.play();
    }

    // ── Straight-line transport animation (conveyor / sock) ────────────────

    /**
     * Moves the token in a straight line from its current position to the
     * centre of destPane.  Used for conveyor belt and contamination sock
     * transport effects.
     */
    public void moveDirect(StackPane destPane, StackPane overlayPane,
                           Runnable onFinished) {
        double[] xy = centreOf(destPane, overlayPane);

        // Remove clip for the slide
        player.standFromCell();

        Timeline slide = new Timeline(
            new KeyFrame(
                Duration.millis(300),
                new KeyValue(translateXProperty(), xy[0],
                             javafx.animation.Interpolator.EASE_BOTH),
                new KeyValue(translateYProperty(), xy[1],
                             javafx.animation.Interpolator.EASE_BOTH)
            )
        );

        slide.setOnFinished(evt -> {
            player.setInCell();
            if (onFinished != null) onFinished.run();
        });

        slide.play();
    }

    // ── Snap (no animation) ────────────────────────────────────────────────

    public void snapToCell(StackPane destPane, StackPane overlayPane) {
        double[] xy = centreOf(destPane, overlayPane);
        setTranslateX(xy[0]);
        setTranslateY(xy[1]);
        player.setInCell();
    }

    // ── Energy delta label ─────────────────────────────────────────────────

    /**
     * Show a floating energy-delta label over the token.
     */
    public void showEnergyDelta(int before, int after) {
        int delta = after - before;
        if (delta != 0) player.affect(delta);
    }

    /**
     * Renders a floating neon popup directly over the active token.
     */
    public void playEnergyPopupAnimation(int amount) {
        if (amount == 0) return;

        Platform.runLater(() -> {
            Label popupLabel = new Label();
            if (amount > 0) {
                popupLabel.setText("+" + amount + " \u26A1");
                popupLabel.setStyle(
                    "-fx-font-family: 'Arial Black'; -fx-font-size: 16px; " +
                    "-fx-text-fill: #00FFCC; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 4, 0, 0, 1);"
                );
            } else {
                popupLabel.setText(String.valueOf(amount) + " \uD83D\uDD0B");
                popupLabel.setStyle(
                    "-fx-font-family: 'Arial Black'; -fx-font-size: 16px; " +
                    "-fx-text-fill: #FF3366; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 4, 0, 0, 1);"
                );
            }

            this.getChildren().add(popupLabel);

            TranslateTransition floatUp = new TranslateTransition(Duration.millis(1000), popupLabel);
            floatUp.setFromY(0);
            floatUp.setToY(-cellSize * 0.8);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(1000), popupLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            ParallelTransition combinedAnimation = new ParallelTransition(floatUp, fadeOut);
            combinedAnimation.setOnFinished(e -> this.getChildren().remove(popupLabel));
            combinedAnimation.play();
        });
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private Timeline buildStepTimeline(StackPane destPane, StackPane overlayPane) {
        double[] xy = centreOf(destPane, overlayPane);
        return new Timeline(
            new KeyFrame(
                Duration.millis(STEP_MS),
                new KeyValue(translateXProperty(), xy[0],
                             javafx.animation.Interpolator.EASE_BOTH),
                new KeyValue(translateYProperty(), xy[1],
                             javafx.animation.Interpolator.EASE_BOTH)
            )
        );
    }

    /**
     * Return [translateX, translateY] that centres this token over
     * destPane, expressed in the overlay's local coordinate space.
     */
    private double[] centreOf(StackPane destPane, StackPane overlayPane) {
        Bounds dest    = destPane.localToScene(destPane.getBoundsInLocal());
        Bounds overlay = overlayPane.localToScene(overlayPane.getBoundsInLocal());

        double tokenW = getBoundsInLocal().getWidth();
        double tokenH = getBoundsInLocal().getHeight();

        // If the token has zero bounds (first frame), fall back to cellSize estimate
        if (tokenW == 0) tokenW = destPane.getPrefWidth()  * 0.75;
        if (tokenH == 0) tokenH = destPane.getPrefHeight() * 0.75;

        // Safety: if overlay has zero bounds (layout not done yet), fall back
        if (overlay.getWidth() <= 0 || overlay.getHeight() <= 0) {
            // Use dest bounds directly relative to overlay's (0,0)
            return new double[]{
                dest.getWidth()  / 2 - tokenW / 2,
                dest.getHeight() / 2 - tokenH / 2
            };
        }

        double x = dest.getMinX() - overlay.getMinX()
                 + dest.getWidth()  / 2 - tokenW / 2;
        double y = dest.getMinY() - overlay.getMinY()
                 + dest.getHeight() / 2 - tokenH / 2;

        return new double[]{x, y};
    }
}
