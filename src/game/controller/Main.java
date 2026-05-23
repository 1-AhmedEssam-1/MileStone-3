package game.controller;

import game.engine.Game;
import game.engine.Role;
import game.view.BoardView;
import game.view.StartScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
	GameBackground background;
    private Stage stage;
    private GamePlay gameplay;
	@Override
    public void start(Stage primalyStage) {//----- 
        background= new GameBackground();
        // Open start screen properly
        StartScreen startScreen = new StartScreen();

        primalyStage=startScreen;
        stage=primalyStage;
        gameplay= new GamePlay(stage);

        background.pauseMusic();

        // ✅ FIXED: now works
        startScreen.getScarerBtn().setOnAction(e -> startGame(Role.SCARER));
        startScreen.getLaugherBtn().setOnAction(e -> startGame(Role.LAUGHER));
        
    }
 // ✅ FIXED GAME START METHOD
    private void startGame(Role role) {
        try {
            Game game = new Game(role);

            gameplay.handleStartGame(role);
            
            //BoardView boardView = new BoardView(stage, gameplay , game);
            //boardView.show();

        } catch (Exception ex) {ex.printStackTrace();}
    }
    
    public static void main(String[] args) {
        System.out.println("siso");
    	launch(args);
    }
}