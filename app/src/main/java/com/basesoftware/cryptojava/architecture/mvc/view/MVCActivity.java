package com.basesoftware.cryptojava.architecture.mvc.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.basesoftware.cryptojava.adapter.CryptoRecyclerAdapter;
import com.basesoftware.cryptojava.architecture.mvc.controller.MVCController;
import com.basesoftware.cryptojava.architecture.mvc.model.MVCRepository;
import com.basesoftware.cryptojava.databinding.ActivityMvcBinding;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.disposables.CompositeDisposable;

@AndroidEntryPoint
public class MVCActivity extends AppCompatActivity {

    private ActivityMvcBinding binding;

    private CompositeDisposable compositeDisposable;

    @Inject
    MVCController controller;

    @Inject
    MVCRepository repository;

    @Inject
    CryptoRecyclerAdapter cryptoRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMvcBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init(); // Initialize işlemleri

        viewSettings(); // Görünüm ayarları yapılıyor

        listenerAndObserver(); // Dinleyiciler açılıyor

        checkDataFromDb(); // // Veritabanı kontrol ediliyor

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    private void init() { compositeDisposable = new CompositeDisposable(); }

    private void viewSettings() {

        binding.recyclerCrypto.setHasFixedSize(true); // Recyclerview boyutunun değişmeyeceği bildirildi [performans artışı]

        binding.recyclerCrypto.setLayoutManager(new LinearLayoutManager(this)); // Row için layout seçildi

        binding.recyclerCrypto.setAdapter(cryptoRecyclerAdapter); // Adaptör bağlandı

    }

    private void listenerAndObserver() {

        binding.swipeLayout.setOnRefreshListener(() -> {

            binding.swipeLayout.setRefreshing(false); // Refresh animasyonu kapatıldı

            controller.swipeAction(); // Controller'a view tarafında swipe yapıldığı bildirildi

        });

        compositeDisposable.addAll(
                repository.getBehaviorCheckDataFromDbInfo().subscribe(this::checkDataFromDbInfo), // Gelen mesaj UI'da gösterildi
                repository.getBehaviorInformationForData().subscribe(this::informationForData), // Gelen mesaj UI'da gösterildi
                repository.getBehaviorQuestionSaveDbData().subscribe(this::questionSaveDbData), // Gelen mesaj UI'da gösterildi
                repository.getBehaviorUpdateRecyclerData().subscribe(this::updateRecyclerData) // Adaptör güncellendi
        );

    }

    public void updateRecyclerData(ArrayList<CryptoRecyclerModel> outputList) { cryptoRecyclerAdapter.updateData(outputList); } // Adaptör güncellendi

    private void checkDataFromDb() { controller.checkDataFromDb(); } // Veritabanı kontrol ediliyor

    public void checkDataFromDbInfo(String message) {

        /**
         * Snackbar sonrası controller tarafında gerekli aksiyon başlatıldı
         * Snackbar dismiss beklenmesinin nedeni veritabanında veri bulunamadığı zaman api verisi alınıyor.
         * Aynı anda iki snackbar açıldığında görünüm karmaşası yaratıyor, bu yüzden dismiss bekleniyor.
         */

        Snackbar
                .make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .addCallback(new BaseTransientBottomBar.BaseCallback<>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        controller.checkDataFromDbProcess(); // UI bilgilendirme sonrası veritabanı veri sonucuna göre işlemler yapılıyor

                    }
                }).show();

    }

    public void informationForData(String message) { Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show(); } // UI bilgilendirme

    public void questionSaveDbData(String question) {

        Snackbar
                .make(binding.getRoot(), question, Snackbar.LENGTH_LONG)
                .setAction("EVET", v -> controller.saveDbDataFromApi()) // API sonrası veriler veritabanına yazılıyor
                .show();

    }

}