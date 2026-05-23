package game.controller;

import game.engine.Game;
import game.engine.Role;
import game.engine.cards.Card;
import game.engine.cells.CardCell;
import game.engine.cells.Cell;
import game.view.BoardView;
import game.view.DiceView;
import game.view.StartScreen;
//import javafx.application.Application;
import javafx.stage.Stage;

public class GamePlay {

    private final Stage primaryStage;
    private Game gameEngine;
    private BoardView boardView;

    public GamePlay(Stage stage) {
        this.primaryStage = stage;
    }

   
   
    
    public void handleStartGame(Role chosenRole) {
        try {
            // 1. Initialize the Core Model engine
            this.gameEngine = new Game(chosenRole);

            // 2. Initialize the main game view
            this.boardView = new BoardView(primaryStage, this, gameEngine);
            
            // 3. Render the board view
            this.boardView.show();
            primaryStage.setFullScreen(true);
         // 🆕 Register Cheat Hotkeys onto the Stage's Scene
            primaryStage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                switch (event.getCode()) {
                    case W:
                        handleCheatTeleportAction();
                        event.consume();
                        break;
                    case E:
                        handleCheatRefillEnergyAction();
                        event.consume();
                        break;
                    default:
                        break;
                    case X:
                        // ❌ Cleanly exits the entire JavaFX application platform and processes
                        System.out.println("🚪 Exiting game via 'X' hotkey...");
                        javafx.application.Platform.exit();
                        System.exit(0); 
                        event.consume();
                        break;
                }
            });
            
        } catch (Exception ex) {
            System.err.println("Initialization Error starting the game loop: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private boolean isTurnInProgress = false;


    
    
    
    
    
    
    
    
    
    ///ahmed adelll new part card deck 
    
    public void handleRollDiceAction() {
        if (gameEngine == null || boardView == null) return;

        if (isTurnInProgress) {
            boardView.showSpamWarningPopup("Hold on! Wait for the dice to finish rolling.");
            return;
        }

        isTurnInProgress = true;

        try {
            // ── 1. Snapshot BEFORE the engine runs ────────────────────────────
            game.engine.monsters.Monster mover = gameEngine.getCurrent();   // who is moving
            int     fromPos      = mover.getPosition();       // where they start
            int     energyBefore = mover.getEnergy();         // energy before any effects

            // ── 2. Engine resolves everything (move, cells, cards, turn switch) ─
            gameEngine.playTurn();

            // ── 3. Read outcomes ───────────────────────────────────────────────
            int trueRollResult = gameEngine.getLastDiceRoll();
            int toPos          = mover.getPosition();        // engine-resolved destination
            int landingCell    = game.engine.Board.getLastLandingCell(); // cell after dice, before transport

            DiceView diceView = boardView.getDiceView();

            // ── 4. Spin the dice face, then walk the token ────────────────────
            diceView.playRollAnimation(trueRollResult, () -> {

                // animateTokenMove walks the token cell-by-cell and shows the
                // floating energy-delta label, then calls our callback.
                // When landingCell != toPos, a straight-line transport animation
                // is played for the second leg (conveyor / sock).
                boardView.getBoard().animateTokenMove(
                    mover, fromPos, toPos, landingCell, energyBefore,
                    () -> {
                        try {
                            // ── 5. Card popup (if a card was drawn this turn) ──
                            game.engine.cards.Card drawnCard =
                                    game.engine.Board.getLastDrawnCard();

                            if (drawnCard != null) {
                                boardView.updateLastDrawnCardSlot(drawnCard);
                                game.view.CardPopup.show(primaryStage, drawnCard, () -> {
                                    boardView.refreshAllViewComponents();
                                    checkForWinCondition();
                                    if (gameEngine.getWinner() == null)
                                        isTurnInProgress = false;
                                });
                            } else {
                                // ── 6. Normal move — refresh & check win ──────
                                boardView.refreshAllViewComponents();
                                checkForWinCondition();
                                if (gameEngine.getWinner() == null)
                                    isTurnInProgress = false;
                            }

                        } catch (Exception ex) {
                            System.err.println("UI sync error: " + ex.getMessage());
                            boardView.refreshAllViewComponents();
                            isTurnInProgress = false;
                        }
                    }
                );
            });

        } catch (Exception ex) {
            // playTurn() threw (InvalidMoveException, etc.) — unlock immediately
            System.err.println("Move skipped or rule exception: " + ex.getMessage());
            boardView.showSpamWarningPopup("Invalid Move: " + ex.getMessage());
            boardView.refreshAllViewComponents();
            isTurnInProgress = false;
        }
    }
    
    
    
    
    
    /**
     * Fired by the Board View when the "ROLL DICE" action occurs.
     * Prevents button spamming by locking inputs and raising a smooth popup notice.
     */
    // old part ahmed instead of adels
//    public void handleRollDiceAction() {
//        if (gameEngine == null || boardView == null) return;
//        
//
//        // 🆕 THE SPAM CHECK: If a turn is already processing, raise the warning popup and block execution
//        if (isTurnInProgress) {
//            boardView.showSpamWarningPopup("Hold on! Wait for the dice to finish rolling.");
//            return;
//        }
//
//        // 🆕 Lock down the action pipeline
//        isTurnInProgress = true;
//
//        try {
//            // 1. Advance backend calculations inside the Model engine
//            gameEngine.playTurn();
//            
//            DiceView diceView = boardView.getDiceView();
//            int trueRollResult = gameEngine.getLastDiceRoll();
//
//            // 2. Pass control to view animation loop utility
//            diceView.playRollAnimation(trueRollResult, () -> {
//                
//                // 3. CALLBACK ON FINISHED: Redraw the board layout items
//                boardView.refreshAllViewComponents();
//                
//                
//                checkForWinCondition();
//                
//                // 🆕 Unlock the action pipeline so players can roll again safely!
//                // Only unlock the turn if someone hasn't won yet
//                if (gameEngine.getWinner() == null) {
//                    isTurnInProgress = false;
//                }
//                
//            });       
//
//        } catch (Exception ex) {
//            System.out.println("Move skipped or rule exception: " + ex.getMessage());
//            boardView.refreshAllViewComponents();
//            
//            // 🆕 Emergency fallback unlock if a core mechanics exception occurs
//            isTurnInProgress = false;
//        }
//    }
    
  
    public void handleUsePowerupAction() {
        if (gameEngine == null || boardView == null) return;

        try {
            gameEngine.usePowerup();
            System.out.println("Power-up is activated successfully by " + gameEngine.getCurrent().getName());
            if(gameEngine.getCurrent()==gameEngine.getPlayer()) 
            	boardView.getBoard().getPlayerToken().getPlayer().powerUPeffect();
            else boardView.getBoard().getPlayerToken().getPlayer().powerUPeffect();
            boardView.refreshAllViewComponents();

        } catch (game.engine.exceptions.OutOfEnergyException ex) {
            System.out.println("Action failed: " + ex.getMessage());
            boardView.showSpamWarningPopup("Not enough energy! Power-ups cost " + 
                    game.engine.Constants.POWERUP_COST + " energy.");
           
        }
    }
    
    
    /**
     * Checks if a win condition has been met and triggers a congratulatory popup overlay.
     */
    private void checkForWinCondition() {
        if (gameEngine == null || boardView == null) return;

        // Use the model engine's check logic to see if a monster reached cell 99 with >= 1000 energy
        game.engine.monsters.Monster winner = gameEngine.getWinner();
        if (winner != null) {
            // Block any further action loops or inputs
            isTurnInProgress = true; 

            // Trigger our custom overlay window directly on the main canvas
            boardView.showVictoryOverlay(
                winner.getName(), 
                winner.getPosition(), 
                (int) winner.getEnergy()
            );
        }
    }
    
    private void handleCheatTeleportAction() {
        if (gameEngine == null || boardView == null) return;
        
        game.engine.monsters.Monster currentMonster = gameEngine.getCurrent();
        currentMonster.setPosition(99);
        System.out.println("🔮 CHEAT: Teleported " + currentMonster.getName() + " to Cell 99!");
        
        // Refresh UI to instantly snap the token and update stats
        boardView.refreshAllViewComponents();
        
        
        checkForWinCondition();
    }

    // 🆕 Cheat 2: Increase current moving monster's energy by 1000
    private void handleCheatRefillEnergyAction() {
        if (gameEngine == null || boardView == null) return;
        
        game.engine.monsters.Monster currentMonster = gameEngine.getCurrent();
        int currentEnergy = currentMonster.getEnergy();
        currentMonster.setEnergy(currentEnergy + 1000);
        System.out.println("⚡ CHEAT: Boosted " + currentMonster.getName() + "'s energy by +1000!");
        
        // Refresh UI to instantly recalculate energy bars/canisters
        boardView.refreshAllViewComponents();
        
        
        checkForWinCondition();
    }


//      @Override
//      public void start(Stage primaryStage) throws Exception {
//              StartScreen screen = new StartScreen();
//              
//              
//              primaryStage = screen ;
//              primaryStage.show();
//              
//              
//      }
}