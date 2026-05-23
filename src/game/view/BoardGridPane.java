package game.view;

import game.engine.Constants;
import game.engine.Game;
import game.engine.Role;
import game.engine.cells.Cell;
import game.engine.cells.ConveyorBelt;
import game.engine.cells.ContaminationSock;
import game.engine.cells.DoorCell;
import game.engine.cells.MonsterCell;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * BoardGridPane — 10x10 board with animated PlayerMonster tokens.
 *
 * Fixes vs previous version:
 *   - overlayPane is exposed via getOverlayPane() and must be stacked on top
 *     of this GridPane inside a parent StackPane (done in BoardView).
 *   - animateTokenMove() splits movement into two phases when the landing
 *     cell is a transport cell (ConveyorBelt / ContaminationSock):
 *       Phase 1: walk cell-by-cell from fromCell to landingCell
 *       Phase 2: straight-line slide from landingCell to toCell
 *   - buildPathPanes() clamps all cell indices to [0, 99] and correctly
 *     handles forward and backward movement.
 *   - refresh() preserves the overlayPane in the scene and rebuilds tokens.
 */
public class BoardGridPane extends GridPane {

    // ── Layout constants ───────────────────────────────────────────────────
    private final int ROWS = Constants.BOARD_ROWS;
    private final int COLS = Constants.BOARD_COLS;
    static final double CELL_SIZE = 80;   // package-visible so PlayerMonster can read it

    // ── State ──────────────────────────────────────────────────────────────
    private final Game game;
    private BiConsumer<Integer, Cell> cellClickListener;

    /** Direct index-to-pane map so animation code never scans GridPane children. */
    private final StackPane[] cellPanes = new StackPane[ROWS * COLS];

    /** NPC stationed-monster GUI list — rebuilt on every drawBoard(). */
    final ArrayList<MonsterGUI> stationedMonsters = new ArrayList<>();

    // ── Overlay & tokens ──────────────────────────────────────────────────
    /**
     * Transparent overlay that sits above the grid so tokens translate freely
     * without being clipped by individual cell panes.
     * BoardView must stack this on top of this GridPane inside a StackPane.
     */
    private final StackPane overlayPane;

