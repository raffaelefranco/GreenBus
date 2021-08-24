package it.unisannio.greenbusapplication.dto;

import java.io.Serializable;
import java.util.List;


public class SessionDTO implements Serializable {

    private String jwt;
    private List<String> roles;

    public SessionDTO() { }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
