package it.unisannio.cityapplication.dto;

import java.io.Serializable;
import java.util.List;

import it.unisannio.cityapplication.dto.internal.Coordinate;

public class NextStationDTO implements Serializable {

    private StationDTO nextStation;
    private List<Coordinate> minPath;

    public NextStationDTO() { }

    public NextStationDTO(StationDTO nextStation) {
        this.nextStation = nextStation;
    }

    public NextStationDTO(StationDTO nextStation, List<Coordinate> minPath) {
        this.nextStation = nextStation;
        this.minPath = minPath;
    }

    public StationDTO getNextStation() {
        return nextStation;
    }

    public void setNextStation(StationDTO nextStation) {
        this.nextStation = nextStation;
    }

    public List<Coordinate> getMinPath() {
        return minPath;
    }

    public void setMinPath(List<Coordinate> minPath) {
        this.minPath = minPath;
    }
}