package com.basesoftware.cryptojava.architecture.mvc.controller;

import com.basesoftware.cryptojava.architecture.mvc.model.MVCRepository;
import javax.inject.Inject;
import dagger.hilt.android.scopes.ActivityScoped;

@ActivityScoped
public class MVCController {

    @Inject
    MVCRepository repository;

    @Inject
    public MVCController() {}

    public void checkDataFromDb() { repository.checkDataFromDb(); } // Repository veritabanını kontrol ediyor

    public void checkDataFromDbProcess() { repository.checkDataFromDbProcess(); } // Veritabanında bilgilendirme sonrası işlem yapılıyor

    public void saveDbDataFromApi() { repository.saveDbDataFromApi(); } // API verisi veritabanına kaydediliyor

    public void swipeAction() { repository.swipeAction(); } // Swipe işlemi modele gönderildi

}
