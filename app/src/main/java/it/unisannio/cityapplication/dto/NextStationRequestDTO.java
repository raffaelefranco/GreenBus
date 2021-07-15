package it.unisannio.cityapplication.dto;

import java.io.Serializable;

public class NextStationRequestDTO implements Serializable {

    private StationDTO currentStation;

    public NextStationRequestDTO() {
    }

    public NextStationRequestDTO(StationDTO currentStation) {
        this.currentStation = currentStation;
    }

    public StationDTO getCurrentStation() {
        return currentStation;
    }

    public void setCurrentStation(StationDTO currentStation) {
        this.currentStation = currentStation;
    }
}
