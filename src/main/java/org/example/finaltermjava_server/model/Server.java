package org.example.finaltermjava_server.model;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import org.example.finaltermjava_server.controller.ServerController;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private VBox vboxMessages;
    private ServerController controller;
    public Server(ServerSocket serverSocket, VBox vboxMessages){
        this.serverSocket = serverSocket;
        this.vboxMessages = vboxMessages;
        startServer();
    }
    public Server(ServerSocket serverSocket, VBox vboxMessages, ServerController controller){
        this.serverSocket = serverSocket;
        this.vboxMessages = vboxMessages;
        this.controller = controller; // FIX: Khởi tạo controller reference
        startServer();
    }

    public void startServer(){
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    Platform.runLater(() ->showAlert(Alert.AlertType.INFORMATION,"Join room","Client connected"));
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,"Error Server","Some mistakes in starting server"));
            }
        }).start();
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    private void showAlert(Alert.AlertType type , String title , String content){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class for handling each client
    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private Server server;

        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
            try {
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                closeEverything();
            }
        }

        @Override
        public void run() {
            String messageFromClient;
            while (socket != null && socket.isConnected()) {
                try {
                    messageFromClient = bufferedReader.readLine();
                    if (messageFromClient != null) {
                        // Broadcast to other clients
                        server.broadcastMessage(messageFromClient, this);
                        // Update server UI with client message
                        if(messageFromClient.startsWith("[IMAGE]")){
                            String base64Image = messageFromClient.substring(7);
                            ServerController.addImageToVBox(base64Image, server.vboxMessages, javafx.geometry.Pos.CENTER_LEFT);
                            server.controller.addClientMessage("","image");

                        }else if(messageFromClient.startsWith("[AUDIO]")) {
                            String base64Audio = messageFromClient.substring(7);
                            ServerController.addAudioToVBox(base64Audio, server.vboxMessages, javafx.geometry.Pos.CENTER_LEFT);
                            server.controller.addClientMessage("","audio");

                        }
                        else {
                            ServerController.addLabel(messageFromClient, server.vboxMessages);

                            server.controller.addClientMessage(messageFromClient, "text");

                        }

                    }
                } catch (IOException e) {
                    closeEverything();
                    break;
                }
            }
        }

        public void sendMessage(String message) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything();
            }
        }

        private void closeEverything() {
            server.removeClient(this);
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
