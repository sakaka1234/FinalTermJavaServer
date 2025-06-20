package org.example.finaltermjava_server.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.example.finaltermjava_server.model.Database;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private Button btn_login;
    @FXML
    private ImageView eye;
    @FXML
    private TextField password_visible;
    @FXML
    private Button change_to_register;

    private boolean isPasswordVisible = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Password is hidden by default
        password_visible.setManaged(false);
        password_visible.setVisible(false);
        password_visible.textProperty().bindBidirectional(password.textProperty());
        //set event
        eye.setOnMouseClicked(this::togglePasswordVisibility);
        change_to_register.setOnAction(this::handleChangeToRegister);
        btn_login.setOnAction(this::handleLogin);
    }
    @FXML
    private void handleLogin(ActionEvent event){
        String mail = email.getText();
        String pass = password.getText();
        if(mail.isEmpty() || pass.isEmpty()){
            showAlert(Alert.AlertType.WARNING,"Validation Error","Please fill all fields");
            return;
        }
        if (!isValidEmail(mail)){
            showAlert(Alert.AlertType.WARNING, "Invalid Email","Please enter a valid email address");
            return;
        }

        try (Connection conn = Database.getConnection()){
            String sql = "SELECT password FROM admin WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,mail);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Email not found.");
                return;
            }
            String hashedPassword = rs.getString("password");
            BCrypt.Result result = BCrypt.verifyer().verify(pass.toCharArray(),hashedPassword);
            if(result.verified){
                String fetchsql = "SELECT username, img FROM fetchall WHERE email = ? AND role = 'admin'";
                PreparedStatement fetchstmt = conn.prepareStatement(fetchsql);
                fetchstmt.setString(1,mail);
                ResultSet fetchRs = fetchstmt.executeQuery();
                String username = "";
                String imgPath = "";
                if(fetchRs.next()){
                    username = fetchRs.getString("username");
                    imgPath = fetchRs.getString("img");
                }
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "Validation success"));
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/adminForm.fxml"));
                Scene scene = new Scene(loader.load());
                AdminFormController controller = loader.getController();
                controller.setAdminInfo(username,imgPath);

                Stage stage = (Stage) btn_login.getScene().getWindow();
                stage.setTitle("Admin");
                stage.setScene(scene);
            }else {
                showAlert(Alert.AlertType.ERROR, "Login failed","Incorrect password");
                return;
            }

        }catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not login admin.");
        }catch (IOException e){
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR," Scene Error","Can not change to admin form");
        }
    }
    @FXML
    private void togglePasswordVisibility(MouseEvent event){
        isPasswordVisible = !isPasswordVisible;
        password_visible.setManaged(isPasswordVisible);
        password_visible.setVisible(isPasswordVisible);
        password.setManaged(!isPasswordVisible);
        password.setVisible(!isPasswordVisible);
    }
    @FXML
    private void handleChangeToRegister(ActionEvent event){
        try{
            FXMLLoader loader =new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/register.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) change_to_register.getScene().getWindow();
            stage.setScene(scene);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(regex);
    }
}
