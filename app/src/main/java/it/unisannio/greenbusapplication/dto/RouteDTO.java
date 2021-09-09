package it.unisannio.greenbusapplication.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RouteDTO implements Serializable {


    private String id;
    private String name;
    private List<StationDTO> stations;
    private Map<String, List<StationDTO>> reachableRoutes;

    public RouteDTO() {}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StationDTO> getStations() {
        return stations;
    }

    public void setStations(List<StationDTO> stations) {
        this.stations = stations;
    }

    public Map<String, List<StationDTO>> getReachableRoutes() {
        return reachableRoutes;
    }

    public void setReachableRoutes(Map<String, List<StationDTO>> reachableRoutes) {
        this.reachableRoutes = reachableRoutes;
    }
}
