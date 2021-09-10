package it.unisannio.greenbusapplication.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RouteDTO implements Serializable {


    private String id;
    private List<StationDTO> stations;

    public RouteDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<StationDTO> getStations() {
        return stations;
    }

    public void setStations(List<StationDTO> stations) {
        this.stations = stations;
    }

}
