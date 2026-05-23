package game.view;

import game.controller.GamePlay;
import game.controller.Main;
import game.engine.Constants;
import game.engine.Game;
import game.engine.Role;
import game.engine.cards.Card;
import game.engine.cells.Cell;
import game.engine.cells.DoorCell;
import game.engine.cells.MonsterCell;
import game.engine.monsters.Monster;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;

public class BoardView {

        private final Stage stage;
        private final GamePlay controller; 
        private final Game game;             

        // ── Live state labels (States panel – right side) ──────────────
        // ── Live state layouts (Right side panel) ──────────────────────
        private VBox energySectionContainer; // Wraps title, pct, and progress bar together
        private Label statsEnergyValueLabel;
        private ProgressBar statsEnergyBar;
        private Label statsStatusLabel;
        private Label statsPositionValueLabel;
        private Label statsCellTypeLabel;
        private ImageView statsCellImageView; // 🆕 Track the cell preview image container
        private Label deckCountLabel; // 🆕 Track the live deck size text display
        private ImageView lastDrawnCardFaceView;
        private VBox lastDrawnContainer;


        // ── Player UI Cards (Bottom Left) ────────────────────────────
        private Label playerNameLabel;
        private Label playerRoleTagLabel;
        private Label playerTurnStatusLabel;
        private Label playerStatusConditionLabel; // 🆕 Permanent dynamic condition tag
        private Label playerEnergyValueLabel;
        private Label oppEnergyValueLabel;
        //private ImageView playerCanister;
        private ProgressBar playerEnergyCanister;
        private ProgressBar oppEnergyCanister;
        // ── Opponent UI Cards (Bottom Right) ──────────────────────────
        private Label opponentNameLabel;
        private Label opponentRoleTagLabel;
        private Label opponentTurnStatusLabel;
        private Label opponentStatusConditionLabel; // 🆕 Permanent dynamic condition tag
        //private ImageView oppCanister;
        // ── Mid-control Panels ────────────────────────────────────────
        private Label turnLabel;
        private BoardGridPane board;
        

		private DiceView diceView;
        private HBox centerControlsRow;
        VBox centerBox;

        public BoardView(Stage stage, GamePlay controller, Game game) {
                this.stage = stage;
                this.controller = controller;
                this.game = game;
                this.stage.setFullScreen(true);
        }
        public BoardGridPane getBoard() {
			return board;
		}
        public void show() {
                if (game == null || game.getBoard() == null)
                        throw new IllegalStateException("Game model not ready for presentation mapping");

                board = new BoardGridPane(game);

                // UI-bound display selection updates stay local to View presentation mechanics
                board.setCellClickListener((cellNumber, cell) -> updateStatsPanel(cellNumber, cell, game));

                BorderPane statsPanel = buildStatsPanel(game);
                HBox bottomBar = buildBottomBar(game);

                BorderPane root = new BorderPane();
                root.getStyleClass().add("game-root");

                // ── CRITICAL: stack the grid and its overlay together so tokens are visible ──
                // The overlayPane holds the animated PlayerMonster tokens and must sit
                // directly on top of the GridPane for coordinate calculations to work.
                StackPane boardWrapper = new StackPane(board, board.getOverlayPane());
                root.setCenter(boardWrapper);
                root.setRight(statsPanel);
                root.setBottom(bottomBar);

                BorderPane.setMargin(boardWrapper, new Insets(12, 0, 0, 12));
                BorderPane.setMargin(statsPanel, new Insets(0));

                Scene scene = new Scene(root, 1440, 900);
                scene.getStylesheets().add(getClass().getResource("/game/view/css/monster.css").toExternalForm());
                URL css = getClass().getResource("/game/view/css/board.css");
                
                if (css != null) {
                        scene.getStylesheets().add(css.toExternalForm());
                } else {
                        System.out.println("board.css NOT FOUND");
                }

                stage.setTitle("Monsters, Inc. — Door Dash");
                stage.setMinWidth(1300);
                stage.setMinHeight(820);
                stage.setScene(scene);
                stage.show();
        }

        /**
         * Unified programmatic presentation refresh. 
         * Called exclusively by the controller when backend state alterations occur.
         */
        public void refreshAllViewComponents() {
                board.refresh();

                Monster currentTurnMonster = game.getCurrent();
                
                // ── 🆕 Synchronize the deck capacity layout label string ──
                                if (deckCountLabel != null && game.getBoard().getCards() != null) {
                                        deckCountLabel.setText("CARDS LEFT: " + game.getBoard().getCards().size());
                                }

                // Reset Right Stats summary panel back to tracking the active moving monster
                updateStatsPanelForMonster(currentTurnMonster, null, game);

                Cell currentCell = board.getCellFromBoard(currentTurnMonster.getPosition());
                // 🆕 Synchronize side graphic back to the active position asset
                updateStatsPanel(currentTurnMonster.getPosition(), currentCell, game);
                statsCellTypeLabel.setText(resolveCellTypeName(currentTurnMonster.getPosition(), currentCell));
                refreshBottomCards(game);

                int lastRollResult = game.getLastDiceRoll();
                //refreshDice(lastRollResult);
                diceView.setDiceValue(lastRollResult);

                turnLabel.setText("🎯 " + currentTurnMonster.getName() + "'s turn");
        }

