package org.example.finaltermjava_server.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.example.finaltermjava_server.model.Server;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    @FXML
    private ScrollPane sp_main;
    @FXML
    private ImageView choose_img;
    @FXML
    private TextField tf_message;
    @FXML
    private ImageView btn_send;
    @FXML
    private VBox vbox_list_user;
    @FXML
    private Button save_xml;
    @FXML
    private Button stop_server;
    @FXML
    private VBox vbox_messages;
    @FXML
    private ImageView audio_send;
    static boolean recording = true;

    private Server server;

    public static class ChatMessage{
        private String sender;
        private String content;
        private String type;
        private LocalDateTime timestamp;

        public ChatMessage(String sender, String content, String type) {
            this.sender = sender;
            this.content = content;
            this.type = type;
            this.timestamp = LocalDateTime.now();
        }
        //Getters
        public String getSender() {
            return sender;
        }
        public String getContent() {
            return content;
        }
        public String getType() {
            return type;
        }
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
    private List<ChatMessage> chatHistory = new ArrayList<>();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try{
            // Tạo server socket lắng nghe kết nối từ client
            server = new Server(new ServerSocket(2002),vbox_messages,this);
        }catch (IOException e){
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,"Error server" ,"Some mistakes in creating server");
        }
        vbox_messages.heightProperty().addListener(((observable, oldValue, newValue) -> {
            sp_main.setVvalue((Double) newValue);
        }));
        btn_send.setOnMouseClicked(e -> {
            sendMessage(new ActionEvent(btn_send,null));
        });
        tf_message.setOnAction(event -> sendMessage(new ActionEvent(tf_message, null)));
        choose_img.setOnMouseClicked(e -> chooseAndSendImage());
        audio_send.setOnMouseClicked(e -> handleAudioSend());
        save_xml.setOnAction(e -> saveConversationToXML());
    }

    //gửi tin nhắn từ server đến client
    public void sendMessage(ActionEvent event){
        String messageToSend = tf_message.getText();
        if(!messageToSend.isEmpty()){
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setPadding(new Insets(5,5,5,10));

            Text text = new Text(messageToSend);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-background-color : rgb(15,125,242);"+
                    "-fx-background-radius : 20px;");
            textFlow.setPadding(new Insets(5,10,5,10));
            text.setFill(Color.color(0.934,0.945,0.996));

            hBox.getChildren().add(textFlow);
            vbox_messages.getChildren().add(hBox);
            //add message to chat history
            chatHistory.add(new ChatMessage("Server", messageToSend, "text"));
            // Broadcast to all clients
            if (server != null) {
                server.broadcastMessage("Server: " + messageToSend, null);
            }
            tf_message.clear();
        }
    }
    //gửi hình ảnh từ server đến client
    public void chooseAndSendImage(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose an Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(choose_img.getScene().getWindow());
        if(selectedFile != null){
            try{
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
                server.broadcastMessage("[IMAGE] " + base64Image, null);
                addImageToVBox(base64Image, vbox_messages,Pos.CENTER_RIGHT);
                chatHistory.add(new ChatMessage("Server", "vừa gửi một ảnh", "image"));

            }catch (IOException e){
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR,"Error","Could not read image file");
            }
        }
    }
    //ghi âm và gửi âm thanh

    private  void handleAudioSend(){
        new Thread(() ->{
            try{
                recording = true;
                AudioFormat format =new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        44100.0f,
                        16,
                        2,
                        4,
                        44100.0f,
                        false
                );
                // Check if the system supports the target data line
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Audio Error", "Audio recording not supported on this system."));
                    return;
                }

                TargetDataLine microphone =(TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                JOptionPane.showMessageDialog(null, "Click OK to start recording audio");

                byte[] buffer = new byte[4096];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                Thread recordingThread = new Thread(() -> {
                    while (recording) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead == -1) break; // End of stream
                        out.write(buffer, 0, bytesRead);
                    }
                });
                recordingThread.start();
                JOptionPane.showMessageDialog(null, "Click OK to stop recording audio");

                recording = false;
                microphone.stop();
                microphone.close();
                recordingThread.join();
                //Encode to Base64
                String audioBase64 = Base64.getEncoder().encodeToString(out.toByteArray());
                String messageToSend = "[AUDIO] " + audioBase64;

                if (server != null){
                    server.broadcastMessage(messageToSend, null);
                    addAudioToVBox(audioBase64, vbox_messages, Pos.CENTER_RIGHT);
                    chatHistory.add(new ChatMessage("Server", "vừa gửi một recording", "audio"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Audio Error", "Could not record audio."));
            }
        }).start();
    }
    // Lưu cuộc trò chuyện client vào file XML
    public void addClientMessage(String message , String type){
        String content;
        switch (type){
            case "image":
                content = "vừa gửi một ảnh";
                break;

            case "audio":
                content = "vừa gửi một recording";
                break;
            default:
                content = message;
                break;
        }
        chatHistory.add(new ChatMessage("Client", content, type));

    }
    private void saveConversationToXML(){
        try{
            // Tạo để cấu hình
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            // Tạo document gốc
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("conversation");
            doc.appendChild(rootElement);
            // Thêm các tin nhắn vào xml
            for (ChatMessage message : chatHistory){
                Element messageElement;
                if(message.getSender().equals("Server")){
                    messageElement = doc.createElement("server");
                } else {
                    messageElement = doc.createElement("client");
                }
                messageElement.setTextContent(message.getContent());

                messageElement.setAttribute("timestamp", message.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                messageElement.setAttribute("type", message.getType());
                rootElement.appendChild(messageElement);



            }
            //luư file xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            //Chọn nơi lưu file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Conversation");
            fileChooser.setInitialFileName("conversation_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xml");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

            File file = fileChooser.showSaveDialog(save_xml.getScene().getWindow());
            if(file != null){
                StreamResult result = new StreamResult(file);
                transformer.transform(source,result);

                showAlert(Alert.AlertType.INFORMATION, "Save Conversation", "Conversation saved successfully to " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save conversation: " + e.getMessage());
        }
    }
    public static void addAudioToVBox(String base64Audio, VBox vBox, Pos alignment) {
        Platform.runLater(() -> {
            try {
                // Create a simple audio indicator/button
                HBox hBox = new HBox();
                hBox.setAlignment(alignment);
                hBox.setPadding(new Insets(5, 5, 5, 10));

                Button audioButton = new Button("🔊 Play Audio");
                audioButton.setStyle("-fx-background-color: lightblue; -fx-background-radius: 20px;");
                audioButton.setOnAction(e -> playAudio(base64Audio));

                hBox.getChildren().add(audioButton);
                vBox.getChildren().add(hBox);

            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Audio Error", "Could not display audio message."));
            }
        });
    }
    private static void playAudio(String base64Audio) {
        new Thread(() -> {
            try {
                byte[] audioBytes = Base64.getDecoder().decode(base64Audio);

                // Create audio input stream
                ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
                AudioFormat format =new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        44100.0f,
                        16,
                        2,
                        4,
                        44100.0f,
                        false
                );
                AudioInputStream audioStream = new AudioInputStream(byteStream, format, audioBytes.length);

                // Play the audio
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();

            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Playback Error", "Could not play audio: " + e.getMessage()));
            }
        }).start();
    }
    //hiển thị hình ảnh từ base64 string
    public static void addImageToVBox(String base64Image, VBox vBox, Pos alignment) {
        Platform.runLater(() -> {
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
                Image image = new Image(new ByteArrayInputStream(imageBytes));

                if(image.isError()){
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Image Error", "Could not decode image data."));
                    return;
                }
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);

                HBox hBox = new HBox(imageView);
                hBox.setAlignment(alignment);
                hBox.setPadding(new Insets(5, 5, 5, 10));

                vBox.getChildren().add(hBox);
            }catch (IllegalArgumentException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Image Error", "Invalid base64 image data."));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred while displaying the image."));
            }
        });
    }
    //Demonstrate text from client
    public static void addLabel(String messageFromClient , VBox vBox){
        Platform.runLater(() -> {
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(5,5,5,10));

            Text text = new Text(messageFromClient);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-background-color: rgb(233,233,235);" +
                    "-fx-background-radius: 20px;");
            textFlow.setPadding(new Insets(5,10,5,10));
            hBox.getChildren().add(textFlow);
            vBox.getChildren().add(hBox);
        });
    }
    // class để dùng lưu trữ cuộc trò chuyện giữa client và server

    private static void showAlert(Alert.AlertType type, String title, String content){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
