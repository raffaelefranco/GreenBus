package it.unisannio.greenbusapplication.dto;

import java.io.Serializable;

import it.unisannio.greenbusapplication.dto.internal.Coordinate;

public class StationDTO implements Serializable {

    private Integer nodeId;
    private Coordinate position;

    public StationDTO() {
    }


    public StationDTO(Integer nodeId, Coordinate position) {
        this.nodeId = nodeId;
        this.position = position;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Coordinate getPosition() {
        return position;
    }

    public void setPosition(Coordinate position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object stationDTO) {
        return this.getNodeId().equals(((StationDTO) stationDTO).getNodeId())
                && this.getPosition().getLongitude().equals(((StationDTO) stationDTO).getPosition().getLongitude())
                && this.getPosition().getLatitude().equals(((StationDTO) stationDTO).getPosition().getLatitude());
    }
}
