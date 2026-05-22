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
    private static GamePlay gameplay;
	@Override
    public void start(Stage primalyStage) {//----- 
        background= new GameBackground();
        // Open start screen properly
        StartScreen startScreen = new StartScreen();
        startScreen.setFullScreen(true);

        primalyStage=startScreen;
        stage=primalyStage;
        gameplay= new GamePlay(stage);

        //background.pauseMusic();

        // ✅ FIXED: now works
        startScreen.getScarerBtn().setOnAction(e -> startGame(Role.SCARER));
        startScreen.getLaugherBtn().setOnAction(e -> startGame(Role.LAUGHER));
        
    }
	
	public static void showMenu() {
        // Create the temporary menu window
		GameBackground.switchTrack("/assets/StartMenuTrack.mp3");
        StartScreen startScreen = new StartScreen();
        startScreen.setFullScreen(true);

        // Bind the buttons directly to trigger gameplay on our permanent primary stage
        startScreen.getScarerBtn().setOnAction(e -> {
            startScreen.close(); // Close the menu window safely
            startGame(Role.SCARER);
        });

        startScreen.getLaugherBtn().setOnAction(e -> {
            startScreen.close(); // Close the menu window safely
            startGame(Role.LAUGHER);
        });
    }
	
	
 // ✅ FIXED GAME START METHOD
    private static void startGame(Role role) {
        try {
        	GameBackground.switchTrack("/assets/Monsters Inc theme full.mp3");
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