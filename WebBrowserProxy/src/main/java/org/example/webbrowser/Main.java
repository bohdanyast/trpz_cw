package org.example.webbrowser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
	
	@Override
	public void start(Stage stage) throws IOException {
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("web_browser.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root);
		stage.setScene(scene);
        stage.setTitle("Web Browser");
        stage.setResizable(false);
		stage.show();
	}	

	public static void main(String[] args) {
		launch(args);
	}
}