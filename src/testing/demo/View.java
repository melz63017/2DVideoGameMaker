package testing.demo;

import api.IEntity;
import api.IEntitySystem;
import api.ISystemManager;
import groovy.lang.GroovyShell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.component.movement.Orientation;
import model.component.movement.Position;
import model.component.visual.ImagePath;
import usecases.SystemManager;

/**
 * 
 * @author Tom
 *
 */
public class View {

	private final double MILLISECOND_DELAY = 10;
	private final double SECOND_DELAY = MILLISECOND_DELAY / 1000;
	private final double gapSize = 10;

	private final Stage myStage;
	private final Group root = new Group();
	private final ConsoleTextArea console = new ConsoleTextArea();
	private final Button evaluateButton = new Button("Evaluate");
	private final Button loadButton = new Button("Load");
	// private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("Groovy");
	private final GroovyShell shell = new GroovyShell(); // MUST USE SHELL
	private final ISystemManager model = new SystemManager();
	private final Pong game = new Pong(shell, model); // TODO: move to level hierarchy
	IEntitySystem universe = model.getEntitySystem();

	public View(Stage stage) {
		this.myStage = stage;
		this.initEngine();
		this.initConsole();
		this.initButtons();
		BorderPane pane = this.createBorderPane();
		Scene scene = new Scene(pane, 500, 500);
		stage.setScene(scene);
		stage.show();

		KeyFrame frame = new KeyFrame(Duration.millis(MILLISECOND_DELAY), e -> this.step(SECOND_DELAY));
		Timeline animation = new Timeline();
		animation.setCycleCount(Timeline.INDEFINITE);
		animation.getKeyFrames().add(frame);
		animation.play();
	}

	private void step(double dt) { // game loop
		// simulate
		model.step(dt);

		// render
		root.getChildren().clear();
		for (IEntity e : model.getEntitySystem().getAllEntities()) {
			if (e.hasComponents(ImagePath.class, Position.class)) {
				ImagePath display = e.getComponent(ImagePath.class);
				ImageView imageView = display.getImageView();

				Position pos = e.getComponent(Position.class);
				imageView.setTranslateX(pos.getX());
				imageView.setTranslateY(pos.getY());

				if (e.hasComponent(Orientation.class)) {
					Orientation o = e.getComponent(Orientation.class);
					imageView.setRotate(o.getOrientation());
				}

				root.getChildren().add(imageView);
			}
		}
	}

	private BorderPane createBorderPane() {
		BorderPane pane = new BorderPane();
		ScrollPane center = new ScrollPane();
		pane.setPadding(new Insets(gapSize, gapSize, gapSize, gapSize));
		pane.setCenter(center);
		center.setContent(root);
		// root.setLayoutX(0);
		// root.setLayoutY(0);
		root.setManaged(false); // IMPORTANT

		center.setPannable(true);
		// center.setFitToHeight(false);
		// center.setFitToWidth(false);
		center.setVbarPolicy(ScrollBarPolicy.NEVER);
		center.setHbarPolicy(ScrollBarPolicy.NEVER);

		// GridPane inputPane = new GridPane();
		// inputPane.add(console, 0, 0);
		// inputPane.add(evaluateButton, 0, 1);
		BorderPane inputPane = new BorderPane();
		inputPane.setTop(console);
		inputPane.setBottom(evaluateButton);
		// inputPane.setRight(loadButton);
		pane.setBottom(inputPane);
		return pane;
	}

	private void initConsole() {
		console.appendText("\n");
		console.setOnKeyPressed(e -> {
			KeyCode keyCode = e.getCode();
			if (keyCode == KeyCode.ENTER) {
				this.evaluate();
				e.consume();
			}
		});
	}

	private void initButtons() {
		// evaluateButton.setText("Evaluate");
		evaluateButton.setOnAction(e -> this.evaluate());
		loadButton.setOnAction(e -> this.load());
	}

	private void initEngine() { // TODO: make it possible to import classes
								// directly within shell
	}

	private void load() { // TODO: load from "demo.xml"
		// this.model = new
		// XMLReader<ISystemManager>().readSingleFromFile("demo.xml");
	}

	private void evaluate() {
		String text = console.getText();
		String command = text.substring(text.lastIndexOf("\n")).trim();
		console.println("\n----------------");
		try {
			// Object result = engine.eval(command);
			Object result = shell.evaluate(command);
			if (result != null) {
				console.print(result.toString());
			}
		} catch (Exception e) {
			console.println(e.getMessage());
		}
		console.println();
	}

}
