package game.view.style.monsters;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;


public class MonsterGUI extends StackPane{
        private StackPane freezeLap;
        private ImageView monImage;
        private Label energyEffect;
        private Label role;
        private String typeColor;
        private double startYenergyEffect;
        private double endYenergyEffect;
        private double size;
        private boolean powered;
        private boolean shield;
        public MonsterGUI(String name,double size ,String typeColor){
                super();
                this.size=size;
                this.startYenergyEffect=-0.125*size;
                this.endYenergyEffect=-0.5*size;
                Image monImg = new Image(getClass().getResourceAsStream("/assets/"+name+".png"));
                monImage=new ImageView(monImg);
                energyEffect=new Label();
                energyEffect.setTranslateX(size*0.2);
                energyEffect.setVisible(false);
                freezeLap=new StackPane();
                powered=false;
                double right=(name.equals("s_Randall Boggs"))?-1:1;
                if(right==-1){
                        monImage.setTranslateY(-18);
                }
                role =new Label();
                role.setTranslateX(size*-0.265*right);
                role.setTranslateY(-0.4*size);
                
                this.typeColor=typeColor;
                
                monSet();
                
        this.getChildren().addAll(energyEffect,role,freezeLap);
        setAlignment(monImage, Pos.CENTER);
        }
        
        private void iceCover(double opacity,boolean together){//0.48
                if(!together)freezeLap.getChildren().clear();
                Rectangle ice =new Rectangle(size*1.05,size*1.28,Color.rgb(161, 229, 247, opacity));
                ice.setArcHeight(17);
                ice.setArcWidth(28);
                if(opacity==0){
                    Image img = new Image(getClass().getResourceAsStream("/assets/shield.png")); 
                    ImagePattern imginput = new ImagePattern(img);
                    ice.setFill(imginput);
                    ice.setTranslateX(size/6);
                    ice.setTranslateY(size/6+5);
                    freezeLap.setOpacity(0.48);

                }else{
                        ice.setTranslateY(-size/8+5);
                        ice.setTranslateX(0);
                        freezeLap.setOpacity(1);
                }
                freezeLap.getChildren().add(ice);
                freezeLap.setVisible(false);
        }
//                  DropShadow glow = new DropShadow();
//                      glow.setColor(Color.web(typeColor, 0.3333*factor));
//                      glow.setRadius(20+factor*5);
//                      glow.setSpread(0.6);
        public void coldFreeze(boolean shielded){
                iceCover(0.48,shielded);
                this.freeze();
                freezeLap.setVisible(true);
                freezeLap.setOnMouseEntered(e->{
                        //Label onIce= new Label("Freezing"); 
                        Glow glow = new Glow();   
                        glow.setLevel(8);
                        freezeLap.setEffect(glow);
                        //freezeLap.getChildren().add(onIce);
                });
                freezeLap.setOnMouseExited(e->freezeLap.setEffect(null));
        }
        
        public void shield(boolean shielded){
                iceCover(0,shielded);
                freezeLap.setVisible(true);
                freezeLap.setOnMouseEntered(e->{
                        Glow glow = new Glow();   
                        glow.setLevel(8);
                        freezeLap.setOpacity(1);
                        freezeLap.setEffect(glow);
                });
                freezeLap.setOnMouseExited(e->{
                        freezeLap.setOpacity(0.48);
                        freezeLap.setEffect(null);
                });
        }
        public void setInCell(){
                Rectangle clip =new Rectangle(size*1.8,size*1.6);
                clip.setTranslateY(-size/8-8);
                this.setClip(clip);
        }
        
        public void confuse() {
            Image image = this.monImage.getImage();
            double FACTOR = 0.25;

            // =======================
            // UPPER HALF (reverted)
            // =======================
            ImageView topImage = new ImageView(image);
            
            monSizeAdjust(topImage, FACTOR*0.0000000125);
            topImage.setScaleX(4*1000000000);
            topImage.setScaleY(4*1000000000);
            topImage.setSmooth(false);
            
//          if(powered) powerUPeffect(topImage);
            // Clip only upper half
            Rectangle topClip = new Rectangle(topImage.getFitWidth(), topImage.getFitHeight() / 2);
            topImage.setClip(topClip);
            topImage.setTranslateY(-20);
            // =======================
            // LOWER HALF (BLURRED)
            // =======================
            ImageView bottomImage = new ImageView(image);
            monSizeAdjust(bottomImage, FACTOR); // Match the layout size factor
            bottomImage.setScaleX(4);           // Match the scale expansion factor!
            bottomImage.setScaleY(4);           // Match the scale expansion factor!
            bottomImage.setSmooth(true);

            GaussianBlur blur = new GaussianBlur(2); // Added a small blur radius (0 did nothing)
            bottomImage.setEffect(blur);

            // Clip only lower half
            Rectangle bottomClip = new Rectangle(bottomImage.getFitWidth(), bottomImage.getFitHeight() / 2);
            bottomClip.setY(bottomImage.getFitHeight() / 2);
            bottomImage.setClip(bottomClip);

            // =======================
            // REPLACEMENT
            // =======================
            this.getChildren().remove(monImage);
            
            StackPane confused = new StackPane();
            confused.getChildren().addAll(bottomImage, topImage);
            this.getChildren().add(0, confused);
        }
        
