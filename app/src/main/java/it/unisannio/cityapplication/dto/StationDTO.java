package it.unisannio.cityapplication.dto;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class StationDTO implements Serializable {

    private Integer nodeId;
    private Double latitude;
    private Double longitude;

    public StationDTO() {
    }

    public StationDTO(Integer nodeId, Double latitude, Double longitude) {
        this.nodeId = nodeId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object stationDTO) {
        return this.getNodeId().equals(((StationDTO) stationDTO).getNodeId())
                && this.getLongitude().equals(((StationDTO) stationDTO).getLongitude())
                && this.getLatitude().equals(((StationDTO) stationDTO).getLatitude());
    }

}
