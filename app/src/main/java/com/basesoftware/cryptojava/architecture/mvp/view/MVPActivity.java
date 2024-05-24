package com.basesoftware.cryptojava.architecture.mvp.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.basesoftware.cryptojava.adapter.CryptoRecyclerAdapter;
import com.basesoftware.cryptojava.architecture.mvp.presenter.MVPPresenter;
import com.basesoftware.cryptojava.databinding.ActivityMvpBinding;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MVPActivity extends AppCompatActivity implements IMVPView {

    private ActivityMvpBinding binding;

    @Inject
    CryptoRecyclerAdapter cryptoRecyclerAdapter;

    @Inject
    MVPPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMvpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewSettings(); // Görünüm ayarları yapılıyor

        listener(); // Swipe dinleyici açılıyor

        checkDataFromDb(); // Veritabanı kontrol ediliyor

    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.initBinds(this); // Presenter içerisine arayüz verildi
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.detach(); // Presenter gözlemcisi ve view kaldırıldı
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

            presenter.swipeAction(cryptoRecyclerAdapter.getCurrentList()); // Presenter'a view tarafında swipe yapıldığı bildirildi

        });

    }

    @Override
    public void updateRecyclerData(ArrayList<CryptoRecyclerModel> arrayCryptoRecycler) {

        /**
         * Adaptör güncellendi
         * MVC ile arasındaki en belirgin fark veri model'den değil, presenter'dan alınmaktadır
         */
        cryptoRecyclerAdapter.updateData(arrayCryptoRecycler);

    }

    private void checkDataFromDb() { presenter.checkDataFromDb(); } // Veritabanı kontrol ediliyor

    @Override
    public void checkDataFromDbInfo(List<CryptoModel> cryptoList, String message) {

        /**
         * Snackbar sonrası presenter tarafında gerekli aksiyon başlatıldı
         * Snackbar dismiss beklenmesinin nedeni veritabanında veri bulunamadığı zaman api verisi alınıyor.
         * Aynı anda iki snackbar açıldığında görünüm karmaşası yaratıyor, bu yüzden dismiss bekleniyor.
         */

        Snackbar
                .make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .addCallback(new BaseTransientBottomBar.BaseCallback<>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        presenter.checkDataFromDbProcess(cryptoList);

                    }
                }).show();

    }

    @Override
    public void informationForData(String message) { Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show(); } // UI bilgilendirme

    @Override
    public void questionSaveDbData(List<CryptoModel> cryptoList, String question) {

        Snackbar
                .make(binding.getRoot(), question, Snackbar.LENGTH_LONG)
                .setAction("EVET", v -> presenter.saveDbDataFromApi(cryptoList)) // API sonrası veriler veritabanına yazılıyor
                .show();

    }

    @Override
    public void swipeEnabled(boolean isEnabled) { binding.swipeLayout.setEnabled(isEnabled); }

}