        void standFromCell(){//cell
                this.setClip(null);
                
        }
        
        public void monSet(){
                // Remove any confused composite pane (but not freezeLap)
                getChildren().removeIf(n -> n instanceof StackPane && n != freezeLap);
                
                glow(2);
                freezeLap.getChildren().clear();
                if(shield) this.shield(shield);
                this.setStyle(null);
                this.getStyleClass().add("monster-token");
                if(powered)powerUPeffect();
                monSizeAdjust(monImage,1);

                // Re-add monImage at index 0 only if it was removed by confuse()
                if (!getChildren().contains(monImage))
                        getChildren().add(0, monImage);
        }
        
        public void monSizeAdjust(ImageView monster,double factor){
                Image monImg=monster.getImage();
                monster.setFitHeight(size*1.5*factor);
                if(monImg.getWidth()<500 ||monImg.getWidth()==860){
                        monster.setPreserveRatio(true);
                        monster.setFitWidth(size*factor);
                }else if(monImg.getWidth()==900){
                        if(monImg.getHeight()==523){
                                monster.setFitWidth(size*2*factor);
                                monster.setFitHeight(size*1.25*factor);
                        }
                        else
                                monster.setFitWidth(size*1.85*factor);
                        monster.setFitHeight(size*1.8*factor);
                }else if(monImg.getWidth()<900){
                        monster.setFitWidth(size*1.3*factor);
                        monster.setFitHeight(size*1.45*factor);
                }else{
                        monster.setFitWidth(size*1.6*factor);
                        monster.setFitHeight(size*1.8*factor);
                }
                monster.setSmooth(true);
        }
        
        public void affect(int amount) {
        // Reset opacity so back-to-back calls always show the label
        energyEffect.setOpacity(1.0);
        energyEffect.setTranslateY(startYenergyEffect);

        if (amount >= 0) {
                heading("+"+amount,"#00ff0f",25);
        } else {
                heading(""+amount,"#ff0f0f",25);
        }
        energyEffect.setVisible(true);

        TranslateTransition floatUp = new TranslateTransition(Duration.millis(2400), energyEffect);
        floatUp.setFromY(startYenergyEffect);
        floatUp.setToY(endYenergyEffect);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(3600), energyEffect);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(1200)); // hold visible before fading

        ParallelTransition composition = new ParallelTransition(floatUp, fadeOut);
        composition.setOnFinished(event -> {
                energyEffect.setVisible(false);
                energyEffect.setOpacity(1.0); // reset for next call
        });
        composition.play();
        }
        
        private void heading(String energy,String color,double size){
                String FONT="Forte";
                Font f1=Font.font(FONT,FontWeight.BOLD, size);
                this.energyEffect.setFont(f1);
                energyEffect.setText(energy);
                energyEffect.setStyle("-fx-text-fill: linear-gradient(to bottom, "+color+" ,#69888C);");
        }       //monImage.setEffect(harm);

        public void freeze(){
                glow(0);
                ColorAdjust c = new ColorAdjust();  
            c.setBrightness(0.2);
            c.setContrast(0.1);  
            c.setSaturation(0.05);  
            monImage.setEffect(c);
                this.getStyleClass().add("freezed");
                
        }
        
        DropShadow glow(double factor){
                DropShadow glow = new DropShadow();
                glow.setColor(Color.web(typeColor, 0.3333*factor));
                glow.setRadius(20+factor*5);
                glow.setSpread(0.6);
                monImage.setEffect(glow);
                return glow;
        }
        
        public void powerUPeffect(){
                DropShadow glowing=glow(2.8);
                powered=true;
                Bloom bloom = new Bloom();  
                bloom.setThreshold(0.08);
                bloom.setInput(glowing);
                monImage.setEffect(bloom);  
                monImage.getStyleClass().add("powerup");
        }
        
        public void scarerGUI(){//this.getStyleClass().add("monster-token");
                role.setText("🦇");
                role.getStyleClass().add("scarer-active");
                
        }
        
        public void laugherGUI(){
                role.setText("😂");
                role.setTranslateX(size*-0.5);
                role.setTranslateY(size*-0.5);
                role.getStyleClass().add("laugher-active");
        }
        
}