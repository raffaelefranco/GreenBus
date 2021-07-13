package it.unisannio.cityapplication.dto;

import java.io.Serializable;

public class JWTTokenDTO implements Serializable {

    private String jwt;

    public JWTTokenDTO() { }

    public JWTTokenDTO(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
