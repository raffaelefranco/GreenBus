package it.unisannio.cityapplication.dto;

import java.io.Serializable;

public class TicketDTO implements Serializable {

    private String oneTimeTicket;

    public TicketDTO() {
    }

    public TicketDTO(String oneTimeTicket) {
        this.oneTimeTicket = oneTimeTicket;
    }

    public String getOneTimeTicket() {
        return oneTimeTicket;
    }

    public void setOneTimeTicket(String oneTimeTicket) {
        this.oneTimeTicket = oneTimeTicket;
    }
}
