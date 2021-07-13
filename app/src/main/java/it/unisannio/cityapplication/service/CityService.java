package it.unisannio.cityapplication.service;

import it.unisannio.cityapplication.dto.JWTTokenDTO;
import it.unisannio.cityapplication.dto.LoginDTO;
import it.unisannio.cityapplication.dto.RegisterDTO;
import it.unisannio.cityapplication.dto.TicketDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface CityService {

    @POST("users/login")
    Call<JWTTokenDTO> getTokenForLogin(@Body LoginDTO loginDTO);

    @POST("users/register")
    Call<JWTTokenDTO> getTokenForSignIn(@Body RegisterDTO registerDTO);

    @GET("tickets")
    Call<TicketDTO> getTicket(@Header("Authorization") String jwt);

}
