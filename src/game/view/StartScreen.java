package game.view;

import java.io.IOException;

import game.view.start.GameInstructions;
import game.view.style.monsters.MonsterGUI;
import game.view.style.monsters.PlayerMonster;
import game.controller.GamePlay;
import game.engine.Role;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class StartScreen extends Stage {

    //private final GamePlay controller; // Controller link

    private StackPane root;
    private VBox mainLayout;
    private Popup rolePopup;
    private Popup instructionsPopup;

    // ✅ FIX: store stage globally
    private static Button scarerBtn;
    private static Button laugherBtn;
    
    
    public VBox getInstructionsPopup() {
		return instructionsPopup;
	}

	public Button getScarerBtn() {
		return scarerBtn;
	}

	public Button getLaugherBtn() {
		return laugherBtn;
	}

    public StartScreen(){ 

        // ✅ FIX: assign stage
        root = new StackPane();
        root.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #1a331a, #0f0225);");

        Pane animationPane = new Pane();
        createScatteredMonsterAnimation(animationPane);

        setupMenus();

        root.getChildren().addAll(animationPane, mainLayout, rolePopup, instructionsPopup);

        Scene scene = new Scene(root, 1500, 980);
        scene.getStylesheets().add(getClass().getResource("/game/view/css/style.css").toExternalForm());

        this.setOnCloseRequest(e -> System.exit(0));
        this.setTitle("DooR DasH: Scare vs Laugh Touchdown");
        this.setScene(scene);
        this.show();
    }

    private void setupMenus() {
        mainLayout = new VBox(30);
        mainLayout.setAlignment(Pos.CENTER);

        Text title = new Text("MONSTERS, INC.");
        title.getStyleClass().add("mi-title");

        Button btnStart = new Button("START GAME");
        btnStart.getStyleClass().add("mi-button");
        btnStart.setOnAction(e -> rolePopup.showPopup());

        Button btnHowTo = new Button("HOW TO PLAY");
        btnHowTo.getStyleClass().add("mi-button");
        btnHowTo.setOnAction(e -> instructionsPopup.showPopup());

        Button btnExit = new Button("EXIT");
        btnExit.getStyleClass().addAll("mi-button", "btn-exit");
        btnExit.setOnAction(e -> System.exit(0));

        mainLayout.getChildren().addAll(title, btnStart, btnHowTo, btnExit);

        // ───────── ROLE POPUP ─────────
        rolePopup =new Popup("SELECT YOUR SIDE");

        scarerBtn = new Button("SCARER");
        scarerBtn.getStyleClass().add("mi-button");

        laugherBtn = new Button("LAUGHER");
        laugherBtn.getStyleClass().add("mi-button");

        Button btnBackRole = new Button("BACK");
        btnBackRole.getStyleClass().add("secondary-button");
        btnBackRole.setOnAction(e -> rolePopup.hidePopup());

        rolePopup.getChildren().addAll(scarerBtn, laugherBtn, btnBackRole);

        // ───────── INSTRUCTIONS POPUP ─────────
        instructionsPopup = new Popup("HOW TO PLAY ?");
        
        GameInstructions instrText = null;
		try {
			instrText = new GameInstructions();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        Button btnBackInstr = new Button("BACK");
        btnBackInstr.getStyleClass().add("secondary-button");
        btnBackInstr.setOnAction(e -> instructionsPopup.hidePopup());

        instructionsPopup.getChildren().addAll(instrText, btnBackInstr);
    }

    private void createScatteredMonsterAnimation(Pane pane) {
        
    	String[] mons=PlayerMonster.getList();
        for (int j = 0; j < mons.length; j++) {
        	double pupilR= Math.random()*12+40;
			PlayerMonster mon=new PlayerMonster(mons[j],pupilR*2,"#123456");
			MonsterGUI[] faces=mon.getPlayerList();
			for (int k = 0; k < faces.length; k++) {
				StackPane monster = new StackPane();
	            double faceR= Math.random()*32+66;
	            int randint= (int)(faceR-20)*9;
	            Circle face = new Circle(faceR,Color.web("#abf"+randint, 0.2));
	            
            	double eyeR= Math.random()*8+39;
            	Circle eye = new Circle(eyeR, Color.WHITE);
                eye.setOpacity(0.4);
                
				
				MonsterGUI moni=faces[k];
				monster.getChildren().addAll(face, eye, moni);
				monster.setLayoutX(Math.random() * 1500 - 200);
	            monster.setLayoutY(Math.random() * 1200 - 200);
	
	            Screen screen = Screen.getPrimary();
	            Rectangle2D bounds = screen.getBounds();
	
	            double w = bounds.getWidth();
	            double l = bounds.getHeight();
	
	            TranslateTransition tt =new TranslateTransition(Duration.seconds(10 + Math.random() * 10),monster);
	            tt.setByX(Math.random() * w - 200);
	            tt.setByY(Math.random() * l - 200);
	
	            tt.setCycleCount(Animation.INDEFINITE);
	            tt.setAutoReverse(true);
	            tt.play();
	
	            pane.getChildren().add(monster);
				
			}            
        }
    }

}