        private BorderPane buildStatsPanel(Game game) {
                // ── 1. Create the Main Wrapper Panel ──
        BorderPane panel = new BorderPane();
        panel.getStyleClass().add("stats-panel");
        panel.setPrefWidth(220);
        panel.setMinWidth(200);
        panel.setMaxWidth(220);
        panel.setPadding(new Insets(24, 15, 24, 15)); // Snug padding to protect horizontal spacing

                Label header = new Label("Stats");
                header.getStyleClass().add("stats-header");

                Label energyTitle = new Label("Energy");
                energyTitle.getStyleClass().add("stats-section-label");

                Monster current = game.getCurrent();
                double energyPct = clampEnergy(current);

                statsEnergyBar = new ProgressBar(energyPct/1000);
                statsEnergyBar.getStyleClass().add("energy-bar");
                statsEnergyBar.setMaxWidth(Double.MAX_VALUE);

                statsEnergyValueLabel = new Label(formatEnergyPct(energyPct));
                statsEnergyValueLabel.getStyleClass().add("energy-pct-label");

                HBox energyRow = new HBox(8, energyTitle, statsEnergyValueLabel);
                energyRow.setAlignment(Pos.CENTER_LEFT);

                energySectionContainer = new VBox(8, energyRow, statsEnergyBar);

                statsStatusLabel = new Label("● ACTIVE");
                statsStatusLabel.getStyleClass().add("status-badge-active");

                Label posTitle = new Label("Position");
                posTitle.getStyleClass().add("stats-card-title");

                statsPositionValueLabel = new Label(String.valueOf(current.getPosition()));
                statsPositionValueLabel.getStyleClass().add("stats-card-value");

                VBox posCard = new VBox(4, posTitle, statsPositionValueLabel);
                posCard.getStyleClass().add("stats-card");
                posCard.setAlignment(Pos.CENTER);

                Label cellTypeTitle = new Label("Cell Type");
                cellTypeTitle.getStyleClass().add("stats-card-title");

                statsCellTypeLabel = new Label("—");
                statsCellTypeLabel.getStyleClass().add("stats-card-value-small");

                VBox cellCard = new VBox(4, cellTypeTitle, statsCellTypeLabel);
                cellCard.getStyleClass().add("stats-card");
                cellCard.setAlignment(Pos.CENTER);
                
                // ── 2. Cell Image Preview Component ──
                statsCellImageView = new ImageView();
                statsCellImageView.setFitWidth(180); 
                statsCellImageView.setPreserveRatio(true);
                statsCellImageView.setSmooth(true);
                statsCellImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        // ── 3. Fixed Deck & Last Played Stack Area ──
        ImageView deckImageView = new ImageView();
        // Downscale slightly to ensure it stays perfectly balanced inside the 220px bounds when rotated
        deckImageView.setFitWidth(300);          
        deckImageView.setFitHeight(200);        
        deckImageView.setPreserveRatio(true);    
        deckImageView.setSmooth(true);           
        deckImageView.setRotate(-90);

        try {
            URL deckUrl = getClass().getResource("/assets/deck.png");
            if (deckUrl != null) {
                deckImageView.setImage(new Image(deckUrl.toExternalForm()));
            }
        } catch (Exception e) {
            System.out.println("Could not load deck asset: " + e.getMessage());
        }

        // Overlapping Last Played Card Placement Slot
        lastDrawnCardFaceView = new ImageView();
        lastDrawnCardFaceView.setFitWidth(110); 
        lastDrawnCardFaceView.setPreserveRatio(true);
        lastDrawnCardFaceView.setSmooth(true);
        lastDrawnCardFaceView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.7), 10, 0, 0, 4);");

        Label previewBadge = new Label("LAST PLAYED");
        previewBadge.setStyle("-fx-text-fill: #e9b949; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(11,26,48,0.85); -fx-padding: 2 6; -fx-background-radius: 4;");

        lastDrawnContainer = new VBox(4, previewBadge, lastDrawnCardFaceView);
        lastDrawnContainer.setAlignment(Pos.CENTER);
        lastDrawnContainer.setVisible(false); 
        lastDrawnContainer.setMouseTransparent(true); 

        // Wrap rotated deck view inside a Group container 
        javafx.scene.Group rotatedGroup = new javafx.scene.Group(deckImageView);
        
        // Fix vertical margins and alignment by centering tightly inside a constrained layout panel
        StackPane deckCenterer = new StackPane(rotatedGroup, lastDrawnContainer);
        deckCenterer.setAlignment(Pos.CENTER);
        deckCenterer.setPrefHeight(210); // Hard caps the height to kill empty top/bottom margins completely
        deckCenterer.setMinHeight(210);
        deckCenterer.setMaxHeight(210);

        deckCountLabel = new Label("CARDS LEFT: " + game.getBoard().getCards().size());
        deckCountLabel.getStyleClass().add("stats-card-title"); 
        deckCountLabel.setStyle("-fx-text-fill: white;");

        // Vertical packaging layout widget box specifically for the card pile elements
        VBox deckWrapper = new VBox(5, deckCenterer, deckCountLabel);
        deckWrapper.setAlignment(Pos.CENTER);

        // ── 4. Unified VBox Container ──
        VBox columnStack = new VBox(14);
        columnStack.setAlignment(Pos.TOP_CENTER);
        columnStack.getChildren().addAll(
            header, 
            statsCellImageView, 
            energySectionContainer, 
            statsStatusLabel, 
            posCard, 
            cellCard,
            deckWrapper
        );

        // Wrap inside a ScrollPane container bound cleanly so layout boundaries remain stable
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(columnStack);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        panel.setCenter(scrollPane);
        return panel;
    }
        
        // ── 🆕 Method to paint the face-up card overlay graphic above the pile ──
                public void updateLastDrawnCardSlot(Card card) {
                        if (card == null) {
                                lastDrawnContainer.setVisible(false);
                                return;
                        }

                        String lookupName = "swap"; // Default fallback graphic identifier

                        // Class evaluation via instanceof matching your Milestone compilation architecture
                        if (card instanceof game.engine.cards.SwapperCard) {
                                lookupName = "swap";
                        } else if (card instanceof game.engine.cards.ShieldCard) {
                                lookupName = "shield";
                        } else if (card instanceof game.engine.cards.EnergyStealCard) {
                                lookupName = "energysteal";
                        } else if (card instanceof game.engine.cards.StartOverCard) {
                                lookupName = "startover";
                        } else if (card instanceof game.engine.cards.ConfusionCard) {
                                lookupName = "confusion";
                        }

                        try {
                                // Pulls safely from your /assets/ folder
                                String imagePath = "/assets/" + lookupName + ".png";
                                Image cardImage = new Image(getClass().getResourceAsStream(imagePath));
                                lastDrawnCardFaceView.setImage(cardImage);
                                lastDrawnContainer.setVisible(true); // Unhide layer to show the face-up artwork layout slot
                        } catch (Exception e) {
                                System.out.println("⚠️ Could not render face-up deck card artwork for asset filename: " + lookupName);
                        }
                }
        ////end adels

                //Label legendTitle = new Label("LEGEND");
                //legendTitle.getStyleClass().add("legend-title");
                ///no need for this part 
                //        VBox legend = new VBox(8,
                //                legendTitle,
                //                legendRow("#f5c542", "Laugh Door"),
                //                legendRow("#9b59b6", "Scare Door"),
                //                legendRow("#2ecc71", "Conveyor"),
                //                legendRow("#e67e22", "Hazard")
                //        );
                //        legend.getStyleClass().add("legend-box");

                // Assemble right side stack
