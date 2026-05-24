package game.view.style.monsters;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

/**
 * PlayerMonster — animated player token that lives on the board overlay.
 *
 * ── Movement design ───────────────────────────────────────────────────────
 *
 * Previous versions used SequentialTransition + KeyFrame to walk cells.
 * That approach teleported the token (no interpolation) because KeyFrame
 * actions just set a value instantly at the frame boundary.
 *
 * This version uses a recursive TranslateTransition chain:
 *   - Step 0 starts a TranslateTransition toward path.get(0)'s centre.
 *   - When that transition finishes its onFinished callback computes the
 *     centre of path.get(1) AT THAT MOMENT and starts the next transition.
 *   - This continues until all cells in the path are visited.
 *
 * Computing the target lazily (inside onFinished, not at list-build time)
 * is critical: localToScene() / sceneToLocal() only return correct values
 * AFTER layout has been applied.  At path-build time, cells that haven't
 * been on screen yet may report zero or stale bounds.
 *
 * ── Coordinate model ──────────────────────────────────────────────────────
 *
 * The token is a direct child of overlayPane (StackPane with TOP_LEFT
 * alignment set by BoardGridPane).  Its layout origin is therefore always
 * (0, 0) inside the overlay, so position is controlled entirely through
 * translateX / translateY.
 *
 * To centre the token over a destination cell:
 *   1. cell.localToScene(bounds)           → cell bounds in scene space
 *   2. overlayPane.sceneToLocal(centreX, centreY)  → centre in overlay space
 *   3. translateX = overlayLocal.x - tokenW/2
 *      translateY = overlayLocal.y - tokenH/2
 *
 * sceneToLocal() correctly inverts ALL ancestor transforms (GridPane gaps,
 * BorderPane margins, StackPane centering, full-screen scaling, HiDPI).
 */
public class PlayerMonster extends StackPane {

    private static final String[] list = {
        "James P. Sullivan", "Mike Wazowski", "Randall Boggs",
        "Celia Mae", "Roz", "Fungus", "Henry J. Waternoose", "Yeti"
    };

    /** [0] = standing sprite, [1] = door-open sprite */
    private final MonsterGUI[] playerList;

    /** Currently visible MonsterGUI child */
    private MonsterGUI player;
    
    //private final double cellSize;
    private static final double STEP_MS = 487.5;

    /** Badge label — persists across sprite swaps. */
    public Label badgeLabel;
    /**
     * Duration of ONE cell-to-cell slide in milliseconds.
     * Shorter = snappier walk.  160 ms feels natural at 6 steps.
     */

    // ── Constructor ────────────────────────────────────────────────────────
    public PlayerMonster(String monsterName, double cellSize, String typeColor) {
        MonsterGUI standing = new MonsterGUI("s_" + monsterName, cellSize - 3, typeColor);
        MonsterGUI doorOpen = new MonsterGUI("o_" + monsterName, cellSize - 3, typeColor);
        this.playerList = new MonsterGUI[]{standing, doorOpen};
        this.player = playerList[0];
        getChildren().add(this.player);
        setPickOnBounds(false);
    }
    
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
    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Walk the token smoothly through every cell in {@code path}, then call
     * {@code onFinished}.
     *
     * Each cell-to-cell slide is a {@link TranslateTransition} whose target
     * is computed RIGHT WHEN that slide starts (lazy evaluation), so bounds
     * are always valid regardless of how many cells are in the path.
     *
     * @param path           ordered StackPanes to visit (origin excluded,
     *                       destination included).  A single element means
     *                       a direct jump (conveyor / sock).
     * @param overlayPane    direct parent of this token — alignment TOP_LEFT
     * @param destinationTag "door" triggers the door-open sprite at the end
     * @param onFinished     called on FX thread after last slide completes
     */
    public void moveAlongPath(List<StackPane> path,
                              StackPane overlayPane,
                              String destinationTag,
                              Runnable onFinished) {

        if (path == null || path.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }

        // Remove cell clip so sprite isn't cropped during the walk
        player.standFromCell();

        // Kick off the recursive chain starting at index 0
        stepTo(path, 0, overlayPane, destinationTag, onFinished);
    }

