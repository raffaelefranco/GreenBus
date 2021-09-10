package it.unisannio.greenbusapplication.dto.internal;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

public class TripDTO implements Serializable {

    private static final long serialVersionUID = 6794687963821239314L;

    private String id;
    private Integer source;
    private Integer destination;
    private Date requestDate;
    private Integer vehicleId;

    public TripDTO() {}

    public TripDTO(String id, Integer source, Integer destination, Date requestDate, Integer vehicleId) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.requestDate = requestDate;
        this.vehicleId = vehicleId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

}

