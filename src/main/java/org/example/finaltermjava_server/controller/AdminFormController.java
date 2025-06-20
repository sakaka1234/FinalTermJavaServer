package org.example.finaltermjava_server.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;

import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.finaltermjava_server.model.Database;
import org.example.finaltermjava_server.model.Ticket;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class AdminFormController implements Initializable {
    @FXML
    private Button open_server;
    @FXML
    private ImageView admin_img;
    @FXML
    private Label admin_name;
    @FXML private TableView<Ticket> ticketTable;

    @FXML private TableColumn<Ticket, String> col_name;
    @FXML private TableColumn<Ticket, String> col_departure;
    @FXML private TableColumn<Ticket, String> col_arrival;
    @FXML private TableColumn<Ticket, String> col_date;
    @FXML private TableColumn<Ticket, String> col_origin;
    @FXML private TableColumn<Ticket, String> col_destination;
    @FXML private TableColumn<Ticket, String> col_coach;
    @FXML private TableColumn<Ticket, String> col_seat;
    @FXML private TableColumn<Ticket, Integer> col_price;
    private ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    @FXML private ComboBox departure;
    @FXML private ComboBox arrival;
    @FXML private TextField price;
    @FXML private TextField coach;
    @FXML private Button update;
    @FXML private Button delete;
    @FXML private Button bill;
    @FXML private ImageView reload_icon;
    @FXML private ImageView camera;
    @FXML private ImageView logout;
    @FXML private PieChart pie_chart;
    @FXML private BarChart bar_chart;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        open_server.setOnAction(this::changeSceneToServer);
        update.setOnAction(e -> updateTicket());
        delete.setOnAction(e -> deleteTicket());
        bill.setOnAction(e -> generateTicketPdf());
        //Match col from table view with table tickettrain in db
        col_name.setCellValueFactory(new PropertyValueFactory<>("username"));
        col_departure.setCellValueFactory(new PropertyValueFactory<>("departure"));
        col_arrival.setCellValueFactory(new PropertyValueFactory<>("arrival"));
        col_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        col_origin.setCellValueFactory(new PropertyValueFactory<>("origin")); // Fixed: orign -> origin
        col_destination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        col_coach.setCellValueFactory(new PropertyValueFactory<>("coach"));
        col_seat.setCellValueFactory(new PropertyValueFactory<>("seat"));
        col_price.setCellValueFactory(new PropertyValueFactory<>("price"));
        loadTicketsFromDatabase();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                departure.getItems().add(time);
                arrival.getItems().add(time);
            }
        }
        ticketTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                departure.setValue(newSelection.getDeparture());
                arrival.setValue(newSelection.getArrival());
                coach.setText(newSelection.getCoach());
                price.setText(String.valueOf(newSelection.getPrice()));
            }
        });
        reload_icon.setOnMouseClicked(event -> loadTicketsFromDatabase());

        logout.setOnMouseClicked(event -> {
            try {
                logOut();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        camera.setOnMouseClicked(e -> chooseImage());
    }

    @FXML
    private void changeSceneToServer(ActionEvent event){
        try{
            FXMLLoader loader =new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/server.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) open_server.getScene().getWindow();
            stage.setScene(scene);

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadTicketsFromDatabase() {
        ticketList.clear(); // Clear existing data before loading new data

        String sql = "SELECT * FROM trainticket";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {



            while (rs.next()) {


                String username = rs.getString("username") != null ? rs.getString("username") : "";
                String departure = rs.getTime("departure") != null ? rs.getTime("departure").toString() : "";
                String arrival = rs.getTime("arrival") != null ? rs.getTime("arrival").toString() : "";
                String origin = rs.getString("origin") != null ? rs.getString("origin") : "";
                String destination = rs.getString("destination") != null ? rs.getString("destination") : "";
                String coach = rs.getString("coach") != null ? rs.getString("coach") : "";
                String seat = rs.getString("seat") != null ? rs.getString("seat") : "";
                String date = rs.getDate("date") != null ? rs.getDate("date").toString() : "";
                int price = rs.getObject("price") != null ? rs.getInt("price") : 0;

                Ticket ticket = new Ticket(username, departure, arrival, origin, destination, coach, seat, date, price);
                ticketList.add(ticket);


            }



            // Refresh the table view
            Platform.runLater(() -> {
                ticketTable.setItems(ticketList);
                ticketTable.refresh();
                loadChartData();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load ticket data: " + e.getMessage());
        }
    }
    private void updateTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No ticket selected", "Please select a ticket to update.");
            return;
        }

        String newDeparture = (String) departure.getValue();
        String newArrival = (String) arrival.getValue();
        String newCoach = coach.getText();
        String newPriceStr = price.getText();

        if (newDeparture == null || newArrival == null || newCoach.isEmpty() || newPriceStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing fields", "Please fill in all required fields.");
            return;
        }

        try {
            int newPrice = Integer.parseInt(newPriceStr);

            String sql = "UPDATE trainticket SET departure = ?, arrival = ?, coach = ?, price = ? WHERE username = ? AND origin = ? AND destination = ? AND seat = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTime(1, java.sql.Time.valueOf(newDeparture + ":00"));
                stmt.setTime(2, java.sql.Time.valueOf(newArrival + ":00"));
                stmt.setString(3, newCoach);
                stmt.setInt(4, newPrice);
                stmt.setString(5, selected.getUsername());
                stmt.setString(6,selected.getOrigin());
                stmt.setString(7,selected.getDestination());
                stmt.setString(8,selected.getSeat());
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Ticket updated successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Update failed", "No ticket was updated.");
                }
            }

            loadTicketsFromDatabase(); // Refresh table
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Update Error", "Failed to update ticket: " + e.getMessage());
        }
    }

    private void deleteTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No ticket selected", "Please select a ticket to delete.");
            return;
        }

        String sql = "DELETE FROM trainticket WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, selected.getUsername());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Ticket deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete failed", "No ticket was deleted.");
            }

            loadTicketsFromDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Delete Error", "Failed to delete ticket: " + e.getMessage());
        }
    }


    public void setAdminInfo(String name, String imgPath){
        admin_name.setText(name);
        try {
            Image image;
            if (imgPath.startsWith("/")) { // đường dẫn resource
                image = new Image(getClass().getResourceAsStream(imgPath));
            } else {
                File file = new File(imgPath);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                } else {
                    throw new IOException("Image file not found");
                }
            }
            admin_img.setImage(image);
        }catch (Exception e){
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,"Error in setting info for admin","Some thing go wrong here"));
            e.printStackTrace();
        }
    }
    private BufferedImage generateQRCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            return image;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void generateTicketPdf() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No ticket selected", "Please select a ticket to generate the PDF.");
            return;
        }

        Document document = new Document();
        try {
            String baseDir = "src/main/resources/org/example/finaltermjava_server/pdf";
            File pdfDir = new File(baseDir);
            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "invoice_" + selected.getUsername() + "_" + timestamp + ".pdf";
            String pdfPath = baseDir + "/" + fileName;

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Train Ticket Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15f);
            document.add(title);

            Font timeFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.GRAY);
            Paragraph timeInfo = new Paragraph("Generated at: " +
                    java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                    timeFont);
            timeInfo.setAlignment(Element.ALIGN_CENTER);
            timeInfo.setSpacingAfter(20f);
            document.add(timeInfo);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);

            table.addCell("Username");
            table.addCell(selected.getUsername());
            table.addCell("Departure Time");
            table.addCell(selected.getDeparture());
            table.addCell("Arrival Time");
            table.addCell(selected.getArrival());
            table.addCell("Date");
            table.addCell(selected.getDate());
            table.addCell("Origin");
            table.addCell(selected.getOrigin());
            table.addCell("Destination");
            table.addCell(selected.getDestination());
            table.addCell("Coach");
            table.addCell(selected.getCoach());
            table.addCell("Seat");
            table.addCell(selected.getSeat());
            table.addCell("Price");
            table.addCell(String.valueOf(selected.getPrice()));

            document.add(table);

            // QR Code chỉ cho 1 vé
            String qrContent = String.format("Ticket for %s\nFrom %s to %s on %s\nCoach: %s, Seat: %s, Price: %d",
                    selected.getUsername(),
                    selected.getOrigin(),
                    selected.getDestination(),
                    selected.getDate(),
                    selected.getCoach(),
                    selected.getSeat(),
                    selected.getPrice());

            BufferedImage qrImage = generateQRCodeImage(qrContent, 150, 150);
            if (qrImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "png", baos);
                com.itextpdf.text.Image qrPdfImg = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                qrPdfImg.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                qrPdfImg.setSpacingBefore(20f);
                document.add(qrPdfImg);
            }

            document.close();
            writer.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Created",
                    "Invoice has been generated for " + selected.getUsername() + ":\n" + pdfPath);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "PDF Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private void chooseImage(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose an image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image files","*.png","*.jpeg","*.jpg","*.gif"));
        File selectedFile = fileChooser.showOpenDialog(admin_img.getScene().getWindow());
        if(selectedFile != null){
            try(Connection conn = Database.getConnection()){
                String imagePath = selectedFile.getAbsolutePath();
                Image image = new Image(selectedFile.toURI().toString());
                admin_img.setImage(image);
                makeImageViewCircular(admin_img);

                String sql = "UPDATE fetchall SET img = ? WHERE username = ? and role = 'admin'";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1,imagePath);
                stmt.setString(2,admin_name.getText());
                stmt.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Avatar updated successfully.");
            }catch (SQLException e){
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update avatar path.");
            }catch (Exception e){
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Image Error", "Failed to load image.");
            }
        }
    }
    private void makeImageViewCircular(ImageView imageView){
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle clip = new Circle(imageView.getFitWidth() / 2, imageView.getFitHeight() / 2, radius);
        imageView.setClip(clip);
    }
    private void logOut() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/finaltermjava_server/login.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) logout.getScene().getWindow();
        stage.setScene(scene);
    }
    private void loadChartData() {
        pie_chart.getData().clear();
        bar_chart.getData().clear();

        String[] cities = {"Da Nang", "Hai Phong", "Ha Noi", "Sai Gon", "Hue"};
        String[] hexColors = {"#FFA500", "#1E90FF", "#32CD32", "#DC143C", "#800080"}; // ORANGE, BLUE, GREEN, RED, PURPLE

        int[] originCounts = new int[cities.length];
        int[] destinationCounts = new int[cities.length];

        // Đếm số lượng từ ticketList
        for (Ticket ticket : ticketList) {
            for (int i = 0; i < cities.length; i++) {
                if (ticket.getOrigin().equalsIgnoreCase(cities[i])) {
                    originCounts[i]++;
                }
                if (ticket.getDestination().equalsIgnoreCase(cities[i])) {
                    destinationCounts[i]++;
                }
            }
        }

        // PieChart: Origin
        for (int i = 0; i < cities.length; i++) {
            PieChart.Data data = new PieChart.Data(cities[i], originCounts[i]);
            pie_chart.getData().add(data);
        }

        // Gán màu HEX cho từng phần của PieChart
        for (int i = 0; i < pie_chart.getData().size(); i++) {
            final PieChart.Data data = pie_chart.getData().get(i);
            final String color = hexColors[i];
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }

        // BarChart: Destination
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Destination");

        for (int i = 0; i < cities.length; i++) {
            javafx.scene.chart.XYChart.Data<String, Number> data = new javafx.scene.chart.XYChart.Data<>(cities[i], destinationCounts[i]);
            series.getData().add(data);
        }

        bar_chart.getData().add(series);

        // Gán màu HEX cho từng cột của BarChart
        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                final javafx.scene.chart.XYChart.Data<String, Number> chartData = series.getData().get(i);
                final String color = hexColors[i];
                if (chartData.getNode() != null) {
                    chartData.getNode().setStyle("-fx-bar-fill: " + color + ";");
                } else {
                    chartData.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle("-fx-bar-fill: " + color + ";");
                        }
                    });
                }
            }
        });


        // Cập nhật tiêu đề biểu đồ
        pie_chart.setTitle("Ticket Origins");
        bar_chart.setTitle("Ticket Destinations");
        bar_chart.setLegendVisible(false);

    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}