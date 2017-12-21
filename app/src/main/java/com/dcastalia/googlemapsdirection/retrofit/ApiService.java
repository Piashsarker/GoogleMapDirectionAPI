package com.dcastalia.googlemapsdirection.retrofit;

import com.dcastalia.googlemapsdirection.model.GoogleMapDirection;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by piashsarker on 12/21/17.
 */

public interface ApiService {


    @GET("maps/api/directions/json")
    Call<GoogleMapDirection> getDirection(@Query("origin")String origin ,
                                          @Query("destination")String destination ,
                                          @Query("mode")String mode,
                                          @Query("key") String key);

}