    private PlayerMonster playerToken;
    private PlayerMonster opponentToken;
   /** Pane that holds all transport-line visuals (green/red) on the overlay.
     *  Uses Pane (not Group) so it doesn't auto-size to its children, and
     *  setManaged(false) so the parent StackPane won't centre-shift it. */
    private final Pane transportLinesGroup = new Pane();
    // ── Constructor ────────────────────────────────────────────────────────
    public BoardGridPane(Game game) {
        this.game = game;

        // Create overlay pane — must fill same area as grid for coordinate math
        overlayPane = new StackPane();
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMouseTransparent(true);

        // Transport-lines pane — unmanaged so StackPane won't reposition it
        transportLinesGroup.setManaged(false);
        transportLinesGroup.setMouseTransparent(true);
        overlayPane.getChildren().add(transportLinesGroup);
        setHgap(4);
        setVgap(4);
        setAlignment(Pos.CENTER);

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
     * Full instant redraw — used for frozen-skip turns, powerup use,
     * and post-animation refresh.  Tokens snap to current engine positions.
     */
    public void refresh() {
        getChildren().clear();
        stationedMonsters.clear();
        drawBoard();
    }

    /**
     * Animate mover's token walking from fromCell to toCell.
     *
     * The token is removed from its current cell pane, placed in the overlay
     * for smooth animation, then moved into the destination cell pane on
     * completion so it is always visible.
     *
     * If landingCell differs from toCell (transport cell displaced the
     * monster), the animation is split:
     *   Phase 1: walk cell-by-cell from fromCell to landingCell
     *   Phase 2: straight-line slide from landingCell to toCell
     *
     * @param mover        the monster that just moved
     * @param fromCell     cell index before the move
     * @param toCell       cell index after the move (engine-resolved)
     * @param landingCell  cell index after dice roll, BEFORE transport effect
     * @param energyBefore mover's energy BEFORE the turn
     * @param onFinished   callback on FX thread after walk + energy label
     */
    public void animateTokenMove(Monster mover,
                                 int fromCell, int toCell, int landingCell,
                                 int energyBefore,
                                 Runnable onFinished) {

        PlayerMonster token = (mover == game.getPlayer()) ? playerToken : opponentToken;
        if (token == null) { if (onFinished != null) onFinished.run(); return; }

        if (fromCell == toCell) {
            // No movement (e.g. frozen skip)
            if (onFinished != null) onFinished.run();
            return;
        }

        // ── Remove token from current cell pane, place in overlay for animation ──
        removeTokenFromCell(token, fromCell);
        // Safety: don't re-add if token is already in the overlay
        if (token.getParent() != overlayPane) {
            overlayPane.getChildren().add(token);
        }

        // Force layout so centreOf() coordinate calculations work
        layout();
        overlayPane.layout();

        // Snap token to starting cell position in overlay
        StackPane fromPane = (fromCell >= 0 && fromCell < cellPanes.length) ? cellPanes[fromCell] : null;
        if (fromPane != null) token.snapToCell(fromPane, overlayPane);

        // Determine destination tag (door / normal)
        Cell destCell = getCellFromBoard(toCell);
        String destTag = (destCell instanceof DoorCell) ? "door" : "";

        // Check if the landing cell is a transport cell that displaced the monster
        Cell landingCellObj = getCellFromBoard(landingCell);
        boolean hasTransport = landingCell >= 0
                && landingCell != toCell
                && (landingCellObj instanceof ConveyorBelt || landingCellObj instanceof ContaminationSock);

        if (hasTransport) {
            // ── Phase 1: walk from fromCell to landingCell ──
            List<StackPane> walkPath = buildPathPanes(fromCell, landingCell);

            // ── Phase 2: straight-line from landingCell to toCell ──
            StackPane transportDest = (toCell >= 0 && toCell < cellPanes.length) ? cellPanes[toCell] : null;

            token.moveAlongPath(walkPath, overlayPane, "", () -> {
                // After walking to the landing cell, do the straight-line transport
                if (transportDest != null) {
                    token.moveDirect(transportDest, overlayPane, () -> {
                        // Re-apply cell clip at final position
                        token.getPlayer().setInCell();

                        // Door animation if landing on a door at final destination
                        if ("door".equals(destTag)) token.openTheDoor();

                        // Show energy delta
                        int energyAfter = mover.getEnergy();
                        token.showEnergyDelta(energyBefore, energyAfter);

                        // ── Move token from overlay into destination cell pane ──
                        overlayPane.getChildren().remove(token);
                        placeTokenInCell(token, toCell);

                        if (onFinished != null) onFinished.run();
                    });
                } else {
                    token.getPlayer().setInCell();
                    if ("door".equals(destTag)) token.openTheDoor();
                    int energyAfter = mover.getEnergy();
                    token.showEnergyDelta(energyBefore, energyAfter);

                    // ── Move token from overlay into destination cell pane ──
                    overlayPane.getChildren().remove(token);
                    placeTokenInCell(token, toCell);

                    if (onFinished != null) onFinished.run();
                }
            });
        } else {
            // ── Normal walk: from fromCell to toCell ──
            List<StackPane> path = buildPathPanes(fromCell, toCell);

            token.moveAlongPath(path, overlayPane, destTag, () -> {
                // Show energy delta over the token that just moved
                int energyAfter = mover.getEnergy();
                token.showEnergyDelta(energyBefore, energyAfter);

                // ── Move token from overlay into destination cell pane ──
                overlayPane.getChildren().remove(token);
                placeTokenInCell(token, toCell);

                if (onFinished != null) onFinished.run();
            });
        }
    }

    /** Refresh only the status visuals (freeze/shield/confuse) on both tokens. */
    public void refreshTokenStatuses() {
        applyStatusToToken(playerToken,   game.getPlayer());
        applyStatusToToken(opponentToken, game.getOpponent());
    }

    // ── Cell-based token placement ────────────────────────────────────────

    /**
     * Place a player token directly into the destination cell's StackPane.
     * If the cell is a MonsterCell, the stationed NPC monster is moved to the
     * RIGHT of the BorderPane and the player token is placed on the LEFT.
     * For all other cells the token is simply added as a child of the StackPane.
     */
    public void placeTokenInCell(PlayerMonster token, int cellIndex) {
        if (cellIndex < 0 || cellIndex >= cellPanes.length) return;
        StackPane cellPane = cellPanes[cellIndex];
        Cell cell = getCellFromBoard(cellIndex);

        // Reset translation — cell layout handles positioning
        token.setTranslateX(0);
        token.setTranslateY(0);
        token.getPlayer().setInCell();

        if (cell instanceof MonsterCell) {
            BorderPane bp = findBorderPane(cellPane);
            if (bp != null) {
                // Move stationed monster from center to right
                Node center = bp.getCenter();
                if (center != null) {
                    bp.setCenter(null);
                    bp.setRight(center);
                }
                // Add player token to left of the BorderPane
                bp.setLeft(token);
            } else {
                // Fallback: no BorderPane found, just add to cell
                cellPane.getChildren().add(token);
            }
        } else {
            cellPane.getChildren().add(token);
        }
    }

    /**
     * Remove a player token from its current cell's StackPane.
     * If the cell is a MonsterCell, the stationed NPC monster is restored
     * to the CENTER of the BorderPane.
     */
    public void removeTokenFromCell(PlayerMonster token, int cellIndex) {
        if (cellIndex < 0 || cellIndex >= cellPanes.length) return;
        StackPane cellPane = cellPanes[cellIndex];
        Cell cell = getCellFromBoard(cellIndex);

        if (cell instanceof MonsterCell) {
            BorderPane bp = findBorderPane(cellPane);
            if (bp != null) {
                // Remove player from left of the BorderPane
                bp.setLeft(null);
                // Restore stationed monster to center
                Node right = bp.getRight();
                if (right != null) {
                    bp.setRight(null);
                    bp.setCenter(right);
                }
            }
        } else {
            cellPane.getChildren().remove(token);
        }
    }

    /**
     * Find the first BorderPane child of a cell StackPane (used for MonsterCell layout).
     */
    private BorderPane findBorderPane(StackPane parent) {
        for (Node n : parent.getChildren()) {
            if (n instanceof BorderPane) return (BorderPane) n;
        }
        return null;
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
    private void drawTransportLine(int srcIdx, int destIdx,
                                   Color color, double strokeWidth) {
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
    private Polygon buildArrowhead(double srcX, double srcY,
                                    double dstX, double dstY,
                                    Color fill) {
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
        arrow.getPoints().addAll(
                tipX,       tipY,
                baseX + px, baseY + py,
                baseX - px, baseY - py);
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

    // ── Coordinate helpers ─────────────────────────────────────────────────

    public int[] cellnumberToGridIndex(int index) {
        int row = index / COLS;
        int col = index % COLS;
        if (row % 2 == 1) col = COLS - 1 - col;
        return new int[]{row, col};
    }

    public Cell getCellFromBoard(int index) {
        if (game == null || game.getBoard() == null) return null;
        if (index < 0 || index >= ROWS * COLS) return null;
        int[] pos = cellnumberToGridIndex(index);
        Cell[][] board = game.getBoard().getBoardCells();
        if (board == null || board[pos[0]] == null) return null;
        return board[pos[0]][pos[1]];
    }

    // ── Overlay / token accessors ──────────────────────────────────────────

    /** The overlay StackPane that holds the two player tokens. */
    public StackPane      getOverlayPane()    { return overlayPane; }
    public PlayerMonster  getPlayerToken()    { return playerToken; }
    public PlayerMonster  getOpponentToken()  { return opponentToken; }

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

        // Place tokens — must wait for layout to compute bounds
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
        PlayerMonster token = new PlayerMonster(m.getName(),
                                                CELL_SIZE * 0.85,
                                                resolveMonsterColor(m));
        // Badge label — stored in PlayerMonster so it survives sprite swaps
        token.setBadge(badge, badgeColor);
        return token;
    }

    public void applyStatusToToken(PlayerMonster token, Monster monster) {
        if (token == null || monster == null) return;
        MonsterGUI gui = token.getPlayer();
        if (gui == null) return;

        gui.monSet(); // reset to clean baseline

        if      (monster.isFrozen())   gui.coldFreeze(monster.isShielded());
        else if (monster.isShielded()) gui.shield(true);
        else if (monster.isConfused()) gui.confuse();
    }

    // ── Internal: path builder ─────────────────────────────────────────────

    /**
     * Returns an ordered list of StackPanes from the step after fromCell
     * to toCell (inclusive).  Handles forward and backwards movement.
     * All indices are clamped to [0, 99].
     */
    private List<StackPane> buildPathPanes(int fromCell, int toCell) {
        List<StackPane> path = new ArrayList<>();
        if (fromCell == toCell) return path;

        int size = ROWS * COLS;
        // Clamp inputs
        fromCell = Math.max(0, Math.min(size - 1, fromCell));
        toCell   = Math.max(0, Math.min(size - 1, toCell));

        if (fromCell == toCell) return path;

        boolean forward = toCell > fromCell;
        int current = fromCell;
        int maxSteps = 24; // safety cap
        while (current != toCell && maxSteps-- > 0) {
            current = forward ? current + 1 : current - 1;
            if (current < 0 || current >= size) break; // boundary guard
            if (cellPanes[current] != null) path.add(cellPanes[current]);
        }
        return path;
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

        // Conveyor tooltip
        if (contains(Constants.CONVEYOR_CELL_INDICES, cellNumber)) {
            if (cell instanceof ConveyorBelt) {
                int offset = ((ConveyorBelt) cell).getEffect();
                int dest = cellNumber + offset;
                Tooltip.install(pane, new Tooltip("Conveyor Belt \u2699\nMoves you " + offset + " cells to Cell " + dest));
            } else {
                Tooltip.install(pane, new Tooltip("Conveyor Belt \u2699"));
            }
        }

        // Sock tooltip
        if (contains(Constants.SOCK_CELL_INDICES, cellNumber)) {
            if (cell instanceof ContaminationSock) {
                int offset = ((ContaminationSock) cell).getEffect();
                int dest = cellNumber + offset;
                Tooltip.install(pane, new Tooltip("Contamination Sock \uD83E\uDDE6\nSlips you " + offset + " cells to Cell " + dest));
            }
        }

        // Layer 1: background image
        String imgFile = getCellImageFilename(cellNumber, cell);
        if (imgFile != null) {
            ImageView bg = loadCellImage(imgFile);
            if (bg != null) { StackPane.setAlignment(bg, Pos.CENTER); pane.getChildren().add(bg); }
        }

        // Layer 2: coord label
        Label coord = new Label(String.valueOf(cellNumber));
        coord.getStyleClass().add("coord-label");
        StackPane.setAlignment(coord, Pos.TOP_LEFT);
        pane.getChildren().add(coord);

        // Layer 3: icon (only when no background image)
        String icon = getCellIcon(cellNumber, cell);
        if (icon != null && imgFile == null) {
            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("cell-icon-label");
            StackPane.setAlignment(iconLabel, Pos.CENTER);
            pane.getChildren().add(iconLabel);
        }

        // Layer 4: stationed NPC monster avatar
        if (cell instanceof MonsterCell) {
            MonsterCell mc = (MonsterCell) cell;
            Monster stationed = mc.getCellMonster();
            if (stationed != null) {
                String tc = resolveMonsterColor(stationed);
                MonsterGUI mg = new MonsterGUI("s_" + mc.getName(), CELL_SIZE, tc);
                if (stationed.getRole() == Role.LAUGHER) mg.laugherGUI(); else mg.scarerGUI();
                stationedMonsters.add(mg);
                BorderPane wrap = new BorderPane();
                wrap.setCenter(new StackPane(mg));
                pane.getChildren().add(wrap);
            }
        }

        // Click handler
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
        if (n == Constants.STARTING_POSITION)             return "\uD83C\uDFE0";
        if (n == Constants.WINNING_POSITION)              return "\uD83C\uDFC1";
        if (contains(Constants.MONSTER_CELL_INDICES,  n)) return "\u26A1";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n)) return "\u2192";
        if (contains(Constants.SOCK_CELL_INDICES,     n)) return "\uD83E\uDDE6";
        if (contains(Constants.CARD_CELL_INDICES,     n)) return "\uD83C\uDCCF";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            if (dc.isActivated()) return "\u274C";
            return dc.getRole() == Role.SCARER ? "\u26A1" : "\uD83D\uDE02";
        }
        return null;
    }

    private String getCellImageFilename(int n, Cell cell) {
        if (n == Constants.STARTING_POSITION)                          return "start.png";
        if (n == Constants.WINNING_POSITION)                           return "end.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n) && n != 66)  return "conveyor.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n) && n == 66)  return "conveyorleft.png";
        if (contains(Constants.SOCK_CELL_INDICES,     n))              return "sock.png";
        if (contains(Constants.CARD_CELL_INDICES,     n))              return "card.png";
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
