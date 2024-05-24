package com.basesoftware.cryptojava.architecture.mvvm.model;

import com.basesoftware.cryptojava.Constant;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.roomdb.CryptoDao;
import com.basesoftware.cryptojava.service.CryptoAPI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

@ActivityRetainedScoped
public class MVVMRepository {

    public CryptoAPI cryptoAPI;

    public CryptoDao cryptoDao;

    public CompositeDisposable compositeDisposable;

    private final BehaviorSubject<List<CryptoModel>> behaviorDataFromDb = BehaviorSubject.create();
    private final BehaviorSubject<List<CryptoModel>> behaviorDataFromApi = BehaviorSubject.create();
    private final BehaviorSubject<String> behaviorInformationForData = BehaviorSubject.create();

    @Inject
    public MVVMRepository(CryptoAPI cryptoAPI, CryptoDao cryptoDao, CompositeDisposable compositeDisposable) {

        this.cryptoAPI = cryptoAPI;

        this.cryptoDao = cryptoDao;

        this.compositeDisposable = compositeDisposable;

    }

    public BehaviorSubject<List<CryptoModel>> getBehaviorDataFromDb() { return behaviorDataFromDb; }

    public BehaviorSubject<List<CryptoModel>> getBehaviorDataFromApi() { return behaviorDataFromApi; }

    public BehaviorSubject<String> getBehaviorInformationForData() { return behaviorInformationForData; }


    public void checkDataFromDb() {

        compositeDisposable.add(cryptoDao.getAllData() // Veritabanından List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread())// UI gösterim
                .subscribe(behaviorDataFromDb::onNext)); // Dönen veriyi kontrol etmek için referans verilmiş method

    }

    public void checkDataFromApi() {

        compositeDisposable.clear(); // Disposable temizlendi

        compositeDisposable.add(cryptoAPI.getCryptoData() // Api tarafından List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                .subscribe(behaviorDataFromApi::onNext, e-> behaviorDataFromApi.onNext(new ArrayList<>())) // Dönen veriyi kontrol etmek için referans verilmiş method
        );

    }

    public void saveDbDataFromApi(List<CryptoModel> arrayCrypto) {

        compositeDisposable.add(
                cryptoDao.insert(arrayCrypto) // Room ile list şeklinde veri kaydediliyor
                        .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                        .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                        .subscribe(() -> behaviorInformationForData.onNext(Constant.CREATE_DB_DATA_SUCCESS), // Başarılı olur ise
                                throwable -> behaviorInformationForData.onNext(Constant.CREATE_DB_DATA_FAIL)) // Başarısız olur ise
        );

    }

}
