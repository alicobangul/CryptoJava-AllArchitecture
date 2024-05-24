package com.basesoftware.cryptojava.di;

import android.content.Context;
import androidx.room.Room;
import com.basesoftware.cryptojava.adapter.CryptoRecyclerAdapter;
import com.basesoftware.cryptojava.roomdb.CryptoDao;
import com.basesoftware.cryptojava.roomdb.CryptoDatabase;
import com.basesoftware.cryptojava.service.CryptoAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import io.reactivex.disposables.CompositeDisposable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(ActivityRetainedComponent.class)
public class CryptoJavaModule {

    @ActivityRetainedScoped
    @Provides
    public static CompositeDisposable compositeDisposableProvider() { return new CompositeDisposable(); }

    @ActivityRetainedScoped
    @Provides
    public static CryptoRecyclerAdapter cryptoAdapterProvider() { return new CryptoRecyclerAdapter(); }

    @ActivityRetainedScoped
    @Provides
    public static CryptoDatabase cryptoDatabaseProvider(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, CryptoDatabase.class, "CryptoDB").allowMainThreadQueries().build();
    }

    @ActivityRetainedScoped
    @Provides
    public static CryptoDao cryptoDaoProvider(CryptoDatabase cryptoDatabase) { return cryptoDatabase.cryptoDao(); }

    @ActivityRetainedScoped
    @Provides
    public static Gson gsonProvider() { return new GsonBuilder().setLenient().create(); }

    @ActivityRetainedScoped
    @Provides
    public static CryptoAPI cryptoAPIProvider(Gson gson) {
        return new Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CryptoAPI.class);
    }
}
