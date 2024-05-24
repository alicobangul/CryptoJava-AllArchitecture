package com.basesoftware.cryptojava.architecture.mvvm.view;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.basesoftware.cryptojava.adapter.CryptoRecyclerAdapter;
import com.basesoftware.cryptojava.architecture.mvvm.model.DataResult;
import com.basesoftware.cryptojava.architecture.mvvm.viewmodel.MVVMViewModel;
import com.basesoftware.cryptojava.databinding.ActivityMvvmBinding;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MvvmActivity extends AppCompatActivity {

    private ActivityMvvmBinding binding;

    private MVVMViewModel viewModel;

    @Inject
    CryptoRecyclerAdapter cryptoRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMvvmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init(); // Initialize işlemi yapıldı

        viewSettings(); // Görünüm ayarları yapılıyor

        listener(); // Swipe dinleyici açılıyor

        observers(); // Gözlemciler eklendi

        checkDataFromDb(); // Veritabanı kontrol ediliyor

    }

    private void init() {

        viewModel = new ViewModelProvider(this).get(MVVMViewModel.class);

        viewModel.openObservers();

    }

    private void viewSettings() {

        binding.recyclerCrypto.setHasFixedSize(true); // Recyclerview boyutunun değişmeyeceği bildirildi [performans artışı]

        binding.recyclerCrypto.setLayoutManager(new LinearLayoutManager(this)); // Row için layout seçildi

        binding.recyclerCrypto.setAdapter(cryptoRecyclerAdapter); // Adaptör bağlandı

        swipeEnabled(false); // Refresh kapalı, ilk önce veri kontrolleri yapılacak

    }

    private void listener() {

        binding.swipeLayout.setOnRefreshListener(() -> {

            binding.swipeLayout.setRefreshing(false); // Refresh animasyonu kapatıldı

            viewModel.swipeAction(cryptoRecyclerAdapter.getCurrentList()); // viewmodel'a view tarafında swipe yapıldığı bildirildi

        });

    }

    private void observers() {

        viewModel.getRecyclerData().observe(this, this::updateRecyclerData);

        viewModel.getDataResult().observe(this, this::checkDataFromDbInfo);

        viewModel.getSwipeStatus().observe(this, this::swipeEnabled);

        viewModel.getInformationForData().observe(this, this::informationForData);

        viewModel.getQuestionSaveDbData().observe(this, this::questionSaveDbData);

    }

    // Veri viewmodel'dan alındı
    public void updateRecyclerData(ArrayList<CryptoRecyclerModel> arrayCryptoRecycler) { cryptoRecyclerAdapter.updateData(arrayCryptoRecycler); }

    private void checkDataFromDb() { viewModel.checkDataFromDb(); } // Veritabanı kontrol ediliyor

    public void checkDataFromDbInfo(@NonNull DataResult dbDataResult) {

        /**
         * Snackbar sonrası viewmodel tarafında gerekli aksiyon başlatıldı
         * Snackbar dismiss beklenmesinin nedeni veritabanında veri bulunamadığı zaman api verisi alınıyor.
         * Aynı anda iki snackbar açıldığında görünüm karmaşası yaratıyor, bu yüzden dismiss bekleniyor.
         */

        Snackbar
                .make(binding.getRoot(), dbDataResult.getMessage(), Snackbar.LENGTH_SHORT)
                .addCallback(new BaseTransientBottomBar.BaseCallback<>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        viewModel.checkDataFromDbProcess(dbDataResult.getCryptoList());

                    }
                }).show();

    }

    public void informationForData(String message) { Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show(); } // UI bilgilendirme

    public void questionSaveDbData(@NonNull DataResult dataResult) {

        Snackbar
                .make(binding.getRoot(), dataResult.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("EVET", v -> viewModel.saveDbDataFromApi(dataResult.getCryptoList())) // API sonrası veriler veritabanına yazılıyor
                .show();

    }

    public void swipeEnabled(boolean isEnabled) { binding.swipeLayout.setEnabled(isEnabled); }

}