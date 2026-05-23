package game.view;

import game.engine.Role;
import game.engine.monsters.Monster;

import javafx.geometry.Pos;

import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.StackPane;

import javafx.scene.paint.Color;

import javafx.scene.shape.Circle;

import java.net.URL;

public class AvatarUtil {

    private AvatarUtil() {}

    public static StackPane buildAvatar(
            Monster m,
            double size
    ) {

        StackPane pane = new StackPane();

        pane.setPrefSize(size, size);

        pane.setMinSize(size, size);

        pane.setMaxSize(size, size);

        pane.setAlignment(Pos.CENTER);

        String filename = toFilename(m.getName());

        URL url =
                AvatarUtil.class.getResource(
                        "/assets/" + filename
                );

        // ───────────────── IMAGE AVATAR ─────────────────

        if (url != null) {

            try {

                Image img =
                        new Image(url.toExternalForm());

                if (!img.isError()) {

                    ImageView iv = new ImageView(img);

                    // Better fitting
                    iv.setFitWidth(size - 4);

                    iv.setFitHeight(size - 4);

                    iv.setPreserveRatio(true);
                    

                    iv.setSmooth(true);

                    // ─── Circular clip ─────────────────

                    Circle clip =
                            new Circle(size / 2);

                    clip.setCenterX(size / 2);

                    clip.setCenterY(size / 2);

                    iv.setClip(clip);

                    // ─── Border circle ─────────────────

                    Circle border =
                            new Circle((size / 2) - 1);

                    border.setFill(Color.TRANSPARENT);

                    border.setStrokeWidth(3);

                    if (m.getRole() == Role.SCARER) {

                        border.setStroke(
                                Color.web("#ef4444")
                        );

                    } else {

                        border.setStroke(
                                Color.web("#38bdf8")
                        );
                    }

                    // ─── Outer glow shadow ─────────────

                    pane.setStyle(
                            "-fx-effect:" +
                            "dropshadow(" +
                            "gaussian," +
                            "rgba(0,0,0,0.45)," +
                            "12,0.25,0,3);"
                    );

                    pane.getChildren().addAll(iv, border);

                    return pane;
                }

            } catch (Exception e) {

                System.out.println(
                        "Avatar load failed: "
                                + filename
                );
            }
        }

        // ───────────────── FALLBACK AVATAR ─────────────────

        Color fill =
                (m.getRole() == Role.SCARER)
                        ? Color.web("#ef4444")
                        : Color.web("#38bdf8");

        Circle circle =
                new Circle(size / 2, fill);

        circle.setStroke(Color.WHITE);

        circle.setStrokeWidth(3);

        Label initial =
                new Label(
                        m.getName()
                         .substring(0, 1)
                         .toUpperCase()
                );

        initial.setStyle(
                "-fx-font-size:"
                        + (int)(size / 2.3)
                        + "px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:white;"
        );

        pane.getChildren().addAll(circle, initial);

        return pane;
    }

    // ────────────────────────────────────────────────

    private static String toFilename(String name) {

        return name
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase()
                + ".png";
    }
}