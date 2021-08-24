package it.unisannio.greenbusapplication.dto;

import java.io.Serializable;

public class TripRequestDTO implements Serializable {

    private Integer osmidSource;
    private Integer osmidDestination;

    public TripRequestDTO() {
    }

    public TripRequestDTO(Integer osmidSource, Integer osmidDestination) {
        this.osmidSource = osmidSource;
        this.osmidDestination = osmidDestination;
    }

    public Integer getOsmidSource() {
        return osmidSource;
    }

    public void setOsmidSource(Integer osmidSource) {
        this.osmidSource = osmidSource;
    }

    public Integer getOsmidDestination() {
        return osmidDestination;
    }

    public void setOsmidDestination(Integer osmidDestination) {
        this.osmidDestination = osmidDestination;
    }

    @Override
    public String toString() {
        return "TripRequestDTO{" +
                "osmidSource=" + osmidSource +
                ", osmidDestination=" + osmidDestination +
                '}';
    }
}

