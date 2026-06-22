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
import java.text.NumberFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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

    @FXML private TableColumn<Ticket, String> col_ticket_id;
    @FXML private TableColumn<Ticket, String> col_name;
    @FXML private TableColumn<Ticket, String> col_departure;
    @FXML private TableColumn<Ticket, String> col_arrival;
    @FXML private TableColumn<Ticket, String> col_date;
    @FXML private TableColumn<Ticket, String> col_origin;
    @FXML private TableColumn<Ticket, String> col_destination;
    @FXML private TableColumn<Ticket, String> col_coach;
    @FXML private TableColumn<Ticket, String> col_seat;
    @FXML private TableColumn<Ticket, Integer> col_price;
    @FXML private TableColumn<Ticket, String> col_status;
    private ObservableList<Ticket> ticketList = FXCollections.observableArrayList();
    private ObservableList<Ticket> filteredTicketList = FXCollections.observableArrayList();

    @FXML private ComboBox departure;
    @FXML private ComboBox arrival;
    @FXML private ComboBox status;
    @FXML private TextField filter_keyword;
    @FXML private TextField filter_origin;
    @FXML private TextField filter_destination;
    @FXML private TextField filter_date;
    @FXML private ComboBox filter_status;
    @FXML private Button filter_apply;
    @FXML private Button filter_clear;
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
    @FXML private Label total_revenue;
    @FXML private Label paid_ticket_count;
    @FXML private Label average_revenue;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        open_server.setOnAction(this::changeSceneToServer);
        update.setOnAction(e -> updateTicket());
        delete.setOnAction(e -> deleteTicket());
        bill.setOnAction(e -> generateTicketPdf());
        //Match col from table view with table tickettrain in db
        col_ticket_id.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        col_name.setCellValueFactory(new PropertyValueFactory<>("username"));
        col_departure.setCellValueFactory(new PropertyValueFactory<>("departure"));
        col_arrival.setCellValueFactory(new PropertyValueFactory<>("arrival"));
        col_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        col_origin.setCellValueFactory(new PropertyValueFactory<>("origin")); // Fixed: orign -> origin
        col_destination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        col_coach.setCellValueFactory(new PropertyValueFactory<>("coach"));
        col_seat.setCellValueFactory(new PropertyValueFactory<>("seat"));
        col_price.setCellValueFactory(new PropertyValueFactory<>("price"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadTicketsFromDatabase();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                departure.getItems().add(time);
                arrival.getItems().add(time);
            }
        }
        status.getItems().addAll("BOOKED", "PAID", "CANCELLED", "USED");
        filter_status.getItems().addAll("ALL", "BOOKED", "PAID", "CANCELLED", "USED");
        filter_status.setValue("ALL");
        filter_apply.setOnAction(e -> applyTicketFilter());
        filter_clear.setOnAction(e -> clearTicketFilter());
        filter_keyword.setOnAction(e -> applyTicketFilter());
        filter_origin.setOnAction(e -> applyTicketFilter());
        filter_destination.setOnAction(e -> applyTicketFilter());
        filter_date.setOnAction(e -> applyTicketFilter());
        filter_status.setOnAction(e -> applyTicketFilter());
        ticketTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                departure.setValue(newSelection.getDeparture());
                arrival.setValue(newSelection.getArrival());
                coach.setText(newSelection.getCoach());
                price.setText(String.valueOf(newSelection.getPrice()));
                status.setValue(newSelection.getStatus());
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
        ensureTicketBusinessColumns();

        String sql = "SELECT * FROM trainticket";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {



            while (rs.next()) {


                String ticketId = rs.getString("ticket_id") != null ? rs.getString("ticket_id") : "";
                String username = rs.getString("username") != null ? rs.getString("username") : "";
                String departure = rs.getTime("departure") != null ? rs.getTime("departure").toString() : "";
                String arrival = rs.getTime("arrival") != null ? rs.getTime("arrival").toString() : "";
                String origin = rs.getString("origin") != null ? rs.getString("origin") : "";
                String destination = rs.getString("destination") != null ? rs.getString("destination") : "";
                String coach = rs.getString("coach") != null ? rs.getString("coach") : "";
                String seat = rs.getString("seat") != null ? rs.getString("seat") : "";
                String date = rs.getDate("date") != null ? rs.getDate("date").toString() : "";
                int price = rs.getObject("price") != null ? rs.getInt("price") : 0;
                String status = rs.getString("status") != null ? rs.getString("status") : "BOOKED";

                Ticket ticket = new Ticket(ticketId, username, departure, arrival, origin, destination, coach, seat, date, price, status);
                ticketList.add(ticket);


            }



            // Refresh the table view
            Platform.runLater(() -> {
                applyTicketFilter();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load ticket data: " + e.getMessage());
        }
    }

    private void applyTicketFilter() {
        String keyword = normalizeFilterText(filter_keyword.getText());
        String origin = normalizeFilterText(filter_origin.getText());
        String destination = normalizeFilterText(filter_destination.getText());
        String date = normalizeFilterText(filter_date.getText());
        String selectedStatus = filter_status.getValue() == null ? "ALL" : filter_status.getValue().toString();

        filteredTicketList.setAll(ticketList.stream()
                .filter(ticket -> keyword.isEmpty()
                        || containsIgnoreCase(ticket.getTicketId(), keyword)
                        || containsIgnoreCase(ticket.getUsername(), keyword))
                .filter(ticket -> origin.isEmpty() || containsIgnoreCase(ticket.getOrigin(), origin))
                .filter(ticket -> destination.isEmpty() || containsIgnoreCase(ticket.getDestination(), destination))
                .filter(ticket -> date.isEmpty() || containsIgnoreCase(ticket.getDate(), date))
                .filter(ticket -> "ALL".equals(selectedStatus) || selectedStatus.equalsIgnoreCase(ticket.getStatus()))
                .toList());

        ticketTable.setItems(filteredTicketList);
        ticketTable.refresh();
        loadChartData();
    }

    private void clearTicketFilter() {
        filter_keyword.clear();
        filter_origin.clear();
        filter_destination.clear();
        filter_date.clear();
        filter_status.setValue("ALL");
        applyTicketFilter();
    }

    private String normalizeFilterText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private void ensureTicketBusinessColumns() {
        try (Connection conn = Database.getConnection()) {
            addColumnIfMissing(conn, "ticket_id", "VARCHAR(36)");
            addColumnIfMissing(conn, "status", "VARCHAR(20) DEFAULT 'BOOKED'");

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE trainticket SET ticket_id = UUID() WHERE ticket_id IS NULL OR ticket_id = ''")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE trainticket SET status = 'BOOKED' WHERE status IS NULL OR status = ''")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to prepare ticket business columns: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Connection conn, String columnName, String columnDefinition) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainticket' AND COLUMN_NAME = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, columnName);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement alterStmt = conn.prepareStatement(
                            "ALTER TABLE trainticket ADD COLUMN " + columnName + " " + columnDefinition)) {
                        alterStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private void updateTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No ticket selected", "Please select a ticket to update.");
            return;
        }
        if (selected.getTicketId() == null || selected.getTicketId().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing ticket ID", "Please reload the ticket list and try again.");
            return;
        }

        String newDeparture = (String) departure.getValue();
        String newArrival = (String) arrival.getValue();
        String newCoach = coach.getText();
        String newPriceStr = price.getText();
        String newStatus = (String) status.getValue();

        if (newDeparture == null || newArrival == null || newCoach.isEmpty() || newPriceStr.isEmpty() || newStatus == null) {
            showAlert(Alert.AlertType.WARNING, "Missing fields", "Please fill in all required fields.");
            return;
        }

        try {
            int newPrice = Integer.parseInt(newPriceStr);
            if (newPrice < 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid price", "Price must not be negative.");
                return;
            }

            String sql = "UPDATE trainticket SET departure = ?, arrival = ?, coach = ?, price = ?, status = ? WHERE ticket_id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTime(1, toSqlTime(newDeparture));
                stmt.setTime(2, toSqlTime(newArrival));
                stmt.setString(3, newCoach);
                stmt.setInt(4, newPrice);
                stmt.setString(5, newStatus);
                stmt.setString(6, selected.getTicketId());
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
        if (selected.getTicketId() == null || selected.getTicketId().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing ticket ID", "Please reload the ticket list and try again.");
            return;
        }

        String sql = "DELETE FROM trainticket WHERE ticket_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, selected.getTicketId());

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

    private java.sql.Time toSqlTime(String value) {
        String normalized = value.trim();
        if (normalized.length() == 5) {
            normalized += ":00";
        }
        return java.sql.Time.valueOf(normalized);
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
        if (selected.getTicketId() == null || selected.getTicketId().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing ticket ID", "Please reload the ticket list and try again.");
            return;
        }
        if (!"PAID".equalsIgnoreCase(selected.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Ticket is not paid", "Only paid tickets can be printed.");
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
            String safeTicketId = selected.getTicketId().replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "invoice_" + safeTicketId + "_" + selected.getUsername() + "_" + timestamp + ".pdf";
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

            table.addCell("Ticket ID");
            table.addCell(selected.getTicketId());
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
            table.addCell("Status");
            table.addCell(selected.getStatus());

            document.add(table);

            // QR Code chỉ cho 1 vé
            String qrContent = String.format("Ticket ID: %s\nTicket for %s\nFrom %s to %s on %s\nCoach: %s, Seat: %s, Price: %d, Status: %s",
                    selected.getTicketId(),
                    selected.getUsername(),
                    selected.getOrigin(),
                    selected.getDestination(),
                    selected.getDate(),
                    selected.getCoach(),
                    selected.getSeat(),
                    selected.getPrice(),
                    selected.getStatus());

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
        ObservableList<Ticket> chartSource = ticketTable.getItems() == null ? ticketList : ticketTable.getItems();
        Map<String, Integer> revenueByRoute = new LinkedHashMap<>();
        int totalRevenueValue = 0;
        int paidTicketValue = 0;

        // Đếm số lượng từ ticketList
        for (Ticket ticket : chartSource) {
            for (int i = 0; i < cities.length; i++) {
                if (ticket.getOrigin().equalsIgnoreCase(cities[i])) {
                    originCounts[i]++;
                }
            }

            if (isRevenueTicket(ticket)) {
                totalRevenueValue += ticket.getPrice();
                paidTicketValue++;
                String route = ticket.getOrigin() + " - " + ticket.getDestination();
                revenueByRoute.merge(route, ticket.getPrice(), Integer::sum);
            }
        }
        updateRevenueSummary(totalRevenueValue, paidTicketValue);

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

        // BarChart: revenue by route
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Revenue");

        for (Map.Entry<String, Integer> entry : revenueByRoute.entrySet()) {
            javafx.scene.chart.XYChart.Data<String, Number> data = new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue());
            series.getData().add(data);
        }

        bar_chart.getData().add(series);

        // Gán màu HEX cho từng cột của BarChart
        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                final javafx.scene.chart.XYChart.Data<String, Number> chartData = series.getData().get(i);
                final String color = hexColors[i % hexColors.length];
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
        bar_chart.setTitle("Revenue by Route");
        bar_chart.setLegendVisible(false);

    }

    private boolean isRevenueTicket(Ticket ticket) {
        return "PAID".equalsIgnoreCase(ticket.getStatus());
    }

    private void updateRevenueSummary(int totalRevenueValue, int paidTicketValue) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        total_revenue.setText(currencyFormat.format(totalRevenueValue) + " VND");
        paid_ticket_count.setText(String.valueOf(paidTicketValue));
        int averageValue = paidTicketValue == 0 ? 0 : totalRevenueValue / paidTicketValue;
        average_revenue.setText(currencyFormat.format(averageValue) + " VND");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
