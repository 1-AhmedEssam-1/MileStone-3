package game.view.style.monsters;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GuiTest extends Application{
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		final double CELL_SIZE=80;
		StackPane root = new StackPane();
		String[] list={"James P. Sullivan","Mike Wazowski","Randall Boggs","Celia Mae","Roz","Fungus","Henry J. Waternoose","Yeti"};
		
		boolean shielded=true;
		VBox items= new VBox();//board pane
        for (int i = 0; i < list.length; i++) {
        	StackPane cellPane = new StackPane();
        	cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);
        	cellPane.setMinSize(CELL_SIZE, CELL_SIZE);
        	cellPane.setMaxSize(CELL_SIZE, CELL_SIZE);
        	cellPane.getStyleClass().add("board-cell");
        	MonsterGUI mon1 =new MonsterGUI("s_"+list[i],CELL_SIZE-4,"#4CA2F7");
        	
        	mon1.laugherGUI();
        	//mon1.powerUPeffect();
        	//mon1.confuse();
        	//mon1.coldFreeze(shielded);		//in player	
        		
        	mon1.shield(shielded);
        	mon1.setInCell();
    		mon1.affect(-450);
    		//mon1.affect(320);
    		cellPane.getChildren().add(mon1);
//    		cellPane.getChildren().add(freezeLap);
    		items.getChildren().add(cellPane);
        }
		
		//mon1.freeze();
		//mon1.freeze();
		// ^ --mon1.monSet();
		//mon1.powerUPeffect();
		
//		MonsterGUI mon2 =new MonsterGUI("s_Mike Wazowski",CELL_SIZE-20,"#97DE4A");
//		mon2.laugherGUI();
//		cellPane.setLeft(mon1);
		
		root.getChildren().add(items);

		Scene scene =new Scene(root,999,999);
		scene.getStylesheets().add(getClass().getResource("/game/view/css/monster.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/game/view/css/board.css").toExternalForm());
		primaryStage.setTitle("GUItest");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	public static void main(String[] args) {
		launch();
	}
}
