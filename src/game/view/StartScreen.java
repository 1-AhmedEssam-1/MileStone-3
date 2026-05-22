package game.view;

import java.io.IOException;

import game.view.start.GameInstructions;
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
        //added by 3ssam
        this.setFullScreen(true);
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
        for (int i = 0; i < 15; i++) {
            Group monster = new Group();
            Circle face = new Circle(25, Color.web(i % 2 == 0 ? "#5b8fd4" : "#6abf69", 0.2));
            Circle eye = new Circle(8, Color.WHITE); eye.setOpacity(0.4);
            Circle pupil = new Circle(3, Color.BLACK); pupil.setOpacity(0.4);

            monster.getChildren().addAll(face, eye, pupil);
            monster.setLayoutX(Math.random() * 1500 - 200);
            monster.setLayoutY(Math.random() * 1200 - 200);

            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getBounds();
            double w = bounds.getWidth();
            double l = bounds.getHeight();

            TranslateTransition tt = new TranslateTransition(Duration.seconds(15 + Math.random() * 10), monster);
            tt.setByX(Math.random() * w - 200);
            tt.setByY(Math.random() * l - 200);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.play();
            pane.getChildren().add(monster);
        }
    }

}