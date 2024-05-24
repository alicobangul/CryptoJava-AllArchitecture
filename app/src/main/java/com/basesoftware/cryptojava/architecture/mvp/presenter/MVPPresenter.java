package com.basesoftware.cryptojava.architecture.mvp.presenter;

import androidx.annotation.NonNull;
import com.basesoftware.cryptojava.Constant;
import com.basesoftware.cryptojava.architecture.mvp.model.MVPRepository;
import com.basesoftware.cryptojava.architecture.mvp.view.IMVPView;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.scopes.ActivityScoped;
import io.reactivex.disposables.CompositeDisposable;

@ActivityScoped
public class MVPPresenter {

    @Inject
    MVPRepository repository;

    private IMVPView view;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public MVPPresenter() {}

    public void detach() {

        compositeDisposable.clear(); // Gözlemciler kaldırıldı

        view = null; // View kaldırıldı

    }

    public void getDataFromDb(List<CryptoModel> cryptoList) {

        // Veritabanı kontrol sonucu, view tarafında snackbar ile gösteriliyor

        if (view != null) {

            view.checkDataFromDbInfo(
                    cryptoList,
                    (!cryptoList.isEmpty()) ? Constant.DB_DATA_AVAILABLE : Constant.DB_DATA_NOTEXIST
            );

        }

    }

    public void getDataFromApi(@NonNull List<CryptoModel> cryptoList) {

        if (!cryptoList.isEmpty()) convertModel(cryptoList, true); // API verisi Recyclerview adaptöründe gösterilecek şekilde modelleniyor

        else {

            if (view != null) {

                view.swipeEnabled(true); // Refresh izni verildi

                view.informationForData(Constant.API_DATA_NOTEXIST); // API verisi olmadığına dair bilgilendirildi

            }

        }

    }

    public void initBinds(@NonNull IMVPView view) {

        this.view = view;  // View tarafından gerekli arayüz alındı

        compositeDisposable.addAll(
          repository.getBehaviorDataFromDb().subscribe(this::getDataFromDb),
          repository.getBehaviorDataFromApi().subscribe(this::getDataFromApi),
          repository.getBehaviorInformationForData().subscribe(view::informationForData)
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

        if (view != null) {

            // Adaptördeki verileri güncelle
            view.updateRecyclerData(recyclerList);

            // Kullanıcıya veritabanına kayıt isteyip istemediği soruluyor
            if (isFromApi) view.questionSaveDbData(arrayDbList, Constant.CREATE_DB_DATA_QUESTION);

            // Refresh izni verildi
            view.swipeEnabled(true);

        }

    }

    public void saveDbDataFromApi(List<CryptoModel> cryptoList) { repository.saveDbDataFromApi(cryptoList); }

    public void swipeAction(@NonNull List<CryptoRecyclerModel> arrayCryptoRecycler) {

        // Eğer hiç veri yok ise API denemesi yap
        if(arrayCryptoRecycler.isEmpty()) repository.checkDataFromApi();

        else {

            // Eğer veri var ise & veritabanı verisi ise, API denemesi yap
            if(!arrayCryptoRecycler.get(0).isApiData) repository.checkDataFromApi();

            // Şuanda API verisi kullanıldığı presenter aracılığıyla aktarılıyor
            else if(view != null) view.informationForData(Constant.USE_API_DATA);

        }

    }

}
