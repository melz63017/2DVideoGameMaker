package src.games;

import src.events.EntityAction;
import src.events.InputSystem;
import src.model.component.character.Health;
import src.model.component.character.Score;
import src.model.component.movement.Position;
import src.model.component.movement.Velocity;
import src.model.component.visual.ImagePath;
import src.model.entity.EntitySystem;
import src.testing.PhysicsEngine;
import src.api.IEntity;
import src.api.IEntitySystem;

import java.util.List;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import src.model.entity.Entity;

public class ACGame {
	
    public static final String TITLE = "Ani's and Carolyn's game";
    public static final int KEY_INPUT_SPEED = 5;
    private static final double GROWTH_RATE = 1.1;
    private static final int BOUNCER_SPEED = 30;

	private final EntitySystem universe = new EntitySystem();
	private final InputSystem inputSystem = new InputSystem();
	private final PhysicsEngine physics = new PhysicsEngine();
	private IEntity character;
	private final String IMAGE_PATH = "resources/images/blastoise.png";
    
    private Scene myScene;
    

    /**
     * Returns name of the game.
     */
    public String getTitle () {
        return TITLE;
    }

    /**
     * Create the game's scene
     */
    public Scene init (int width, int height) {
        // Create a scene graph to organize the scene
        Group root = new Group();
        // Create a place to see the shapes
        myScene = new Scene(root, width, height, Color.WHITE);
        initEngine();
        return myScene;
    }
    
    public void initEngine() { 
    	addCharacter();
    	
    }

	private void addCharacter() {
		character = new Entity("Anolyn");
		character.forceAddComponent(new Health((double) 100), true);
		character.forceAddComponent(new Score((double) 100), true);
		Position pos = new Position(250.0, 250.0);
		character.forceAddComponent(pos, true);
		character.forceAddComponent(new ImagePath(IMAGE_PATH), true);
		character.forceAddComponent(new Velocity(20.0, 50.0), true);
		character.getComponent(Position.class).getProperties().get(0).addListener(new EntityAction(character));
    	universe.addEntity(character);
	}
	
	private void step(double dt, IEntity character, IEntitySystem system) {
		physics.update(system, dt);
		moveEntity(character, 20);
	}
	
	private void moveEntity(IEntity character, int move) { 
		 Position pos = character.getComponent(Position.class);
		 pos.setX(pos.getX() + move);
	}
}
