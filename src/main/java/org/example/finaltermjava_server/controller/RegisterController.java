package org.example.finaltermjava_server.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
import java.util.Collection;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    @FXML
    private TextField username;
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private TextField password_visible;
    @FXML
    private Button btn_register;
    @FXML
    private Button change_to_login;
    @FXML
    private ImageView eye;
    private boolean isPasswordVisible = false;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Ẩn textfield lúc ban đầu
        password_visible.setManaged(false);
        password_visible.setVisible(false);
        // Đồng bộ hai trường password
        password_visible.textProperty().bindBidirectional(password.textProperty());
        eye.setOnMouseClicked(this::togglePassWordVisibility);
        change_to_login.setOnAction(this::handleChangeToLogin);
        btn_register.setOnAction(this::handleRegister);
    }

    @FXML
    private void togglePassWordVisibility(MouseEvent event){
        isPasswordVisible =!isPasswordVisible;
        password_visible.setManaged(isPasswordVisible);
        password_visible.setVisible(isPasswordVisible);
        password.setManaged(!isPasswordVisible);
        password.setVisible(!isPasswordVisible);
    }
    @FXML
    private void handleChangeToLogin(ActionEvent event){
        try{
            FXMLLoader loader =new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) change_to_login.getScene().getWindow();
            stage.setScene(scene);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    @FXML
    private void handleRegister(ActionEvent event){
        String admin =username.getText();
        String mail = email.getText();
        String pass = password.getText();
        String role = "admin";
        String defaultAvatar = "/org/example/finaltermjava_server/Images/avatar.png";
        if(admin.isEmpty() || pass.isEmpty() || mail.isEmpty()){
            showAlert(Alert.AlertType.WARNING,"Validation Error","Please fill in all fields");
            return;
        }
        if(!isValidEmail(mail)){
            showAlert(Alert.AlertType.WARNING, "Invalid Email","Please enter a valid email address");
            return;
        }
        if(pass.length() < 6){
            showAlert(Alert.AlertType.WARNING,"Weak Password","Password must be at least 6 characters long");
            return;
        }
        //Hash password
        String hasedPassword = BCrypt.withDefaults().hashToString(12,pass.toCharArray());
        //Save to database;
        try(Connection conn = Database.getConnection()){
            //check email is existed before;
            String checkSql = "SELECT COUNT(*) FROM admin WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1,mail);
            ResultSet rs =checkStmt.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                showAlert(Alert.AlertType.ERROR,"Email Exists","Email has already existed, Please choose another one");
                return;
            }
            //check name is existed before
            String checkNameSql = "SELECT COUNT(*) admin WHERE username = ?";
            PreparedStatement checkNameStmt = conn.prepareStatement(checkNameSql);
            checkNameStmt.setString(1,admin);
            ResultSet rsName = checkNameStmt.executeQuery();
            if(rsName.next() && rsName.getInt(1) > 0){
                showAlert(Alert.AlertType.ERROR,"Username Exists","Name has already existed, Please choose another one");
                return;
            }
            //add admin into admin table
            String sql = "INSERT INTO admin(username,email,password) VALUES (?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,admin);
            stmt.setString(2,mail);
            stmt.setString(3,hasedPassword);
            stmt.executeUpdate();

            //add admin into fetchall table
            String fetchsql = "INSERT INTO fetchall(username,email,img,role) VALUES (?,?,?,?)";
            PreparedStatement fetchstmt = conn.prepareStatement(fetchsql);
            fetchstmt.setString(1,admin);
            fetchstmt.setString(2,mail);
            fetchstmt.setString(3,defaultAvatar);
            fetchstmt.setString(4,role);
            fetchstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION,"Success","Admin register successfully");
            //convert to admin form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/adminForm.fxml"));
            Scene scene = new Scene(loader.load());
            //add info in AdminFormController
            AdminFormController controller = loader.getController();
            controller.setAdminInfo(admin,defaultAvatar);

            Stage stage = (Stage) btn_register.getScene().getWindow();
            stage.setScene(scene);
            clearForm();
        }catch (SQLException e){
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not register admin.");
        } catch (IOException e){
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Change Scene Error", "Can not access to admin page");
        }

    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearForm() {
        email.clear();
        username.clear();
        password.clear();
    }

    //check input email whether suitable or not ?
    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(regex);
    }
}
