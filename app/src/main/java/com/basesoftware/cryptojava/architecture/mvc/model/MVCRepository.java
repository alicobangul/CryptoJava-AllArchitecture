package com.basesoftware.cryptojava.architecture.mvc.model;

import com.basesoftware.cryptojava.Constant;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import com.basesoftware.cryptojava.roomdb.CryptoDao;
import com.basesoftware.cryptojava.service.CryptoAPI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.scopes.ActivityScoped;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

@ActivityScoped
public class MVCRepository {

    public CryptoAPI cryptoAPI;

    public CryptoDao cryptoDao;

    public CompositeDisposable compositeDisposable;

    private ArrayList<CryptoRecyclerModel> arrayCryptoRecycler = new ArrayList<>();

    private List<CryptoModel> arrayCrypto = new ArrayList<>();

    private Boolean isFromApi = false;

    private Boolean isSwipeEnabled = false;

    private final BehaviorSubject<String> behaviorCheckDataFromDbInfo = BehaviorSubject.create();
    private final BehaviorSubject<String> behaviorInformationForData = BehaviorSubject.create();
    private final BehaviorSubject<String> behaviorQuestionSaveDbData = BehaviorSubject.create();
    private final BehaviorSubject<ArrayList<CryptoRecyclerModel>> behaviorUpdateRecyclerData = BehaviorSubject.create();

    @Inject
    public MVCRepository(CryptoAPI cryptoAPI, CryptoDao cryptoDao, CompositeDisposable compositeDisposable) {

        this.cryptoAPI = cryptoAPI;

        this.cryptoDao = cryptoDao;

        this.compositeDisposable = compositeDisposable;

    }

    public BehaviorSubject<String> getBehaviorCheckDataFromDbInfo() { return behaviorCheckDataFromDbInfo; }

    public BehaviorSubject<String> getBehaviorInformationForData() { return behaviorInformationForData; }

    public BehaviorSubject<String> getBehaviorQuestionSaveDbData() { return behaviorQuestionSaveDbData; }

    public BehaviorSubject<ArrayList<CryptoRecyclerModel>> getBehaviorUpdateRecyclerData() { return behaviorUpdateRecyclerData; }

    public void swipeAction() {

        if (isSwipeEnabled) {

            // Şuanki data durumlarına göre swipe aksiyonu yapılıyor

            if(arrayCryptoRecycler.isEmpty()) checkDataFromApi(); // Eğer hiç veri yok ise API denemesi yap

            else {

                if(!arrayCryptoRecycler.get(0).isApiData) checkDataFromApi(); // Eğer veri var ise & veritabanı verisi ise, API denemesi yap

                // Şuanda API verisi kullanıldığı gözlemcilere aktarılıyor
                else behaviorInformationForData.onNext(Constant.USE_API_DATA);

            }

        }

    }

    public void checkDataFromDb() {

        compositeDisposable.add(cryptoDao.getAllData() // Veritabanından List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread())// UI gösterim
                .subscribe(this::getDataFromDb)); // Dönen veriyi kontrol etmek için referans verilmiş method

    }

    public void getDataFromDb(List<CryptoModel> cryptoList) {

        compositeDisposable.clear(); // Disposable temizlendi

        arrayCrypto = cryptoList; // Dönen veri diğer fonksiyonların işlem yapabilmesi için değişkene atıldı

        isFromApi = false; // API verisi olmadığı belirtildi

        behaviorCheckDataFromDbInfo.onNext((!arrayCrypto.isEmpty()) ? Constant.DB_DATA_AVAILABLE : Constant.DB_DATA_NOTEXIST); // Gözlemciler database kontrolü sonrası bilgilendiriliyor

    }

    public void checkDataFromDbProcess() {

        if (!arrayCrypto.isEmpty()) convertModel(); // Veritabanı verisi var, veri Recyclerview adaptöründe gösterilecek şekilde modelleniyor

        else checkDataFromApi(); // Veritabanında veri yok [liste boş], API verisi kontrol ediliyor

    }

    public void checkDataFromApi() {

        compositeDisposable.add(cryptoAPI.getCryptoData() // Api tarafından List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                .subscribe(this::getDataFromApi, e-> getDataFromApi(new ArrayList<>())) // Dönen veriyi kontrol etmek için referans verilmiş method
        );

    }

    public void getDataFromApi(List<CryptoModel> cryptoList) {

        compositeDisposable.clear(); // Disposable temizlendi

        arrayCrypto = cryptoList; // Dönen veri diğer fonksiyonların işlem yapabilmesi için değişkene atıldı

        isFromApi = true; // API verisi olduğu belirtildi

        if (!arrayCrypto.isEmpty()) convertModel(); // API verisi Recyclerview adaptöründe gösterilecek şekilde modelleniyor

        else {

            isSwipeEnabled = true; // Refresh izni verildi

            behaviorInformationForData.onNext(Constant.API_DATA_NOTEXIST); // API verisi olmadığına dair gözlemciler bilgilendirildi

        }

    }

    public void saveDbDataFromApi() {

        compositeDisposable.add(
                cryptoDao.insert(arrayCrypto) // Room ile list şeklinde veri kaydediliyor
                        .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                        .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                        .subscribe(() -> behaviorInformationForData.onNext(Constant.CREATE_DB_DATA_SUCCESS), // Başarılı olur ise
                                throwable -> behaviorInformationForData.onNext(Constant.CREATE_DB_DATA_FAIL)) // Başarısız olur ise
        );

    }

    public void convertModel() {

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

        arrayCryptoRecycler = new ArrayList<>(); // Yeni listeyi oluştur

        // Yeni modeller ile liste hazırlanıyor
        for (CryptoModel cryptoModel : arrayCrypto) arrayCryptoRecycler.add( new CryptoRecyclerModel(cryptoModel.currency, cryptoModel.price, isFromApi) );

        behaviorUpdateRecyclerData.onNext(arrayCryptoRecycler); // Son item'a gelince gözlemcileri bilgilendir

        // Kullanıcıya veritabanına kayıt isteyip istemediği soruluyor
        if (isFromApi) behaviorQuestionSaveDbData.onNext(Constant.CREATE_DB_DATA_QUESTION);

        isSwipeEnabled = true; // Refresh izni verildi

    }

}
