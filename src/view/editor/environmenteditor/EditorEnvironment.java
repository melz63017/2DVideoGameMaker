package view.editor.environmenteditor;

import api.IEntity;
import api.ILevel;
import api.ISerializable;
import api.IView;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import model.component.movement.Position;
import model.component.visual.Sprite;
import view.View;
import view.editor.Editor;
import view.editor.EditorFactory;
import view.editor.entityeditor.EditorEntity;
import view.enums.DefaultEntities;
import view.enums.DefaultStrings;
import view.enums.GUISize;
import view.utilities.*;
import voogasalad.util.reflection.Reflection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class EditorEnvironment extends Editor {

	private String myLanguage;
	private BorderPane environmentPane = new BorderPane();
	private ILevel myLevel;
	private ResourceBundle myResources;
	private ObservableList<IEntity> masterEntityList;
	private ObservableList<ILevel> allEnvironmentsList;
	private VBox leftPane = new VBox();
	private VBox rightPane = new VBox();
	private VBox masterEntityButtonsBox = new VBox();
	private VBox environmentEntityButtonsBox = new VBox();
	private TextField nameField = new TextField();
	private ScrollPane scrollPane = new ScrollPane(environmentPane);
	private IView view;

	public EditorEnvironment(String language, ILevel toEdit, ObservableList<IEntity> masterList,
			ObservableList<ILevel> addToList) {
		myLanguage = language;
		myResources = ResourceBundle.getBundle(language);
		masterList.addListener((ListChangeListener<IEntity>) c -> {
			this.updateEditor();
		});
		masterEntityList = masterList;
		myLevel = toEdit;
		allEnvironmentsList = addToList;

		view = new View((GUISize.TWO_THIRDS_OF_SCREEN.getSize()), GUISize.HEIGHT_MINUS_TAB.getSize(), myLevel,
				myLanguage);
		addLayoutComponents();
	}

	private void addLayoutComponents() {
		setLeftPane();
		setRightPane();
	}

	private void setLeftPane() {
		leftPane.getChildren().add(setNameDisplay());
		leftPane.getChildren().add(setEntityOptionsDisplay());
	}

	private TextField setNameDisplay() {
		if (view.getEntitySystem().getName().equals("")) {
			nameField.setText(myResources.getString("environmentName"));
		} else {
			nameField.setText(view.getEntitySystem().getName());
		}
		return nameField;
	}

	private ScrollPane setEntityOptionsDisplay() {
		if (masterEntityList.isEmpty()) {
			loadDefaults();
		}
		populateVbox(masterEntityButtonsBox, masterEntityList, "createAddEntityButton");
		return (new ScrollPane(TitledPaneFactory.makeTitledPane(myResources.getString("masterTemplates"), masterEntityButtonsBox, true)));
	}

	private void populateVbox(VBox vbox, Collection<IEntity> collection, String methodName) {
		vbox.getChildren().clear();
		for (IEntity entity : collection) {
			Button button = (Button) Reflection.callMethod(this, methodName, entity);
			(button).setMaxWidth(Double.MAX_VALUE);
			vbox.getChildren().add(button);
		}
	}

	public Button createAddEntityButton(IEntity entity) {
		return ButtonFactory.makeButton((entity).getName(), e -> addToSystem(EntityCopier.copyEntity(entity)));
	}

	public Button createEntityButton(IEntity entity) {
		Button entityInButton = new Button(entity.getName());
		entityInButton.setMaxWidth(Double.MAX_VALUE);
		entityInButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				MouseButton button = event.getButton();
				if (button == MouseButton.PRIMARY) {
					entityLeftClicked(entity);
				} else if (button == MouseButton.SECONDARY) {
					entityRightClicked(entity, entityInButton, event);
				}
			}
		});
		return entityInButton;
	}

	private void addToSystem(IEntity entity) {
		String newName = UserInputBoxFactory.userInputBox(myResources.getString("noName"),
				myResources.getString("addEntityName"));
		if (newName != null) {
			entity.setName(newName);
		}
		view.getEntitySystem().addEntity(entity);
		try {
			if (!entity.hasComponent(Position.class) || !entity.hasComponent(Sprite.class)) {
				addComponents(entity);
			}
			environmentEntityButtonsBox.getChildren().add(createEntityButton(entity));
		} catch (Exception e) {
			Alerts.showAlert(myResources.getString("error"), null, myResources.getString("unableToAddEntity"),
					AlertType.ERROR);
		}
	}

	private void loadDefaults() {
		if (Alerts.showAlert(myResources.getString("addDefaults"), myResources.getString("addDefaultsQuestion"),
				myResources.getString("defaultsMessage"), AlertType.CONFIRMATION)) {
			masterEntityList.add(DefaultEntities.BACKGROUND.getDefault());
			masterEntityList.add(DefaultEntities.CHAR_1.getDefault());
			masterEntityList.add(DefaultEntities.CHAR_2.getDefault());
		}
	}

	private void setRightPane() {
		rightPane.getChildren()
				.add(ButtonFactory.makeButton(myResources.getString("saveEnvironment"), e -> saveEnvironment()));
		rightPane.getChildren().add(new ScrollPane(TitledPaneFactory.makeTitledPane(myResources.getString("environmentInstances"), environmentEntityButtonsBox, true)));
	}

	private void entityLeftClicked(IEntity entity) {
		view.toggleHighlight(entity);
		ObservableList<IEntity> entityList = FXCollections.observableArrayList();
		EditorEntity entityEditor = (EditorEntity) new EditorFactory().createEditor(EditorEntity.class.getName(),
				myLanguage, entity, entityList);
		entityEditor.populateLayout();
		entityList.addListener((ListChangeListener<IEntity>) c -> {this.updateEditor();});
		PopUp myPopUp = new PopUp(GUISize.ENTITY_EDITOR_WIDTH.getSize(), GUISize.ENTITY_EDITOR_HEIGHT.getSize());
		myPopUp.show(entityEditor.getPane());
	}

	private void entityRightClicked(IEntity entity, Button entityButton, MouseEvent event) {
		Map<String, EventHandler<ActionEvent>> menuMap = new LinkedHashMap<String, EventHandler<ActionEvent>>();
		menuMap.put(myResources.getString("remove"), e -> removeFromDisplay(entity, entityButton));
		menuMap.put(myResources.getString("sendBack"), e -> sendToBack(entity));
		menuMap.put(myResources.getString("sendFront"), e -> sendToFront(entity));
		menuMap.put(myResources.getString("sendBackOne"), e -> sendBackward(entity));
		menuMap.put(myResources.getString("sendForwardOne"), e -> sendForward(entity));
		menuMap.put(myResources.getString("saveAsMasterTemplate"), e -> saveToMasterList(entity));
		entityButton.setContextMenu(ContextMenuFactory.createContextMenu(menuMap));
	}

	private void saveToMasterList(IEntity entity) {
		masterEntityList.add(entity);
	}

	private void sendToFront(IEntity e) {
		if (view.getEntitySystem().removeEntity(e.getID()) != null) {
			view.getEntitySystem().addEntity(e);
		}
		populateVbox(environmentEntityButtonsBox, view.getLevel().getAllEntities(), "createEntityButton");
	}

	private void sendToBack(IEntity e) {
		if (view.getEntitySystem().removeEntity(e.getID()) != null) {
			view.getEntitySystem().getAllEntities().add(0, e);
		}
		populateVbox(environmentEntityButtonsBox, view.getLevel().getAllEntities(), "createEntityButton");
	}

	private void sendForward(IEntity e) {
		int index = view.getEntitySystem().getAllEntities().indexOf(e) + 1;
		if (view.getEntitySystem().removeEntity(e.getID()) != null) {
			if (index < view.getEntitySystem().getAllEntities().size()) {
				view.getEntitySystem().getAllEntities().add(index, e);
			} else {
				view.getEntitySystem().getAllEntities().add(e);
			}
		}
		populateVbox(environmentEntityButtonsBox, view.getLevel().getAllEntities(), "createEntityButton");
	}

	private void sendBackward(IEntity e) {
		int index = view.getEntitySystem().getAllEntities().indexOf(e) - 1;
		if (view.getEntitySystem().removeEntity(e.getID()) != null) {
			if (index >= 0) {
				view.getEntitySystem().getAllEntities().add(index, e);
			} else {
				view.getEntitySystem().getAllEntities().add(0, e);
			}
		}
		populateVbox(environmentEntityButtonsBox, view.getLevel().getAllEntities(), "createEntityButton");
	}

	@Override
	public void updateEditor() {
		populateVbox(masterEntityButtonsBox, masterEntityList, "createAddEntityButton");
		populateVbox(environmentEntityButtonsBox, view.getLevel().getAllEntities(), "createEntityButton");
	}

	@Override
	public void populateLayout() {
		environmentPane.setRight(rightPane);
		environmentPane.setLeft(leftPane);
		environmentPane.setCenter(view.getPane());
	}

	private void saveEnvironment() {
		String name = getName();
		view.getEntitySystem().setName(name);
		allEnvironmentsList.remove(view.getLevel());
		allEnvironmentsList.add(view.getLevel());
		environmentPane.getChildren().clear();
		environmentPane.setCenter(saveMessage(myResources.getString("saveMessage")));
	}

	private String getName() {
		String returnName = null;
		if (nameField.getText().equals(myResources.getString("environmentName"))) {
			returnName = UserInputBoxFactory.userInputBox(myResources.getString("noName"),
					myResources.getString("noNameMessage"));
		} else {
			returnName = nameField.getText();
		}
		return returnName;
	}

	private void addComponents(IEntity entity) {
		if (Alerts.showAlert(myResources.getString("confirm"), myResources.getString("componentsRequired"),
				myResources.getString("addComponentQuestion"), AlertType.CONFIRMATION)) {
			addPositionComponent(entity);
			addImagePathComponent(entity);
		}
	}

	private void addPositionComponent(IEntity entity) {
		entity.setSpec(Position.class, 1);
		Position pos = new Position();
		entity.addComponent(pos);
	}

	private void addImagePathComponent(IEntity entity) {
		File file = FileUtilities.promptAndGetFile(FileUtilities.getImageFilters(), 
				myResources.getString("pickImagePathImage"), DefaultStrings.GUI_IMAGES.getDefault());
		entity.setSpec(Sprite.class, 1);
		entity.addComponent(new Sprite(file.getPath()));
	}

	private void removeFromDisplay(IEntity entity, Button entityButton) {
		view.getEntitySystem().removeEntity(entity.getID());
		environmentEntityButtonsBox.getChildren().remove(entityButton);
	}

	public ILevel getLevel() {
		return view.getLevel();
	}

	@Override
	public ScrollPane getPane() {
		return scrollPane;
	}

	public boolean displayContains(IEntity checkEntity) {
		return masterEntityList.contains(checkEntity);
	}

	public boolean environmentContains(IEntity checkEntity) {
		return view.getEntitySystem().containsEntity(checkEntity);
	}

}