//              VBox panel = new VBox(16, header, energySectionContainer, statsStatusLabel, posCard, cellCard);
//              panel.getStyleClass().add("stats-panel");
//              panel.setPrefWidth(220);
//              panel.setMinWidth(200);
//              panel.setPadding(new Insets(24, 20, 24, 20));
//
//              return panel;
//      }

        //    private HBox legendRow(String hex, String text) {
        //        Circle dot = new Circle(7, Color.web(hex));
        //        Label lbl = new Label(text);
        //        lbl.getStyleClass().add("legend-item-label");
        //        HBox row = new HBox(10, dot, lbl);
        //        row.setAlignment(Pos.CENTER_LEFT);
        //        return row;
        //    }

                private HBox buildBottomBar(Game game) {
                        // ── Player 1 View Construction Setup ──
                        StackPane playerAvatar = AvatarUtil.buildAvatar(game.getPlayer(), 70);
                        Label p1Badge = new Label("P1");
                        p1Badge.getStyleClass().add("player-badge-p1");
                        StackPane.setAlignment(p1Badge, Pos.TOP_LEFT);
                        playerAvatar.getChildren().add(p1Badge);

                        playerNameLabel = new Label(game.getPlayer().getName());
                        playerNameLabel.getStyleClass().add("bottom-player-name");

                        playerRoleTagLabel = new Label(game.getPlayer().getRole() + "  A");
                        playerRoleTagLabel.getStyleClass().add("bottom-role-tag");

                        playerTurnStatusLabel = new Label("Turn Active");
                        playerTurnStatusLabel.getStyleClass().add("bottom-turn-active");

                        // 🆕 Setup status label for P1 card
                        playerStatusConditionLabel = new Label("● NORMAL");
                        playerStatusConditionLabel.getStyleClass().add("bottom-condition-normal");

                        VBox playerInfo = new VBox(2, playerNameLabel, playerRoleTagLabel, playerTurnStatusLabel, playerStatusConditionLabel);
                        playerInfo.setAlignment(Pos.CENTER_LEFT);
                        playerEnergyCanister = new ProgressBar(1.0);
                        StackPane p1CanisterLayout = createScreamCanisterView(playerEnergyCanister);
                        playerEnergyValueLabel = new Label("1000 / 1000 ⚡");
                        playerEnergyValueLabel.setStyle("-fx-text-fill: #00ff99; -fx-font-weight: bold; -fx-font-size: 13px;");
                        //playerEnergyCanister.setProgress(1.0);
                        //playerEnergyCanister.setPrefWidth(120);
                        playerEnergyCanister.getStyleClass().add("scream-canister-bar");
                        playerEnergyCanister.setPrefWidth(140);
                        playerEnergyCanister.setPrefHeight(18);
                        playerEnergyCanister.setStyle(
                            "-fx-accent: #00ffcc;"
                        );
                        VBox p1CanisterGroup = new VBox(4, p1CanisterLayout, playerEnergyValueLabel);
                        p1CanisterGroup.setAlignment(Pos.CENTER);

                        HBox playerCard = new HBox(12, playerAvatar, playerInfo,p1CanisterGroup);
                        playerCard.setAlignment(Pos.CENTER_LEFT);
                        playerCard.getStyleClass().add("bottom-player-card");
                        playerCard.setPadding(new Insets(10, 20, 10, 16));
                        HBox.setHgrow(playerCard, Priority.ALWAYS);



                        // ── Turn Management Setup ──
                        diceView = new DiceView(); // 🆕 Simply instantiate the custom view
                        turnLabel = new Label("🎯 " + game.getCurrent().getName() + "'s turn");
                        turnLabel.getStyleClass().add("bottom-turn-label");

                        //Button rollBtn = new Button("ROLL DICE");
                        // ... (styling stays the same)
                        diceView.setOnMouseClicked(e -> controller.handleRollDiceAction());

                        // ⚡ Power Up Button Setup
                        Button powerupBtn = new Button("USE POWERUP");
                        powerupBtn.getStyleClass().add("powerup-button");
                        powerupBtn.setPrefWidth(120);
                        powerupBtn.setPrefHeight(42);
                        powerupBtn.setOnAction(e -> controller.handleUsePowerupAction());

                        // 🆕 Side-by-Side Row: Packs the dice face image and both buttons together horizontally
                        centerControlsRow = new HBox(14, diceView, powerupBtn);
                        centerControlsRow.setAlignment(Pos.CENTER);

                        // Stack the horizontal controls row directly on top of the text label
                         centerBox = new VBox(8, centerControlsRow, turnLabel);
                        centerBox.setAlignment(Pos.CENTER);
                        centerBox.setPadding(new Insets(8, 24, 8, 24));

                        // ── Player 2 View Construction Setup ──
                        StackPane oppAvatar = AvatarUtil.buildAvatar(game.getOpponent(), 70);
                        Label p2Badge = new Label("P2");
                        p2Badge.getStyleClass().add("player-badge-p2");
                        StackPane.setAlignment(p2Badge, Pos.TOP_RIGHT);
                        oppAvatar.getChildren().add(p2Badge);

                        opponentNameLabel = new Label(game.getOpponent().getName());
                        opponentNameLabel.getStyleClass().add("bottom-player-name");

                        opponentRoleTagLabel = new Label(game.getOpponent().getRole() + "  B");
                        opponentRoleTagLabel.getStyleClass().add("bottom-role-tag");

                        opponentTurnStatusLabel = new Label("Waiting...");
                        opponentTurnStatusLabel.getStyleClass().add("bottom-turn-waiting");

                        // 🆕 Setup status label for P2 card
                        opponentStatusConditionLabel = new Label("● NORMAL");
                        opponentStatusConditionLabel.getStyleClass().add("bottom-condition-normal");

                        VBox oppInfo = new VBox(2, opponentNameLabel, opponentRoleTagLabel, opponentTurnStatusLabel, opponentStatusConditionLabel);
                        oppInfo.setAlignment(Pos.CENTER_RIGHT);
                        oppEnergyCanister = new ProgressBar(1.0);
                        StackPane p2CanisterLayout = createScreamCanisterView(oppEnergyCanister);
                        oppEnergyValueLabel = new Label("1000 / 1000 ⚡");
                        oppEnergyValueLabel.setStyle("-fx-text-fill: #00ff99; -fx-font-weight: bold; -fx-font-size: 13px;");

                        // 🆕 Wrap canister layout and value text vertically together
                        VBox p2CanisterGroup = new VBox(4, p2CanisterLayout, oppEnergyValueLabel);
                        p2CanisterGroup.setAlignment(Pos.CENTER);
                        //oppEnergyCanister.setProgress(1.0);
                        oppEnergyCanister.getStyleClass().add("scream-canister-bar");
                        oppEnergyCanister.setPrefWidth(120);

                        oppEnergyCanister.setPrefWidth(140);
                        oppEnergyCanister.setPrefHeight(18);
                        
                        oppEnergyCanister.setStyle(
                            "-fx-accent: #00ffcc;"
                        );
                        
                        HBox oppCard = new HBox(12,p2CanisterGroup, oppInfo, oppAvatar);
                        oppCard.setAlignment(Pos.CENTER_RIGHT);
                        oppCard.getStyleClass().add("bottom-opponent-card");
                        oppCard.setPadding(new Insets(10, 16, 10, 20));
                        HBox.setHgrow(oppCard, Priority.ALWAYS);

                        HBox bar = new HBox(0, playerCard, centerBox, oppCard);
                        bar.setAlignment(Pos.CENTER);
                        bar.getStyleClass().add("bottom-bar");
                        bar.setPrefHeight(100);

                        // Load the initial static values for the bottom bars
                        refreshBottomCards(game);

                        return bar;
                }
        ///this is callled in the controller
        public DiceView getDiceView() {
                return this.diceView;
        }



        // ── Cell Click Presentation Manager ─────────────────────────
        private void updateStatsPanel(int cellNumber, Cell cell, Game game) {
                Monster m = null;
                
                
                // ── 🆕 Update Dynamic Cell Preview Graphic ──
                                String imgFile = null;
                                if (cellNumber == Constants.STARTING_POSITION)                 imgFile = "start.png";
                                else if (cellNumber == Constants.WINNING_POSITION)             imgFile = "end.png";
                                else if (contains(Constants.CONVEYOR_CELL_INDICES, cellNumber))imgFile = (cellNumber == 66) ? "conveyorleft.png" : "conveyor.png";
                                else if (contains(Constants.SOCK_CELL_INDICES,     cellNumber))imgFile = "sock.png";
                                else if (contains(Constants.CARD_CELL_INDICES,     cellNumber))imgFile = "card.png";
                                else if (cell instanceof DoorCell) {
                                        DoorCell dc = (DoorCell) cell;
                                        imgFile = dc.isActivated() ? "door_exhausted.png" : (dc.getRole() == Role.SCARER ? "scare_door.png" : "laugh_door.png");
                                }

                                if (imgFile != null) {
                                        try {
                                                URL imgUrl = getClass().getResource("/assets/" + imgFile);
                                                if (imgUrl != null) {
                                                        statsCellImageView.setImage(new Image(imgUrl.toExternalForm()));
                                                        statsCellImageView.setVisible(true);
                                                        statsCellImageView.setManaged(true);
                                                }
                                        } catch (Exception e) {
                                                System.out.println("Could not resolve cell image preview: " + e.getMessage());
                                        }
                                } else {
                                        // Clear image if clicking a normal cell grid space that doesn't hold unique asset files
                                        statsCellImageView.setImage(null);
                                }
                // Check if an active moving character is standing on the clicked cell
                if (cell != null && cell.isOccupied()) {
                        m = cell.getMonster();
                } 
                // Or check if it's a fixed station monster inside a door cell
                else if (cell instanceof MonsterCell) {
                        m = ((MonsterCell) cell).getCellMonster();
                }

                // 1. Update the Cell Type Label dynamically (will now show Door details)
                statsCellTypeLabel.setText(resolveCellTypeName(cellNumber, cell));

                if (m != null) {
                        // ─── SHOW MONSTER ENERGY & STATUS ───
                        if (!energySectionContainer.isVisible()) {
                                energySectionContainer.setManaged(true);
                                energySectionContainer.setVisible(true);
                                statsStatusLabel.setVisible(true);

                                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                                                javafx.util.Duration.millis(250), energySectionContainer
                                                );
                                fadeIn.setFromValue(0.0);
                                fadeIn.setToValue(1.0);
                                fadeIn.play();
                        }
                        updateStatsPanelForMonster(m, cell, game);

                } else if (cell instanceof DoorCell) {

                    DoorCell dc = (DoorCell) cell;

                    // ─── SHOW ENERGY SECTION FOR DOORS TOO ───
                    if (!energySectionContainer.isVisible()) {

                        energySectionContainer.setManaged(true);
                        energySectionContainer.setVisible(true);
                        statsStatusLabel.setVisible(true);

                        javafx.animation.FadeTransition fadeIn =
                                new javafx.animation.FadeTransition(
                                        javafx.util.Duration.millis(250),
                                        energySectionContainer
                                );

                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    }

                    // Door energy display
                    double pct = dc.getEnergy();

                    statsEnergyBar.setProgress(Math.max(0, pct / 1000.0));
                    statsEnergyValueLabel.setText(String.valueOf((int) pct));

                    statsPositionValueLabel.setText(String.valueOf(cellNumber));

                    // Door status
                    if (dc.isActivated()) {

                        statsStatusLabel.setText("❌ EXHAUSTED");
                        statsStatusLabel.getStyleClass().setAll("status-badge-frozen");
                        

                    } else {

                        statsStatusLabel.setText("🚪 DOOR ACTIVE");
                        statsStatusLabel.getStyleClass().setAll("status-badge-active");
                    }
                }else {
                        // ─── STANDARD EMPTY TILE (SHIFT UP / HIDE ENERGY & STATUS) ───
                        if (energySectionContainer.isVisible() || statsStatusLabel.isVisible()) {
                                statsStatusLabel.setVisible(false);
                                statsPositionValueLabel.setText(String.valueOf(cellNumber));

                                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                                                javafx.util.Duration.millis(200), energySectionContainer
                                                );
                                fadeOut.setFromValue(1.0);
                                fadeOut.setToValue(0.0);

                                fadeOut.setOnFinished(event -> {
                                        energySectionContainer.setVisible(false);
                                        energySectionContainer.setManaged(false); 
                                });
                                fadeOut.play();
                        } else {
                                statsPositionValueLabel.setText(String.valueOf(cellNumber));
                        }
                }
        }
        private double clampEnergy(Monster m) {
                double max = 1000.0;
                double pct = m.getEnergy();
                return Math.max(0, pct);
        }

        private String formatEnergyPct(double pct) {
                return (int) Math.round(pct * 1.00) + "";
        }
        private void updateStatsPanelForMonster(Monster m, Cell cell, Game game) {
                if (m == null) return;

                double pct = clampEnergy(m);
                statsEnergyBar.setProgress(Math.max(0, pct / 1000.0));
                statsEnergyValueLabel.setText(formatEnergyPct(pct));
                statsPositionValueLabel.setText(String.valueOf(m.getPosition()));

                // Assign correct text style classes to side status badge
                if (m.isFrozen()) {
                        statsStatusLabel.setText("❄ FROZEN - 1 turn");
                        statsStatusLabel.getStyleClass().setAll("status-badge-frozen");
                } else if (m.isShielded()) {
                        statsStatusLabel.setText("🛡 SHIELDED");
                        statsStatusLabel.getStyleClass().setAll("status-badge-shielded");
                } else if (m.isConfused()) {
                        statsStatusLabel.setText("😵 CONFUSED");
                        statsStatusLabel.getStyleClass().setAll("status-badge-confused");
                } else {
                        statsStatusLabel.setText("● ACTIVE");
                        statsStatusLabel.getStyleClass().setAll("status-badge-active");
                }
        }

        private void refreshBottomCards(Game game) {
                
                Monster cur = game.getCurrent();
                Monster p1 = game.getPlayer();
                Monster p2 = game.getOpponent();

                // 1. Update Turn Status labels
                playerTurnStatusLabel.setText(p1 == cur ? "Turn Active" : "Waiting...");
                playerTurnStatusLabel.getStyleClass().setAll(p1 == cur ? "bottom-turn-active" : "bottom-turn-waiting");

                opponentTurnStatusLabel.setText(p2 == cur ? "Turn Active" : "Waiting...");
                opponentTurnStatusLabel.getStyleClass().setAll(p2 == cur ? "bottom-turn-active" : "bottom-turn-waiting");
 
                
                PauseTransition delay = new PauseTransition(Duration.seconds(0.2));

                delay.setOnFinished(e -> {

                    if (game.getCurrent() == game.getPlayer()) {

                        centerBox.setPadding(new Insets(8, 600, 8, 0));

                    } else {

                        centerBox.setPadding(new Insets(8, 0, 8, 600));
                    }
                });

                delay.play();
                String p1MonsterClass = p1.getClass().getSimpleName(); // Drops "Dasher", "MultiTasker", etc.
            playerRoleTagLabel.setText(p1.getRole() + " — " + p1MonsterClass + "  A");

            String p2MonsterClass = p2.getClass().getSimpleName();
            opponentRoleTagLabel.setText(p2.getRole() + " — " + p2MonsterClass + "  B");

            // 2. Update Permanent Dynamic Condition for Player 1
            updatePlayerConditionLabel(p1, playerStatusConditionLabel);

            // 3. Update Permanent Dynamic Condition for Player 2
            updatePlayerConditionLabel(p2, opponentStatusConditionLabel);
           
                
                // 2. 🆕 Update Permanent Dynamic Condition for Player 1
                
                updateCanisterIcon(p1, playerEnergyCanister);
                updateCanisterIcon(p2, oppEnergyCanister);
                playerEnergyValueLabel.setText("Energy : "+(int)p1.getEnergy() + " ⚡");
                oppEnergyValueLabel.setText("Energy : "+(int)p2.getEnergy() + " ⚡");
                
        }
        
        private StackPane createScreamCanisterView(ProgressBar progressBar) {
            StackPane container = new StackPane();
            container.setPrefSize(160, 60);
            container.setMaxSize(160, 60);

            // 1. Configure the progress bar to sit perfectly inside the glass chamber
            progressBar.getStyleClass().add("scream-canister-bar");
            progressBar.setPrefWidth(160);
            progressBar.setPrefHeight(90);
            
            // Insets mimic the CSS margins programmatically to keep the fluid inside the caps
            progressBar.setPadding(new Insets(20, 22, 10, 22)); 

            // 2. Load the canister frame mask image using Java's absolute resource path
            ImageView canisterFrame = new ImageView();
            try {
                // This looks directly inside your src/assets/ folder at runtime
                URL imgUrl = getClass().getResource("/assets/canister.png"); 
                if (imgUrl != null) {
                    canisterFrame.setImage(new Image(imgUrl.toExternalForm()));
                } else {
                    System.err.println("⚠️ Could not find canister.png in assets folder!");
                }
            } catch (Exception e) {
                System.err.println("Error loading canister image: " + e.getMessage());
            }
            
            canisterFrame.setFitWidth(160);
            canisterFrame.setFitHeight(60);
            canisterFrame.setPreserveRatio(false);
            canisterFrame.setSmooth(true);
            canisterFrame.setPickOnBounds(false); // Allows clicks to pass through transparent bits

            // Layer them up: Progress bar fill stands underneath, canister outline overlays on top
            container.getChildren().addAll(progressBar, canisterFrame);
            return container;
        }

        // 🆕 Helper method to sync condition texts and styles onto specific player badges
        private void updatePlayerConditionLabel(Monster player, Label statusLabel) {
            if (statusLabel == null || player == null) return;

            if (player.isFrozen()) {
                statusLabel.setText("❄ FROZEN");
                statusLabel.getStyleClass().setAll("bottom-condition-frozen");
            } else if (player.isShielded()) {
                statusLabel.setText("🛡 SHIELDED");
                statusLabel.getStyleClass().setAll("bottom-condition-shielded");
            } else if (player.isConfused()) {
                // ✅ FIX: Read the exact confusion turns count from your milestone logic
                int turnsLeft = player.getConfusionTurns(); 
                statusLabel.setText("😵 CONFUSED (" + turnsLeft + ")");
                statusLabel.getStyleClass().setAll("bottom-condition-confused");
            } else {
                statusLabel.setText("● NORMAL");
                statusLabel.getStyleClass().setAll("bottom-condition-normal");
            }
        }

        private String resolveCellTypeName(int cellNumber, Cell cell) {
                if (cellNumber == Constants.STARTING_POSITION) return "Start";
                if (cellNumber == Constants.WINNING_POSITION) return "Finish 🏁";

                // ── Now clean, readable, and error-free! ──
                if (cell instanceof DoorCell) {
                        DoorCell dc = (DoorCell) cell;
                        return (dc.getRole() == Role.SCARER) ? "Scare Door ⚡\n"+dc.getEnergy() : "Laugh Door 😂 \n"+dc.getEnergy();
                }

                if (contains(Constants.MONSTER_CELL_INDICES, cellNumber)) return "Monster Cell 👾";
                if (contains(Constants.CONVEYOR_CELL_INDICES, cellNumber)) return "Conveyor ⚙️";
                if (contains(Constants.SOCK_CELL_INDICES, cellNumber)) return "Sock 🧦";
                if (contains(Constants.CARD_CELL_INDICES, cellNumber)) return "Card 🃏";
                return "Normal Cell";
        }

        private boolean contains(int[] arr, int n) {
                if (arr == null) return false;
                for (int v : arr) if (v == n) return true;
                return false;
        }
        // too check energy level ranges and selectr the correct canister image
        private void updateCanisterIcon(Monster m, ProgressBar bar) {
            if (m == null || bar == null) return;

            // 1. Calculate progress fraction (0.0 to 1.0)
            double progress = Math.max(0, m.getEnergy() / 1000.0);
            bar.setProgress(progress);

            // 2. Safe check for the internal sub-component
            javafx.scene.Node innerBarFill = bar.lookup(".bar");

            // Define our dynamic colors
            String colorStyle;
            if (progress > 0.65) {
                colorStyle = "-fx-background-color: linear-gradient(to right, rgba(0, 255, 153, 0.8), rgba(0, 255, 204, 0.6));";
            } else if (progress > 0.25) {
                colorStyle = "-fx-background-color: linear-gradient(to right, rgba(255, 214, 51, 0.8), rgba(255, 170, 0, 0.6));";
            } else {
                colorStyle = "-fx-background-color: linear-gradient(to right, rgba(255, 68, 68, 0.8), rgba(204, 0, 0, 0.6));";
            }

            // 3. 🌟 THE SAFETY CHECK SHIELD 🌟
            if (innerBarFill != null) {
                // If it's already rendered on screen, color the liquid directly
                innerBarFill.setStyle(colorStyle);
            } else {
                // Fallback: Apply an inline rule to the progress bar accent directly until it pulses
                if (progress > 0.65) {
                    bar.setStyle("-fx-accent: #00ff99;");
                } else if (progress > 0.25) {
                    bar.setStyle("-fx-accent: #ffd633;");
                } else {
                    bar.setStyle("-fx-accent: #ff4444;");
                }
            }
        }






        ////////////////pop up for multiple pressing on the roll dice to make the audio work 
        // prperly laggging 


        public void showSpamWarningPopup(String messageText) {
        if (!(stage.getScene().getRoot() instanceof BorderPane)) return;
        BorderPane rootPane = (BorderPane) stage.getScene().getRoot();

        // Check if a popup warning is already floating on screen to avoid layering duplicate copies
        if (rootPane.lookup(".spam-warning-container") != null) return;

        // ── 1. Construct the Alert Container Card ──
        HBox warningPopup = new HBox(12);
        warningPopup.getStyleClass().add("spam-warning-popup");
        warningPopup.setAlignment(Pos.CENTER);
        warningPopup.setMaxWidth(400); // Slightly wider to hold varying text dimensions nicely
        warningPopup.setMaxHeight(60);
        warningPopup.setPadding(new Insets(15, 24, 15, 24));
        
        // Custom inline style fallback to ensure it pops off your game board backdrop perfectly
        warningPopup.setStyle(
            "-fx-background-color: #1e1b4b; " + 
            "-fx-border-color: #ef4444; " + 
            "-fx-border-width: 2px; " + 
            "-fx-border-radius: 8px; " + 
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.6), 15, 0, 0, 4);"
        );

        Label warningLabel = new Label("⚠️ " + messageText);
        warningLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-alignment: center;");
        warningLabel.setWrapText(true);
        warningPopup.getChildren().add(warningLabel);

        // ── 🆕 2. Build the Overlay Layer Blocker ──
        // This StackPane covers the central layout layer area. It acts as an transparent 
        // overlay layer holding your warning popup centered perfectly.
        StackPane overlayContainer = new StackPane(warningPopup);
        overlayContainer.getStyleClass().add("spam-warning-container");
        overlayContainer.setAlignment(Pos.CENTER);
        
        // CRITICAL: This allows mouse clicks to pass right through the empty spaces of the container 
        // so background elements don't lose drag/click interactivity during the alert duration window!
        overlayContainer.setPickOnBounds(false); 

        // Safely cache your existing active game board grid pane before applying the warning layer
        javafx.scene.Node previousCenterNode = rootPane.getCenter();

        // Place the overlay containing the warning box directly into the central workspace boundaries
        if (previousCenterNode instanceof StackPane && !(previousCenterNode.getStyleClass().contains("spam-warning-container"))) {
            // If center is already a layout StackPane container, simply stack our popup node cleanly over it
            ((StackPane) previousCenterNode).getChildren().add(overlayContainer);
        } else {
            // Otherwise, combine your original game board and our new popup container inside a composite StackPane
            StackPane layoutStack = new StackPane();
            if (previousCenterNode != null) {
                layoutStack.getChildren().add(previousCenterNode);
            }
            layoutStack.getChildren().add(overlayContainer);
            rootPane.setCenter(layoutStack);
        }

        // ── 3. Animation Processing Pass ──
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(250), overlayContainer
        );
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(350), overlayContainer
        );
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        // ── 🆕 FIXED: Extended retention delay window length to exactly 5 seconds ──
        fadeOut.setDelay(javafx.util.Duration.seconds(5.0)); 

        // ── 🆕 4. Clean Up Sequence ──
        fadeOut.setOnFinished(e -> {
            // Safely rip down the warning layer node out of your layout hierarchies
            if (rootPane.getCenter() instanceof StackPane) {
                StackPane currentCenterStack = (StackPane) rootPane.getCenter();
                currentCenterStack.getChildren().remove(overlayContainer);
                
                // If only your original board node remains, clear out the temporary stack wrapping to restore baseline layout
                if (currentCenterStack.getChildren().size() == 1 && previousCenterNode != null) {
                    rootPane.setCenter(previousCenterNode);
                }
            }
        });

        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

        public void showVictoryOverlay(String winnerName, int position, int energy) {
            // 1. Main full-screen translucent overlay container letting the board show through
            VBox victoryOverlayContainer = new VBox(20);
            victoryOverlayContainer.setAlignment(Pos.CENTER);
            victoryOverlayContainer.setPadding(new Insets(30));
            victoryOverlayContainer.setStyle("-fx-background-color: rgba(17, 17, 34, 0.75);"); // Dark dimming filter

            // Common text glow effect for sharp contrast
            String textGlowStyle = "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.95), 12, 0.6, 0, 0);";

            // 2. Large Header Banner
            Label titleLabel = new Label("VICTORY!");
            titleLabel.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 52px; -fx-text-fill: #ffcc00; -fx-font-weight: bold;" + textGlowStyle);
            
            Label subTitleLabel = new Label(winnerName.toUpperCase() + " WINS THE COMPETITION!");
            subTitleLabel.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 24px; -fx-text-fill: #ffffff; -fx-font-weight: bold;" + textGlowStyle);

            // 3. Side-by-Side Player Profiles Container
            HBox profilesLayout = new HBox(40);
            profilesLayout.setAlignment(Pos.CENTER);
            profilesLayout.setPadding(new Insets(20));

            // Gather engine monster objects
            game.engine.monsters.Monster winnerMonster = game.getWinner();
            if (winnerMonster == null) {
                winnerMonster = game.getCurrent(); // Fallback safety
            }
            
            // Find the opponent monster instance
            game.engine.monsters.Monster opponentMonster = game.getOpponent();

            // ── WINNER CARD COLUMN ──────────────────────────────────────────────────
            VBox winnerCard = new VBox(15);
            winnerCard.setAlignment(Pos.CENTER);
            winnerCard.setPadding(new Insets(25));
            winnerCard.setMinWidth(300);
            // Gold tint border for winner card frame
            winnerCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-background-radius: 15px; -fx-border-color: #ffcc00; -fx-border-width: 2px; -fx-border-radius: 15px;");

            Label winnerHeader = new Label("🏆 WINNER");
            winnerHeader.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 22px; -fx-text-fill: #ffcc00; -fx-font-weight: bold;");

            if (winnerMonster != null) {
                StackPane winnerAvatar = AvatarUtil.buildAvatar(winnerMonster, 110);
                winnerCard.getChildren().add(winnerAvatar);
            }

            Label winnerStatsLabel = new Label(
                "Name: " + (winnerMonster != null ? winnerMonster.getName() : winnerName) + "\n" +
                "Final Position: Cell " + position + "\n" +
                "Energy Canister: " + energy + " ⚡"
            );
            winnerStatsLabel.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 16px; -fx-text-fill: #ffffff; -fx-text-alignment: center; -fx-line-spacing: 5px; -fx-font-weight: bold;");
            winnerCard.getChildren().addAll(winnerHeader, winnerStatsLabel);

            // ── OPPONENT CARD COLUMN ────────────────────────────────────────────────
            VBox opponentCard = new VBox(15);
            opponentCard.setAlignment(Pos.CENTER);
            opponentCard.setPadding(new Insets(25));
            opponentCard.setMinWidth(300);
            // Silver/Gray tint border for opponent card frame
            opponentCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 15px; -fx-border-color: #a0a0a0; -fx-border-width: 1px; -fx-border-radius: 15px;");

            Label opponentHeader = new Label("🥈 OPPONENT");
            opponentHeader.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 20px; -fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

            if (opponentMonster != null) {
                StackPane opponentAvatar = AvatarUtil.buildAvatar(opponentMonster, 110);
                opponentCard.getChildren().add(opponentAvatar);
            }

            Label opponentStatsLabel = new Label(
                "Name: " + (opponentMonster != null ? opponentMonster.getName() : "Opponent") + "\n" +
                "Final Position: Cell " + (opponentMonster != null ? opponentMonster.getPosition() : "N/A") + "\n" +
                "Energy Canister: " + (opponentMonster != null ? opponentMonster.getEnergy() : "0") + " ⚡"
            );
            opponentStatsLabel.setStyle("-fx-font-family: 'Nunito'; -fx-font-size: 16px; -fx-text-fill: #cccccc; -fx-text-alignment: center; -fx-line-spacing: 5px; -fx-font-weight: bold;");
            opponentCard.getChildren().addAll(opponentHeader, opponentStatsLabel);

            // Add both profiles to the horizontal layout container row
            profilesLayout.getChildren().addAll(winnerCard, opponentCard);

            // 4. Return to Main Menu Action Button
            Button closeButton = new Button("BACK TO MAIN MENU");
            closeButton.getStyleClass().add("mi-button"); 
            closeButton.setPrefWidth(220);
            closeButton.setOnAction(e -> {
                try {

                    // 🆕 Open the Start Screen stage window first
                    Main menu = new Main();
                    menu.start(stage);
                    
                    // 🆕 Close the current running board stage game window loop
                    this.stage.close();

                    //Main menu = new Main();
                    stage.close();
                    //menu.start(stage);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    stage.close();
                }
            });

            // 5. Build full scene stack layout view
            victoryOverlayContainer.getChildren().addAll(titleLabel, subTitleLabel, profilesLayout, closeButton);

            // 6. Layer the updated container onto the active scene tree safely without swapping windows
            if (stage.getScene() != null && stage.getScene().getRoot() instanceof Pane) {
                Pane currentRoot = (Pane) stage.getScene().getRoot();

                if (!(currentRoot instanceof StackPane)) {
                    StackPane wrapperStack = new StackPane();
                    javafx.scene.Parent oldRoot = stage.getScene().getRoot();
                    
                    stage.getScene().setRoot(new Pane()); // Clean reference link detach
                    wrapperStack.getChildren().addAll(oldRoot, victoryOverlayContainer);
                    stage.getScene().setRoot(wrapperStack);
                } else {
                    ((StackPane) currentRoot).getChildren().add(victoryOverlayContainer);
                }
            } else {
                Scene victoryScene = new Scene(victoryOverlayContainer, 1100, 750);
                stage.setScene(victoryScene);
            }
        }







}