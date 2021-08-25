package it.unisannio.greenbusapplication.service;

import java.util.List;

import it.unisannio.greenbusapplication.dto.LoginDTO;
import it.unisannio.greenbusapplication.dto.RegisterDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.SessionDTO;
import it.unisannio.greenbusapplication.dto.TicketDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GreenBusService {

    @GET("routes")
    Call<List<RouteDTO>> getRoutes();

    @POST("users/login")
    Call<SessionDTO> getTokenForLogin(@Body LoginDTO loginDTO);

    @POST("users/register")
    Call<SessionDTO> getTokenForSignIn(@Body RegisterDTO registerDTO);

    @GET("tickets")
    Call<TicketDTO> getTicket(@Header("Authorization") String jwt);

}
