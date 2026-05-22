package game.view;

import game.engine.Constants;
import game.engine.Game;
import game.engine.Role;
import game.engine.cells.Cell;
import game.engine.cells.DoorCell;
import game.engine.cells.MonsterCell;
import game.engine.monsters.Dasher;
import game.engine.monsters.Dynamo;
import game.engine.monsters.Monster;
import game.engine.monsters.MultiTasker;
import game.view.style.monsters.MonsterGUI;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class BoardGridPane extends GridPane {

    private final int ROWS = Constants.BOARD_ROWS;
    private final int COLS = Constants.BOARD_COLS;

    private static final double CELL_SIZE = 80;
    ArrayList<MonsterGUI> stationedMonsters;
    private Game game;

    // Callback fired when the user clicks a cell: (cellNumber, cell)
    private BiConsumer<Integer, Cell> cellClickListener;

    public BoardGridPane(Game game) {
        this.game = game;
        this.stationedMonsters=new ArrayList<MonsterGUI>();
        setHgap(4);
        setVgap(4);
        setAlignment(Pos.CENTER);
        

        if (game == null || game.getBoard() == null) {
            System.out.println("ERROR: Game or Board is NULL");
            return;
        }

        drawBoard();
    }

    /** Register the callback that fires when a cell is clicked. */
    public void setCellClickListener(BiConsumer<Integer, Cell> listener) {
        this.cellClickListener = listener;
    }

    // ── Helpers ──────────────────────────────────────────────────

    private boolean contains(int[] arr, int n) {
        if (arr == null) return false;
        for (int v : arr) if (v == n) return true;
        return false;
    }

    public Cell getCellFromBoard(int index) {
        if (game == null || game.getBoard() == null) return null;
        int row = index / COLS;
        int col = index % COLS;
        if (row % 2 == 1) col = COLS - 1 - col;
        Cell[][] board = game.getBoard().getBoardCells();
        if (board == null || board[row] == null) return null;
        return board[row][col];
    }

    private int getCellNumber(int boardRow, int col) {
        int base = boardRow * COLS;
        return (boardRow % 2 == 0) ? base + col : base + (COLS - 1 - col);
    }

    // ── Cell type → CSS class ────────────────────────────────────
    private String getCellStyle(int n,Cell cell) {
        if (n == Constants.STARTING_POSITION)                 return "start-cell";
        if (n == Constants.WINNING_POSITION)                  return "end-cell";
        if (contains(Constants.MONSTER_CELL_INDICES,  n))     return "monster-cell";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n))     return "conveyor-cell";
        if (contains(Constants.SOCK_CELL_INDICES,     n))     return "sock-cell";
        if (contains(Constants.CARD_CELL_INDICES,     n))     return "card-cell";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            return dc.getRole() == Role.SCARER ? "scare-door-cell" : "laugh-door-cell";
        }
        return "normal-cell";
    }

    // ── Cell type → icon character drawn inside the cell ─────────
    private String getCellIcon(int n,Cell cell) {
        if (n == Constants.STARTING_POSITION)                 return "🏠";
        if (n == Constants.WINNING_POSITION)                  return "🏁";
        if (contains(Constants.MONSTER_CELL_INDICES,  n))     return "⚡";   // door icon
        if (contains(Constants.CONVEYOR_CELL_INDICES, n))     return "→";
        if (contains(Constants.SOCK_CELL_INDICES,     n))     return "🧦";
        if (contains(Constants.CARD_CELL_INDICES,     n))     return "🃏";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            if (dc.isActivated()) return "❌"; // Cross indicator if door is used/exhausted
            return dc.getRole() == Role.SCARER ? "⚡" : "😂"; 
        }
        return null;
    }

    // ── Cell type → background image file ────────────────────────
    private String getCellImageFilename(int n,Cell cell) {
        if (n == Constants.STARTING_POSITION)                 return "start.png";
        if (n == Constants.WINNING_POSITION)                  return "end.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n)&&n!=66)     return "conveyor.png";
        if (contains(Constants.CONVEYOR_CELL_INDICES, n)&&n==66)     return "conveyorleft.png";
        if (contains(Constants.SOCK_CELL_INDICES,     n))     return "sock.png";
        if (contains(Constants.CARD_CELL_INDICES,     n))     return "card.png";
        if (cell instanceof DoorCell) {
            DoorCell dc = (DoorCell) cell;
            if (dc.isActivated()) {
                return "door_exhausted.png"; // Renders used up door look
            }
            return dc.getRole() == Role.SCARER ? "scare_door.png" : "laugh_door.png";
        }
        return null;
    }

    private ImageView loadCellImage(String filename) {
        URL url = getClass().getResource("/assets/" + filename);
        if (url == null) return null;
        try {
            Image img = new Image(url.toExternalForm(), CELL_SIZE*2, CELL_SIZE, false, true);
            if (img.isError()) return null;
            ImageView iv = new ImageView(img);
            iv.setFitWidth(CELL_SIZE*2);
            iv.setFitHeight(CELL_SIZE);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Monster label (player/opponent token) ────────────────────
    private Label buildMonsterLabel(Cell cell, Monster player, Monster opponent) {
        if (cell == null || !cell.isOccupied()) return null;
        Monster m = cell.getMonster();
        if (m == null) return null;

        Label label = new Label();
        label.getStyleClass().add("monster-label");
        label.getStyleClass().add("monster-name-label");

        if (m == player) {
            label.setText("P1");
            label.getStyleClass().add("monster-player");
        } else if (m == opponent) {
            label.setText("P2");
            label.getStyleClass().add("monster-opponent");
        }

        StackPane.setAlignment(label, Pos.TOP_RIGHT);
        return label;
    }

    // ── Draw all cells ───────────────────────────────────────────
    private void drawBoard() {
        if (game == null || game.getBoard() == null) return;

        Monster player   = game.getPlayer();
        Monster opponent = game.getOpponent();

        for (int boardRow = ROWS-1; boardRow>=0; boardRow--) {
            int gridRow = (ROWS - 1) - boardRow;

            for (int col = COLS-1; col>=0; col--) {
                int  cellNumber = getCellNumber(boardRow, col);
                Cell cell       = getCellFromBoard(cellNumber);
                
                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(CELL_SIZE*2, CELL_SIZE);
                cellPane.setMinSize(CELL_SIZE, CELL_SIZE);
                cellPane.setMaxSize(CELL_SIZE*2, CELL_SIZE);
                cellPane.getStyleClass().add("board-cell");
                cellPane.getStyleClass().add(getCellStyle(cellNumber,cell));
                
                
                if (contains(Constants.CONVEYOR_CELL_INDICES, cellNumber)) {
                    // Cast to ConveyorBelt if your model cell stores destination data
                    if (cell instanceof game.engine.cells.ConveyorBelt) {
                        game.engine.cells.ConveyorBelt cb = (game.engine.cells.ConveyorBelt) cell;
                        
                        // Assuming your ConveyorBelt engine class has a target/destination getter (e.g., getTargetCell() or getTargetIndex())
                        // Adjust the method name below to match your exact backend ConveyorBelt class implementation!
                        int destination = cb.getEffect(); 
                        
                        Tooltip conveyorTooltip = new Tooltip("Conveyor Belt ⚙️\nSpeeds you up to Cell: " + destination);
                        conveyorTooltip.getStyleClass().add("conveyor-tooltip");
                        
                        // Setting a snappy show-delay makes the UX feel responsive and smooth
                        //conveyorTooltip.setShowDelay(Duration.millis(150));
                        Tooltip.install(cellPane, conveyorTooltip);
                    } else {
                        // Fallback generic tooltip if target lookup relies on index mapping
                        Tooltip conveyorTooltip = new Tooltip("Conveyor Belt ⚙️\nHover over to check track.");
                        Tooltip.install(cellPane, conveyorTooltip);
                    }
                }

                // ── Layer 1: background image ─────────────────────
                String imgFile = getCellImageFilename(cellNumber,cell);
                if (imgFile != null) {
                    ImageView bg = loadCellImage(imgFile);
                    if (bg != null) {
                        StackPane.setAlignment(bg, Pos.CENTER);
                        cellPane.getChildren().add(bg);
                    }
                }

                // ── Layer 2: cell number top-left ─────────────────
                Label coordLabel = new Label(String.valueOf(cellNumber));
                coordLabel.getStyleClass().add("coord-label");
                StackPane.setAlignment(coordLabel, Pos.TOP_LEFT);
                cellPane.getChildren().add(coordLabel);

                // ── Layer 3: special cell icon (center) ───────────
                String icon = getCellIcon(cellNumber,cell);
                if (icon != null && imgFile == null) {
                    // Only show icon if no image is covering it
                    Label iconLabel = new Label(icon);
                    iconLabel.getStyleClass().add("cell-icon-label");
                    StackPane.setAlignment(iconLabel, Pos.CENTER);
                    cellPane.getChildren().add(iconLabel);
                }

                // ── Layer 4: monster-cell stationed avatar ─────────
                if (cell instanceof MonsterCell) {
                    MonsterCell mc = (MonsterCell) cell;
                    Monster stationed = mc.getCellMonster();
                    if (stationed != null) {
                        // Show a compact icon box matching the reference image style
                    	StackPane stp=new StackPane();
                    	String name=mc.getName();
                    	String typeColor="";
                    	if(stationed instanceof Dynamo)typeColor="#4CA2F7";
                    	else if(stationed instanceof Dasher) typeColor="#97DE4A";
                    	else if(stationed instanceof MultiTasker) typeColor="#FF8C00";
                    	else typeColor="#C0C0C0";
                    	MonsterGUI monster= new MonsterGUI("s_"+name,CELL_SIZE, typeColor);
                    	if(stationed.getRole()==Role.LAUGHER)monster.laugherGUI();
                    	else monster.scarerGUI();
                    	this.stationedMonsters.add(monster);
                    	stp.getChildren().add(monster);
                        cellPane.getChildren().add(stp);
                    }
                }

                // ── Layer 5: active P1/P2 badge ───────────────────
                Label monsterLabel = buildMonsterLabel(cell, player, opponent);
                if (monsterLabel != null)
                    cellPane.getChildren().add(monsterLabel);

                // ── Click → update Stats panel ────────────────────
                
                
                final int cn   = cellNumber;
                final Cell cel = cell;
                cellPane.setOnMouseClicked(e -> {
                	
                    if (cellClickListener != null) {
                        cellClickListener.accept(cn, cel);
                    }
                    for (javafx.scene.Node node : getChildren()) {
                        if (node instanceof StackPane) {
                            node.getStyleClass().remove("cell-selected");
                        }
                    }
                    // Highlight selected
                    //cellPane.getStyleClass().remove("cell-selected");
                    cellPane.getStyleClass().add("cell-selected");
                });
                
                add(cellPane, col, gridRow);
            }
        }
        for (MonsterGUI monsterGUI : stationedMonsters) {
			monsterGUI.setInCell();
		}
    }

    private StackPane buildDoorIconBox(
            int cellNumber,
            Monster stationed
    ) {

        boolean isScareDoor =
                contains(
                        Constants.MONSTER_CELL_INDICES,
                        cellNumber
                );

        String boxStyle =
                isScareDoor
                        ? "door-box-scare"
                        : "door-box-laugh";

        StackPane box = new StackPane();

        // Bigger container
        box.setPrefSize(100, 100);

        box.setMinSize(78, 78);

        box.setMaxSize(78, 78);

        box.getStyleClass().add("door-box");

        box.getStyleClass().add(boxStyle);

        // Bigger avatar
        StackPane avatar =
                AvatarUtil.buildAvatar(stationed, 64);

        StackPane.setAlignment(
                avatar,
                Pos.CENTER
        );

        box.getChildren().add(avatar);

        return box;
    }

    public void refresh() {
        getChildren().clear();
        drawBoard();
    }
}