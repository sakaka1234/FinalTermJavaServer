package org.example.finaltermjava_server.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Ticket {
    private final SimpleStringProperty ticketId;
    private final SimpleStringProperty username;
    private final SimpleStringProperty departure;
    private final SimpleStringProperty arrival;
    private final SimpleStringProperty origin;
    private final SimpleStringProperty destination;
    private final SimpleStringProperty coach;
    private final SimpleStringProperty seat;
    private final SimpleStringProperty date;
    private final SimpleIntegerProperty price;
    private final SimpleStringProperty status;

    // Constructor with String parameters (easier to use)
    public Ticket(String ticketId, String username, String departure, String arrival, String origin,
                  String destination, String coach, String seat, String date, int price, String status) {
        this.ticketId = new SimpleStringProperty(ticketId);
        this.username = new SimpleStringProperty(username);
        this.departure = new SimpleStringProperty(departure);
        this.arrival = new SimpleStringProperty(arrival);
        this.origin = new SimpleStringProperty(origin);
        this.destination = new SimpleStringProperty(destination);
        this.coach = new SimpleStringProperty(coach);
        this.seat = new SimpleStringProperty(seat);
        this.date = new SimpleStringProperty(date);
        this.price = new SimpleIntegerProperty(price);
        this.status = new SimpleStringProperty(status);
    }

    // Constructor with Property parameters (for backward compatibility)
    public Ticket(SimpleStringProperty ticketId, SimpleStringProperty username, SimpleStringProperty departure, SimpleStringProperty arrival,
                  SimpleStringProperty origin, SimpleStringProperty destination, SimpleStringProperty coach,
                  SimpleStringProperty seat, SimpleStringProperty date, SimpleIntegerProperty price, SimpleStringProperty status) {
        this.ticketId = ticketId;
        this.username = username;
        this.departure = departure;
        this.arrival = arrival;
        this.origin = origin;
        this.destination = destination;
        this.coach = coach;
        this.seat = seat;
        this.date = date;
        this.price = price;
        this.status = status;
    }

    public String getTicketId() {
        return ticketId.get();
    }

    public SimpleStringProperty ticketIdProperty() {
        return ticketId;
    }

    public String getUsername() {
        return username.get();
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public String getDeparture() {
        return departure.get();
    }

    public SimpleStringProperty departureProperty() {
        return departure;
    }

    public String getArrival() {
        return arrival.get();
    }

    public SimpleStringProperty arrivalProperty() {
        return arrival;
    }

    public String getOrigin() {
        return origin.get();
    }

    public SimpleStringProperty originProperty() {
        return origin;
    }

    public String getDestination() {
        return destination.get();
    }

    public SimpleStringProperty destinationProperty() {
        return destination;
    }

    public String getCoach() {
        return coach.get();
    }

    public SimpleStringProperty coachProperty() {
        return coach;
    }

    public String getSeat() {
        return seat.get();
    }

    public SimpleStringProperty seatProperty() {
        return seat;
    }

    public String getDate() {
        return date.get();
    }

    public SimpleStringProperty dateProperty() {
        return date;
    }

    public int getPrice() {
        return price.get();
    }

    public SimpleIntegerProperty priceProperty() {
        return price;
    }

    public String getStatus() {
        return status.get();
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }
}
