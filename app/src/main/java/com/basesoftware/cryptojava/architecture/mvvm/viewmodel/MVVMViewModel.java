package com.basesoftware.cryptojava.architecture.mvvm.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.basesoftware.cryptojava.Constant;
import com.basesoftware.cryptojava.architecture.mvvm.model.DataResult;
import com.basesoftware.cryptojava.architecture.mvvm.model.MVVMRepository;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class MVVMViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<CryptoRecyclerModel>> recyclerData = new MutableLiveData<>();

    private final MutableLiveData<DataResult> dataResult = new MutableLiveData<>();

    private final MutableLiveData<Boolean> swipeStatus = new MutableLiveData<>();

    private final MutableLiveData<String> informationForData = new MutableLiveData<>();

    private final MutableLiveData<DataResult> questionSaveDbData = new MutableLiveData<>();

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    MVVMRepository repository;

    @Inject
    public MVVMViewModel() {}

    public LiveData<ArrayList<CryptoRecyclerModel>> getRecyclerData() { return recyclerData; }

    public LiveData<DataResult> getDataResult() { return dataResult; }

    public LiveData<Boolean> getSwipeStatus() { return swipeStatus; }

    public LiveData<String> getInformationForData() { return informationForData; }

    public LiveData<DataResult> getQuestionSaveDbData() { return questionSaveDbData; }

    public void getDataFromDb(List<CryptoModel> cryptoList) {

        // Veritabanı kontrol sonucu, view tarafında snackbar ile gösteriliyor

        dataResult.setValue(
                new DataResult(
                        cryptoList,
                        (!cryptoList.isEmpty()) ? Constant.DB_DATA_AVAILABLE : Constant.DB_DATA_NOTEXIST
                )
        );

    }

    public void getDataFromApi(@NonNull List<CryptoModel> cryptoList) {

        if (!cryptoList.isEmpty()) convertModel(cryptoList, true); // API verisi Recyclerview adaptöründe gösterilecek şekilde modelleniyor

        else {

            swipeStatus.setValue(true); // Refresh izni verildi

            informationForData.setValue(Constant.API_DATA_NOTEXIST); // API verisi olmadığına dair bilgilendirildi

        }

    }

    public void openObservers() {

        compositeDisposable.addAll(
                repository.getBehaviorDataFromDb().subscribe(this::getDataFromDb),
                repository.getBehaviorDataFromApi().subscribe(this::getDataFromApi),
                repository.getBehaviorInformationForData().subscribe(informationForData::setValue)
        ); // Gözlemciler eklendi

    }

    public void checkDataFromDb() { repository.checkDataFromDb(); } // Model, veritabanındaki veriyi kontrol ediliyor

    public void checkDataFromDbProcess(@NonNull List<CryptoModel> cryptoList) {

        // Veritabanı verisi var, veri Recyclerview adaptöründe gösterilecek şekilde modelleniyor
        if (!cryptoList.isEmpty()) convertModel(cryptoList, false);

        // Veritabanında veri yok [liste boş], API verisi kontrol ediliyor
        else repository.checkDataFromApi();

    }

    public void convertModel(@NonNull List<CryptoModel> cryptoList, boolean isFromApi) {

        /**
         * arrayCryptoRecycler = new ArrayList<>() yapılmasının nedeni: RecyclerView adaptöründe AsyncListDiffer kullanılması.
         * arrayCryptoRecycler = new ArrayList<>() yapılmadan Recyclerview adaptöründeki updateData methodu içerisinde
         * mDiffer.submitList(arrayNewCrypto); [arrayNewCrypto parametreden gelen liste] yapılırsa
         * iki liste aynı olduğu için ItemCallback tetiklenmeyecektir. Bu nedenle arrayCryptoRecycler.clear(); yerine new ArrayList<>() yapılmalıdır.
         * Aksi halde; ekranda veritabanından gelen veriler var iken refresh yapılırsa ve api verilerinin olduğu liste submitList ile verilirse
         * Ekrandaki OFFLINE (DB DATA) yazısı değişmeyecek, item güncellenmeyecektir.
         * Çünkü AsyncListDiffer sistemi eski ve yeni liste eşleştirmesi üzerine kuruludur.
         *
         * Seçenek a-) convertModel içerisinde arrayCryptoRecycler = new ArrayList<>(); yapılması
         * Seçenek b-) Adaptör içerisinde mDiffer.submitList(new ArrayList<>(arrayNewCrypto)); yapılması [arrayNewCrypto parametreden gelen liste]
         */

        // Yeni listeyi oluştur
        ArrayList<CryptoRecyclerModel> arrayCryptoRecycler = new ArrayList<>();

        // Yeni modeller ile liste hazırlanıyor
        for (CryptoModel cryptoModel : cryptoList) arrayCryptoRecycler.add( new CryptoRecyclerModel(cryptoModel.currency, cryptoModel.price, isFromApi) );

        // Model çevirme işlemi tamamlandı
        convertModelComplete(cryptoList, arrayCryptoRecycler, isFromApi);

    }

    public void convertModelComplete(List<CryptoModel> arrayDbList, ArrayList<CryptoRecyclerModel> recyclerList, boolean isFromApi) {

        // Adaptördeki verileri güncelle
        recyclerData.setValue(recyclerList);

        // Kullanıcıya veritabanına kayıt isteyip istemediği soruluyor
        if (isFromApi) questionSaveDbData.setValue( new DataResult(arrayDbList, Constant.CREATE_DB_DATA_QUESTION) );

        // Refresh izni verildi
        swipeStatus.setValue(true);

    }

    public void saveDbDataFromApi(List<CryptoModel> cryptoList) { repository.saveDbDataFromApi(cryptoList); }

    public void swipeAction(@NonNull List<CryptoRecyclerModel> arrayCryptoRecycler) {

        // Eğer hiç veri yok ise API denemesi yap
        if(arrayCryptoRecycler.isEmpty()) repository.checkDataFromApi();

        else {

            // Eğer veri var ise & veritabanı verisi ise, API denemesi yap
            if(!arrayCryptoRecycler.get(0).isApiData) repository.checkDataFromApi();

            // Şuanda API verisi kullanıldığı livedata aracılığıyla aktarılıyor
            else informationForData.setValue(Constant.USE_API_DATA);

        }

    }

}