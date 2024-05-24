package com.basesoftware.cryptojava.service;

import com.basesoftware.cryptojava.model.CryptoModel;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.http.GET;

public interface CryptoAPI {

    @GET("atilsamancioglu/K21-JSONDataSet/master/crypto.json")
    Single<List<CryptoModel>> getCryptoData();

}
