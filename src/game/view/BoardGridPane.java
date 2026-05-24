package game.view;

import game.engine.Constants;
import game.engine.Game;
import game.engine.Role;
import game.engine.cells.Cell;
import game.engine.cells.ConveyorBelt;
import game.engine.cells.ContaminationSock;
import game.engine.cells.DoorCell;
import game.engine.cells.MonsterCell;
import game.engine.cells.TransportCell;
import game.engine.monsters.Dasher;
import game.engine.monsters.Dynamo;
import game.engine.monsters.Monster;
import game.engine.monsters.MultiTasker;
import game.view.style.monsters.MonsterGUI;
import game.view.style.monsters.PlayerMonster;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class BoardGridPane extends GridPane {

    // ── Layout constants ───────────────────────────────────────────────────
    private final int ROWS = Constants.BOARD_ROWS;
    private final int COLS = Constants.BOARD_COLS;
    static final double CELL_SIZE = 80;

    // ── State ──────────────────────────────────────────────────────────────
    private final Game game;
    private BiConsumer<Integer, Cell> cellClickListener;

    /**
     * Direct cell-number → StackPane map.
     * Built once per drawBoard() call; never re-scanned.
     */
    private final StackPane[] cellPanes = new StackPane[ROWS * COLS];

    /** NPC stationed-monster GUI list — rebuilt on every drawBoard(). */
    final ArrayList<MonsterGUI> stationedMonsters = new ArrayList<>();

    // ── Overlay & tokens ──────────────────────────────────────────────────
    /**
     * Transparent StackPane that sits on top of the grid inside the
     * StackPane wrapper created by BoardView.  Player tokens live here
     * so they can translate freely without being clipped.
     *
     * CRITICAL: alignment must be TOP_LEFT so that every token's
     * layout origin is (0,0) in the overlay and translateX/Y values
     * map 1-to-1 to pixels from the overlay's top-left corner.
     */
    private final StackPane overlayPane = new StackPane();
    private final Pane transportLinesGroup = new Pane();
    private PlayerMonster playerToken;
    private PlayerMonster opponentToken;

    // ── Constructor ────────────────────────────────────────────────────────
    public BoardGridPane(Game game) {
        this.game = game;
        setHgap(4);
        setVgap(4);
        setAlignment(Pos.CENTER);

        // TOP_LEFT is required so token translateX/Y equals pixel offset
        // from the overlay's top-left corner (not its centre).
     // Transport-lines pane — unmanaged so StackPane won't reposition it
        transportLinesGroup.setManaged(false);
        transportLinesGroup.setMouseTransparent(true);
        overlayPane.getChildren().add(transportLinesGroup);
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setAlignment(Pos.TOP_LEFT);
        overlayPane.setPickOnBounds(false);
        overlayPane.setMouseTransparent(true);

        if (game == null || game.getBoard() == null) {
            System.out.println("ERROR: Game or Board is NULL");
            return;
        }
        drawBoard();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void setCellClickListener(BiConsumer<Integer, Cell> listener) {
        this.cellClickListener = listener;
    }

    /**
     * Full instant redraw.
     * Tokens are re-created and snapped to current engine positions.
     */
    public void refresh() {
        getChildren().clear();
        stationedMonsters.clear();
        drawBoard();
    }

    /**
     * Animate mover's token from fromCell to toCell.
     *
     * The path passed to PlayerMonster.moveAlongPath() depends on WHY
     * the monster ended up at toCell:
     *
     *   • Normal dice roll:  walk every intermediate cell (fromCell+1 … toCell).
     *   • Conveyor belt:     the engine moved the monster by the belt's effect
     *                        AFTER placing it on the belt cell.  We first walk
     *                        from fromCell to the belt cell (diceRoll steps),
     *                        then do a STRAIGHT JUMP to toCell.
     *   • Contamination sock: same pattern as conveyor — walk to the sock cell,
     *                        then straight jump backwards to toCell.
     *   • StartOver / swap:  toCell may be anywhere; always a straight jump
     *                        (path = [cellPanes[toCell]]).
     *
     * The caller (GamePlay) already ran playTurn() so:
     *   mover.getPosition() == toCell  (final engine-resolved position)
     *
     * We detect conveyor/sock by checking whether the raw dice-roll landing
     * cell (diceCell) is different from toCell.  diceCell = fromCell + diceRoll
     * (mod 100, clamped).  If diceCell == toCell the move was a plain roll.
     *
     * @param mover        monster that just moved
     * @param fromCell     position BEFORE the turn
     * @param toCell       engine-resolved final position AFTER the turn
     * @param diceRoll     the roll value returned by game.getLastDiceRoll()
     *                     (0 for a frozen-skip turn)
     * @param energyBefore mover energy snapshotted before playTurn()
     * @param onFinished   callback on FX thread when animation completes
     */
    public void animateTokenMove(Monster mover,int fromCell, int toCell,int landingCell,
                              	int energyBefore,Runnable onFinished) {

        PlayerMonster token = (mover == game.getPlayer()) ? playerToken : opponentToken;
        if (token == null) { if (onFinished != null) onFinished.run(); return; }

        // Frozen-skip: diceRoll is 0, no movement needed
        if (fromCell == landingCell) {
            if (onFinished != null) onFinished.run();
            return;
        }
     // ── Remove token from current cell pane, place in overlay for animation ──
        removeTokenFromCell(token, fromCell);
        // Safety: don't re-add if token is already in the overlay
        if (token.getParent() != overlayPane) overlayPane.getChildren().add(token);

        // Force layout so centreOf() coordinate calculations work
        layout();
        overlayPane.layout();
        StackPane fromPane = cellPanes[fromCell];
        token.snapToCell(fromPane, overlayPane);
        
        List<StackPane> path=buildStepPath(fromCell, landingCell);
        
        Cell destCell = getCellFromBoard(toCell);
        String destTag = (destCell instanceof DoorCell) ? "door" : "";//not proper
        token.moveAlongPath(path, overlayPane, destTag, () -> {
        	if(landingCell != toCell){
        		token.moveDirect(cellPanes[toCell], overlayPane, () -> {
        			overlayPane.getChildren().remove(token);
		            placeTokenInCell(token, toCell);
		            int energyAfter = mover.getEnergy();//?show delta not functioning
		            token.showEnergyDelta(energyBefore, energyAfter);
		            System.out.println("energy: "+(energyAfter - energyBefore));
		            if (onFinished != null) onFinished.run();});
        		return;
        	}
	        overlayPane.getChildren().remove(token);
            placeTokenInCell(token, toCell);
            int energyAfter = mover.getEnergy();//?show delta not functioning
            token.showEnergyDelta(energyBefore, energyAfter);
            System.out.println("energy: "+(energyAfter - energyBefore));
            if (onFinished != null) onFinished.run();
        });
        
	}

    /** Refresh status visuals (freeze/shield/confuse) on both tokens. */
    public void refreshTokenStatuses() {
        applyStatusToToken(playerToken,   game.getPlayer());
        applyStatusToToken(opponentToken, game.getOpponent());
    }
    
    public void removeTokenFromCell(PlayerMonster token, int cellIndex) {
    	StackPane cellPane = cellPanes[cellIndex];
    	Cell cell = getCellFromBoard(cellIndex);
    	if (cell instanceof MonsterCell) {
    		BorderPane bp = findBorderPane(cellPane);
    	// Remove player from left of the BorderPane
			bp.setLeft(null);	
			Node right = bp.getRight();
			bp.setRight(null);
			bp.setCenter(right);	// Restore stationed monster to center
    	} else	cellPane.getChildren().remove(token);
    	token.badgeLabel.setVisible(false);
    }
    
    public void placeTokenInCell(PlayerMonster token, int cellIndex) {
    	StackPane cellPane = cellPanes[cellIndex];
    	Cell cell = getCellFromBoard(cellIndex);
    	// Reset translation — cell layout handles positioning
    	token.setTranslateX(0);
    	token.setTranslateY(0);
    	token.getPlayer().setInCell();
    	if (cell instanceof MonsterCell) {
    		BorderPane bp = findBorderPane(cellPane);
			// Move stationed monster from center to right
			Node center = bp.getCenter();
			if(((MonsterCell) cell).getCellMonster().getRole()==cell.getMonster().getRole())//cup effect
				token.getPlayer().powerUPeffect();
			if (center != null) {
				bp.setCenter(null);
				bp.setRight(center);
			}
			// Add player token to left of the BorderPane
			bp.setLeft(token);
    	} else 	cellPane.getChildren().add(token);
    	token.badgeLabel.setVisible(true);
    }
    
    // ── Coordinate helpers ─────────────────────────────────────────────────
    
    public int[] cellnumberToGridIndex(int index) {
        int row = index / COLS;
        int col = index % COLS;
        if (row % 2 == 1) col = COLS - 1 - col;
        return new int[]{row, col};
    }

    public Cell getCellFromBoard(int index) {
        if (game == null || game.getBoard() == null) return null;
        int[] pos = cellnumberToGridIndex(index);
        Cell[][] board = game.getBoard().getBoardCells();
        if (board == null || board[pos[0]] == null) return null;
        return board[pos[0]][pos[1]];
    }

    // ── Overlay / token accessors ──────────────────────────────────────────

    public StackPane     getOverlayPane()   { return overlayPane; }
    public PlayerMonster getPlayerToken()   { return playerToken; }
    public PlayerMonster getOpponentToken() { return opponentToken; }

    // ── Internal: draw ─────────────────────────────────────────────────────

    private void drawBoard() {
        if (game == null || game.getBoard() == null) return;

        Monster player   = game.getPlayer();
        Monster opponent = game.getOpponent();

        for (int cellNumber = (ROWS * COLS) - 1; cellNumber >= 0; cellNumber--) {
            int boardRow = cellNumber / COLS;
            int gridRow  = (ROWS - 1) - boardRow;
            int col = (boardRow % 2 == 0)
                    ? cellNumber % COLS
                    : (COLS - 1) - (cellNumber % COLS);

            Cell cell = getCellFromBoard(cellNumber);
            StackPane pane = buildCellPane(cellNumber, cell, player, opponent);
            cellPanes[cellNumber] = pane;
            this.add(pane, col, gridRow);
        }

        for (MonsterGUI mg : stationedMonsters) mg.setInCell();

        // Token placement needs post-layout bounds → defer one pulse
        Platform.runLater(() -> {
        	placeTokens(player, opponent);
        	drawTransportLines();
        });
    }

    // ── Internal: token placement ──────────────────────────────────────────

    private void placeTokens(Monster player, Monster opponent) {
        playerToken   = createToken(player,   "P1", "#1d4ed8");
        opponentToken = createToken(opponent, "P2", "#b91c1c");

        applyStatusToToken(playerToken,   player);
        applyStatusToToken(opponentToken, opponent);
        
     // Clear overlay (tokens no longer live there permanently)
        // Preserve the transport-lines group which is a permanent overlay child
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(transportLinesGroup);
        // Add tokens directly to their cell panes — no coordinate math needed
        placeTokenInCell(playerToken,   player.getPosition());
        placeTokenInCell(opponentToken, opponent.getPosition());
    }

    private PlayerMonster createToken(Monster m, String badge, String badgeColor) {
        PlayerMonster token = new PlayerMonster(m.getName(), CELL_SIZE *0.95, resolveMonsterColor(m));
        MonsterGUI mg =token.getPlayer();
        if (m.getRole() == Role.LAUGHER) mg.laugherGUI(); else mg.scarerGUI();
        // Badge label — stored in PlayerMonster so it survives sprite swaps
        token.setBadge(badge, badgeColor);
        return token;
    }

    public void applyStatusToToken(PlayerMonster token, Monster monster) {
        if (token == null || monster == null) return;
        MonsterGUI gui = token.getPlayer();
        if (gui == null) return;
        gui.monSet();
        if      (monster.isFrozen())   gui.coldFreeze(monster.isShielded());
        else if (monster.isShielded()) gui.shield(true);
        else if (monster.isConfused()) {
        	if (monster.getRole() == Role.LAUGHER) gui.laugherGUI(); else gui.scarerGUI();
        	gui.confuse();
        }//powered		????
    }

    // ── Internal: path builders ────────────────────────────────────────────

    /**
     * Build an ordered list of cell panes for a step-by-step walk from
     * {@code fromCell} (exclusive) to {@code toCell} (inclusive).
     *
     * Direction is determined by the shortest path on the board:
     *   • If toCell > fromCell  → always forward (normal dice roll).
     *   • If toCell < fromCell  → backward (contamination sock / start-over).
     *   • Wrap-around (e.g. fromCell=98, toCell=2) → forward (shorter arc).
     *
     * This is the only place direction is decided, so conveyor and sock
     * displacement always walk the correct arc.
     */
    private List<StackPane> buildStepPath(int fromCell, int toCell) {
        List<StackPane> path = new ArrayList<>();
        if (fromCell == toCell) return path;

        int size = ROWS * COLS;
        int forwardSteps  = (toCell - fromCell + size) % size;
        int current  = fromCell;
        int maxSteps = 20; // safety cap — prevents infinite loop
        while (current != toCell && forwardSteps-- >0 & maxSteps-- > 0 ) {
            current = (current + 1) % size;
            if (cellPanes[current] != null)//?important
                path.add(cellPanes[current]);
        }
        return path;
    }

    // ── Transport lines (green = conveyor, red = sock) ───────────────────
    /**
     * Draws solid lines from each transport cell centre to its destination
     * cell centre:
     *   - Green lines for ConveyorBelt cells (cell + positive effect)
     *   - Red   lines for ContaminationSock cells (cell + negative effect)
     *
     * Each line is a thick solid arrow with a triangular arrowhead at the
     * destination end.  Lines are drawn on the overlay pane so they span
     * across cells without clipping.
     * Must be called after layout is complete (e.g. inside Platform.runLater).
     */
    public void drawTransportLines() {
    	transportLinesGroup.getChildren().clear();
    	
    	// ── Conveyor lines (green) ──
    	for (int srcIdx : Constants.CONVEYOR_CELL_INDICES) {
    		Cell cell = getCellFromBoard(srcIdx);
    		if (cell instanceof ConveyorBelt) {
    			int effect = ((ConveyorBelt) cell).getEffect();
    			int destIdx = Math.max(0, Math.min(ROWS * COLS - 1, srcIdx + effect));
    			drawTransportLine(srcIdx, destIdx, Color.web("#22c55e"), 3.0);
    		}
    	}
    	// ── Sock lines (red) ──
    	for (int srcIdx : Constants.SOCK_CELL_INDICES) {
    		Cell cell = getCellFromBoard(srcIdx);
    		if (cell instanceof ContaminationSock) {
    			int effect = ((ContaminationSock) cell).getEffect();
    			int destIdx = Math.max(0, Math.min(ROWS * COLS - 1, srcIdx + effect));
    			drawTransportLine(srcIdx, destIdx, Color.web("#ff4444"), 3.0);
    		}
    	}
    }
    
    /**
     * Draws a solid line from the centre of srcIdx cell to the centre of
     * destIdx cell, with a triangular arrowhead at the destination end.
     */
    private void drawTransportLine(int srcIdx, int destIdx, Color color, double strokeWidth) {
    	double[] src = cellCenterInOverlay(srcIdx);
    	double[] dst = cellCenterInOverlay(destIdx);
    	if (src == null || dst == null) return;
    	// ── Solid line from source centre to destination centre ──
    	Line line = new Line(src[0], src[1], dst[0], dst[1]);
    	line.setStroke(color);
    	line.setStrokeWidth(strokeWidth);
    	line.setMouseTransparent(true);
    	// ── Arrowhead triangle at the destination ──
    	Polygon arrowhead =
    			buildArrowhead(src[0], src[1], dst[0], dst[1], color);
    	if (arrowhead != null) {
    		arrowhead.setMouseTransparent(true);
    		transportLinesGroup.getChildren().addAll(line, arrowhead);
    	} else {
    		transportLinesGroup.getChildren().add(line);
    	}
    }
    /**
     * Builds a triangular arrowhead at (dstX, dstY) pointing from
     * (srcX, srcY) towards (dstX, dstY).
     */
    private Polygon buildArrowhead(double srcX, double srcY,double dstX, double dstY, Color fill) {
    	double dx = dstX - srcX;
    	double dy = dstY - srcY;
    	double len = Math.sqrt(dx * dx + dy * dy);
    	if (len < 1.0) return null;
    	double ux = dx / len;
    	double uy = dy / len;
    	double arrowLen = 14.0;
    	double arrowW   = 7.0;
    
    	double tipX  = dstX;
    	double tipY  = dstY;
    	double baseX = tipX - ux * arrowLen;
    	double baseY = tipY - uy * arrowLen;
    	double px    = -uy * arrowW;
    	double py    =  ux * arrowW;
    
    	Polygon arrow = new Polygon();
    	arrow.getPoints().addAll(tipX, tipY, baseX + px, baseY + py, baseX - px, baseY - py);
    	arrow.setFill(fill);
    	return arrow;
    }
    /**
   * Returns the centre point of a cell pane in the overlay pane's local
   * coordinate space, or null if the cell index is invalid.
   */
    private double[] cellCenterInOverlay(int cellIndex) {
    	if (cellIndex < 0 || cellIndex >= cellPanes.length) return null;
    	StackPane pane = cellPanes[cellIndex];
    	if (pane == null) return null;
    	javafx.geometry.Bounds bounds = pane.localToScene(pane.getBoundsInLocal());
    	double sceneX = bounds.getMinX() + bounds.getWidth()  / 2.0;
    	double sceneY = bounds.getMinY() + bounds.getHeight() / 2.0;

    	javafx.geometry.Point2D local = overlayPane.sceneToLocal(sceneX, sceneY);
    	return new double[]{ local.getX(), local.getY() };
    }
    // ── Internal: cell pane builder ───────────────────────────────────────

    private StackPane buildCellPane(int cellNumber, Cell cell,
                                    Monster player, Monster opponent) {
        StackPane pane = new StackPane();
        pane.setPrefSize(CELL_SIZE * 2, CELL_SIZE);
        pane.setMinSize(CELL_SIZE,      CELL_SIZE);
        pane.setMaxSize(CELL_SIZE * 2,  CELL_SIZE);
        pane.getStyleClass().add("board-cell");
        pane.getStyleClass().add(getCellStyle(cellNumber, cell));

        if (cell instanceof TransportCell) {
        	 int offset = ((TransportCell) cell).getEffect();
        	 int dest = cellNumber + offset;
        	 if(cell instanceof ConveyorBelt)
        		 Tooltip.install(pane, new Tooltip("Conveyor Belt ⚙️\nMoves you " + offset + " cells to Cell " + dest));
        	 else 
        		 Tooltip.install(pane, new Tooltip("Contamination Sock ⚙️\nSlips you " + offset + " cells to Cell " + dest));
        } else 	Tooltip.install(pane, new Tooltip("Conveyor Belt ⚙️"));

        String imgFile = getCellImageFilename(cellNumber, cell);
        if (imgFile != null) {
            ImageView bg = loadCellImage(imgFile);
            if (bg != null) { StackPane.setAlignment(bg, Pos.CENTER); pane.getChildren().add(bg); }
        }

        Label coord = new Label(String.valueOf(cellNumber));
        coord.getStyleClass().add("coord-label");
        StackPane.setAlignment(coord, Pos.TOP_LEFT);
        pane.getChildren().add(coord);

        String icon = getCellIcon(cellNumber, cell);
        if (icon != null && imgFile == null) {
            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("cell-icon-label");
            StackPane.setAlignment(iconLabel, Pos.CENTER);
            pane.getChildren().add(iconLabel);
        }

        if (cell instanceof MonsterCell) {
            MonsterCell mc = (MonsterCell) cell;
            Monster stationed = mc.getCellMonster();
            if (stationed != null) {
                MonsterGUI mg = new MonsterGUI(
                        "s_" + stationed.getName(), CELL_SIZE, resolveMonsterColor(stationed));
                if (stationed.getRole() == Role.LAUGHER) mg.laugherGUI(); else mg.scarerGUI();
                stationedMonsters.add(mg);
                BorderPane wrap = new BorderPane();
                wrap.setCenter(new StackPane(mg));
                pane.getChildren().add(wrap);
            }
        }

        final int  cn  = cellNumber;
        final Cell cel = cell;
        pane.setOnMouseClicked(e -> {
            if (cellClickListener != null) cellClickListener.accept(cn, cel);
            for (Node n : getChildren())
                if (n instanceof StackPane) n.getStyleClass().remove("cell-selected");
            pane.getStyleClass().add("cell-selected");
        });

        return pane;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private BorderPane findBorderPane(StackPane parent) {
    	for (Node n : parent.getChildren())  if (n instanceof BorderPane) return (BorderPane) n;
    	return null;
    }
    
    private String resolveMonsterColor(Monster m) {
        if (m instanceof Dynamo)      return "#4CA2F7";
        if (m instanceof Dasher)      return "#97DE4A";
        if (m instanceof MultiTasker) return "#FF8C00";
        return "#C0C0C0";
    }

    private boolean contains(int[] arr, int n) {
        if (arr == null) return false;
        for (int v : arr) if (v == n) return true;
        return false;
    }

    private String getCellStyle(int n, Cell cell) {
        if (n == Constants.STARTING_POSITION)             return "start-cell";
        if (n == Constants.WINNING_POSITION)              return "end-cell";
        if (contains(Constants.MONSTER_CELL_INDICES,  n)) return "monster-cell";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n)) return "conveyor-cell";
        if (contains(Constants.SOCK_CELL_INDICES,     n)) return "sock-cell";
        if (contains(Constants.CARD_CELL_INDICES,     n)) return "card-cell";
        if (cell instanceof DoorCell)
            return ((DoorCell) cell).getRole() == Role.SCARER ? "scare-door-cell" : "laugh-door-cell";
        return "normal-cell";
    }

    private String getCellIcon(int n, Cell cell) {
        if (n == Constants.STARTING_POSITION)             return "🏠";
        if (n == Constants.WINNING_POSITION)              return "🏁";
        if (contains(Constants.MONSTER_CELL_INDICES,  n)) return "⚡";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n)) return "→";
        if (contains(Constants.SOCK_CELL_INDICES,     n)) return "🧦";
        if (contains(Constants.CARD_CELL_INDICES,     n)) return "🃏";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            if (dc.isActivated()) return "❌";
            return dc.getRole() == Role.SCARER ? "⚡" : "😂";
        }
        return null;
    }

    private String getCellImageFilename(int n, Cell cell) {
        if (n == Constants.STARTING_POSITION)                         return "start.png";
        if (n == Constants.WINNING_POSITION)                          return "end.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n) && n != 66) return "conveyor.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n) && n == 66) return "conveyorleft.png";
        if (contains(Constants.SOCK_CELL_INDICES,     n))             return "sock.png";
        if (contains(Constants.CARD_CELL_INDICES,     n))             return "card.png";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            return dc.isActivated() ? "door_exhausted.png"
                    : (dc.getRole() == Role.SCARER ? "scare_door.png" : "laugh_door.png");
        }
        return null;
    }

    private ImageView loadCellImage(String filename) {
        URL url = getClass().getResource("/assets/" + filename);
        if (url == null) return null;
        try {
            Image img = new Image(url.toExternalForm(), CELL_SIZE * 2, CELL_SIZE, false, true);
            if (img.isError()) return null;
            ImageView iv = new ImageView(img);
            iv.setFitWidth(CELL_SIZE * 2);
            iv.setFitHeight(CELL_SIZE);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) { return null; }
    }
}