    /**
     * Instantly snap the token to the centre of {@code destPane}.
     * Used after every full board refresh so tokens are in the right place
     * without animation.
     */
    public void moveDirect(StackPane destPane, StackPane overlayPane, Runnable onFinished) {
		double[] xy = centreOf(destPane, overlayPane);
		
		// Remove clip for the slide
		player.standFromCell();
		
		Timeline slide = new Timeline(
		new KeyFrame(Duration.millis(300), new KeyValue(translateXProperty(), xy[0], 
				javafx.animation.Interpolator.EASE_BOTH),
		new KeyValue(translateYProperty(), xy[1],  javafx.animation.Interpolator.EASE_BOTH)));
		
		slide.setOnFinished(evt -> {
			player.setInCell();
			if (onFinished != null) onFinished.run();
		});
		
		slide.play();
}
    public void snapToCell(StackPane destPane, StackPane overlayPane) {
        double[] xy = centreOf(destPane, overlayPane);
        setTranslateX(xy[0]);
        setTranslateY(xy[1]);
        player.setInCell();
    }

    /** Show a floating +/- energy label above the token. */
    public void showEnergyDelta(int before, int after) {
        int delta = after - before;
        if (delta != 0) player.affect(delta);
    }

    // ── Door animation ─────────────────────────────────────────────────────

    public void openTheDoor() {
        getChildren().setAll(playerList[1],badgeLabel);
        this.player = playerList[1];
        PauseTransition timer = new PauseTransition(Duration.millis(3200));
        timer.setOnFinished(e -> standUp());
        timer.play();
    }

    public void standUp() {
    	//badgeLabel.setVisible(false);
        this.player = playerList[0];
        getChildren().setAll(this.player,badgeLabel);
    }

    // ── Getters / setters ──────────────────────────────────────────────────

    public MonsterGUI    getPlayer()             { return player; }
    public MonsterGUI[]  getPlayerList()         { return playerList; }
    public static String[] getList()             { return list; }

    // ── Private: recursive step chain ─────────────────────────────────────

    /**
     * Start a TranslateTransition toward {@code path.get(stepIndex)}.
     * When it finishes, either start the next step or fire the final callback.
     *
     * Because the target is computed here — at the moment the step fires —
     * localToScene() always sees the correct, post-layout bounds even for
     * cells that are far from the token's current position.
     */
    private void stepTo(List<StackPane> path,
                        int stepIndex,
                        StackPane overlayPane,
                        String destinationTag,
                        Runnable onFinished) {
    		///? as queue is alot better
        if (stepIndex >= path.size()) {
            if ("door".equals(destinationTag)) openTheDoor();
            player.setInCell();
            if (onFinished != null) onFinished.run();
            return;
        }

        StackPane destPane = path.get(stepIndex);

        // Compute target coordinates LAZILY right now, not at list-build time
        double[] xy = centreOf(destPane, overlayPane);

        TranslateTransition slide = new TranslateTransition(Duration.millis(STEP_MS), this);
        slide.setToX(xy[0]);
        slide.setToY(xy[1]);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        slide.setOnFinished(evt ->stepTo(path, stepIndex + 1, overlayPane, destinationTag, onFinished));
        slide.play();
    }

    // ── Coordinate helper ──────────────────────────────────────────────────

    /**
     * Compute [translateX, translateY] to centre THIS token over
     * {@code destPane} in the overlay's local coordinate space.
     *
     *   a) destPane.localToScene(bounds)   → cell rectangle in scene space
     *   b) compute scene-space centre of cell
     *   c) overlayPane.sceneToLocal(cx, cy) → that point in overlay space
     *   d) subtract half token size → top-left to translate to
     *
     * Using sceneToLocal() on the overlay means ALL ancestor transforms
     * (GridPane gaps, BorderPane insets, StackPane alignment, full-screen
     * scaling, HiDPI) are automatically accounted for.
     */
    private double[] centreOf(StackPane destPane, StackPane overlayPane) {
        // a) cell bounds in scene coordinates
        Bounds cellInScene = destPane.localToScene(destPane.getBoundsInLocal());

        // b) scene-space centre of the cell
        double sceneCX = cellInScene.getMinX() + cellInScene.getWidth()  / 2.0;
        double sceneCY = cellInScene.getMinY() + cellInScene.getHeight() / 2.0;

        // c) that point expressed in the overlay's own local space
        Point2D localCentre = overlayPane.sceneToLocal(sceneCX, sceneCY);

        // d) token size — prefer actual rendered bounds, fall back to pref/cell
        double tokenW = getBoundsInLocal().getWidth();
        double tokenH = getBoundsInLocal().getHeight();
        if (tokenW <= 0) tokenW = getPrefWidth();
        if (tokenH <= 0) tokenH = getPrefHeight();
        if (tokenW <= 0) tokenW = cellInScene.getWidth()  * 0.75;
        if (tokenH <= 0) tokenH = cellInScene.getHeight() * 0.75;

        return new double[]{
            localCentre.getX() - tokenW / 2.0,
            localCentre.getY() - tokenH / 2.0
        };
    }
}