package view.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import api.IEditor;
import api.ISerializable;
import enums.DefaultStrings;
import enums.FileExtensions;
import enums.GUISize;
import enums.ViewInsets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.entity.Entity;
import model.entity.EntitySystem;
import view.Authoring;
import view.Utilities;

public class GameEditor extends Editor {
	
	private VBox pane;
	private List<Node> entryList;
	private String iconPath;
	private ImageView icon;
	private ResourceBundle myResources;
	private EditorFactory editFact;
	private Authoring authEnv;
	private String myLanguage;
	
	public GameEditor(Authoring authEnv, String language){
		myLanguage = language;
		pane = new VBox(GUISize.GAME_EDITOR_PADDING.getSize());
		pane.setPadding(ViewInsets.GAME_EDIT.getInset());
		pane.setAlignment(Pos.TOP_LEFT);
		entryList = new ArrayList<>();
		myResources = ResourceBundle.getBundle(language);
		editFact = new EditorFactory();
		this.authEnv=authEnv;
		
	}

	@Override
	public void loadDefaults() {
		// TODO Auto-generated method stub

	}

	@Override
	public Pane getPane() {
		populateLayout();
		return pane;
	}

	@Override
	public void populateLayout() {
		createTextEntry("gName");
		createTextEntry("gDesc");
		showIcon();
		editorButtons();
		pane.getChildren().add(Utilities.makeButton(myResources.getString("saveGame"), e->saveGame()));

	}

	
	private void createTextEntry(String name){
		HBox container = new HBox(GUISize.GAME_EDITOR_HBOX_PADDING.getSize());
		Label title = new Label(myResources.getString(name));
		TextArea entryBox = Utilities.makeTextArea(myResources.getString(name));
		container.getChildren().addAll(title, entryBox);
		HBox.setHgrow(entryBox, Priority.SOMETIMES);
		pane.getChildren().add(container);
		entryList.add(entryBox);
	}

	private void saveGame() {
		// TODO Auto-generated method stub
	}

	private void editorButtons() {
		pane.getChildren().add(Utilities.makeButton(myResources.getString(DefaultStrings.ENTITY_EDITOR_NAME.getDefault()), 
				e->createEntityEditor(DefaultStrings.ENTITY_EDITOR_NAME.getDefault())));
		pane.getChildren().add(Utilities.makeButton(myResources.getString(DefaultStrings.ENVIRONMENT_EDITOR_NAME.getDefault()), 
				e->createEnvironmentEditor(DefaultStrings.ENVIRONMENT_EDITOR_NAME.getDefault())));
	//pane.getChildren().add(Utilities.makeButton(myResources.getString(DefaultStrings.ENVIRONMENT_EDITOR_NAME.getDefault()), 
		//e->createEnvironmentEditor()));
		}
	
	private void createEntityEditor(String editorName){
		ISerializable passedParameter = new Entity();
		createEditor(editorName, passedParameter);
	}

	private void createEnvironmentEditor(String editorName) {
		EntitySystem entitySystem = new EntitySystem();
		entitySystem.addEntity(new Entity());
		entitySystem.addEntity(new Entity());
		entitySystem.addEntity(new Entity());
		createEditor(editorName, entitySystem);
	}

	private void createEditor(String editName, ISerializable passedParameter) {
		IEditor editor = editFact.createEditor(editName,myLanguage,passedParameter);
		editor.populateLayout();
		authEnv.createTab(editor.getPane(), editName, true);
	}

	private void showIcon() {
		HBox iconBox = new HBox(GUISize.GAME_EDITOR_HBOX_PADDING.getSize());
		iconBox.setAlignment(Pos.CENTER_LEFT);
		Label iconTitle = new Label(myResources.getString("gIcon"));
		icon = new ImageView();
		setIconPicture(new File(DefaultStrings.DEFAULT_ICON.getDefault()));
		iconBox.getChildren().addAll(iconTitle, icon, Utilities.makeButton(myResources.getString("cIcon"), e->updateIcon()));
		pane.getChildren().add(iconBox);
	}

	private void setIconPicture(File file) {
		iconPath = file.toURI().toString();
		icon.setImage(new Image(iconPath));
		icon.setFitHeight(GUISize.ICON_SIZE.getSize());
		icon.setFitWidth(GUISize.ICON_SIZE.getSize());
	}

	private void updateIcon() {
		Stage s = new Stage();
		FileChooser fChoose = new FileChooser();
		fChoose.setTitle(myResources.getString("cIcon"));
		fChoose.getExtensionFilters().addAll(FileExtensions.GIF.getFilter(), FileExtensions.JPG.getFilter(), FileExtensions.PNG.getFilter());
		File file = fChoose.showOpenDialog(s);
		setIconPicture(file);
		
	}


	@Override
	public void updateEditor() {
		populateLayout();

	}

	@Override
	public void addSerializable(ISerializable serialize) {
		// TODO Auto-generated method stub
		
	}

}
