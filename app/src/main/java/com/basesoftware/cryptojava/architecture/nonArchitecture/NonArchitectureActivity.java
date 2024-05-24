package com.basesoftware.cryptojava.architecture.nonArchitecture;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;
import com.basesoftware.cryptojava.Constant;
import com.basesoftware.cryptojava.adapter.CryptoRecyclerAdapter;
import com.basesoftware.cryptojava.databinding.ActivityNonArchitectureBinding;
import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;
import com.basesoftware.cryptojava.roomdb.CryptoDao;
import com.basesoftware.cryptojava.roomdb.CryptoDatabase;
import com.basesoftware.cryptojava.service.CryptoAPI;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * <h2>CryptoJava - JAVA Language</h2>
 * <hr>
 *
 * <ul>
 * <li>RxJAVA</li>
 * <li>Room</li>
 * <li>Retrofit</li>
 * <li>DataBinding</li>
 * </ul>
 */

public class NonArchitectureActivity extends AppCompatActivity {

    private ActivityNonArchitectureBinding binding; // Görünümlere erişmek için binding

    private CryptoRecyclerAdapter cryptoRecyclerAdapter; // Recyclerview'a verilen adaptör

    private ArrayList<CryptoRecyclerModel> arrayCryptoRecycler; // Recyclerview içerisine verilen liste

    private List<CryptoModel> arrayCrypto; // Veritabanından & API'den dönen liste

    private CompositeDisposable compositeDisposable; // İstekler için disposable

    private CryptoAPI cryptoAPI; // Retrofit ile veri çekmek için arayüz

    private CryptoDao cryptoDao; // Veritabanından veri çekmek için DAO

    private Boolean isFromApi; // Veri çekme işleminin nereden yapıldığı belirten değişken

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNonArchitectureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        init(); // Değişkenler initialize ediliyor

        viewSettings(); // Görünüm ayarları yapılıyor

        listener(); // Swipe dinleyici açılıyor


        checkDataFromDb(); // Veritabanı kontrol ediliyor

    }


    private void init() {

        CryptoDatabase cryptoDatabase = Room
                .databaseBuilder(getApplicationContext(), CryptoDatabase.class, "CryptoDB")
                .allowMainThreadQueries()
                .build(); // Veritabanı bağlantısı

        cryptoDao = cryptoDatabase.cryptoDao(); // Dao tanımlandı

        arrayCryptoRecycler = new ArrayList<>(); // Adaptöre verilecek liste oluşturuldu

        arrayCrypto = new ArrayList<>(); // Api & Database'den dönen verilerde kullanılan liste

        isFromApi = false; // Şuanki veri alma modu database [DEFAULT]

        cryptoRecyclerAdapter = new CryptoRecyclerAdapter(); // Adaptör oluşturuldu

        compositeDisposable = new CompositeDisposable(); // CompositeDisposable oluşturuldu

        Gson gson = new GsonBuilder().setLenient().create(); // GsonBuilder oluşturuldu

        String BASE_URL = "https://raw.githubusercontent.com/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        cryptoAPI = retrofit.create(CryptoAPI.class); // Api sınıfı oluşturuldu

    }

    private void viewSettings() {

        binding.recyclerCrypto.setHasFixedSize(true); // Recyclerview boyutunun değişmeyeceği bildirildi [performans artışı]

        binding.recyclerCrypto.setLayoutManager(new LinearLayoutManager(this)); // Row için layout seçildi

        binding.recyclerCrypto.setAdapter(cryptoRecyclerAdapter); // Adaptör bağlandı

        binding.swipeLayout.setEnabled(false); // Swipe kapalı [DEFAULT]

    }

    private void listener() {

        binding.swipeLayout.setOnRefreshListener(() -> {

            binding.swipeLayout.setEnabled(false); // Swipe kapatıldı

            binding.swipeLayout.setRefreshing(false); // Swipe animasyonu kapatıldı

            // Şuanki data durumlarına göre işlem yapılıyor

            if(arrayCryptoRecycler.isEmpty()) checkDataFromApi(); // Eğer hiç veri yok ise API'den veri almayı dene

            else {

                if(!arrayCryptoRecycler.get(0).isApiData) checkDataFromApi(); // Eğer veri var ise & veritabanı verisi ise API'den veri almayı dene

                else {

                    informationSnackbar(Constant.USE_API_DATA); // API UI bilgilendirme

                    binding.swipeLayout.setEnabled(true); // Swipe açıldı

                }

            }

        });

    }


    public void checkDataFromDb() {

        isFromApi = false; // Veritabanından veri almaya çalışıldığı bildirildi

        compositeDisposable.add(cryptoDao.getAllData() // Veritabanından List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                .subscribe(this::getDataFromDb)); // Dönen veriyi kontrol etmek için referans verilmiş method

    }

    public void getDataFromDb(List<CryptoModel> cryptoList) {

        arrayCrypto = cryptoList; // Veritabanından dönen liste diğer methodlara parametre verilmemesi için değişkene atıldı

        compositeDisposable.clear(); // Disposable temizlendi

        String message = (!arrayCrypto.isEmpty()) ? Constant.DB_DATA_AVAILABLE : Constant.DB_DATA_NOTEXIST;

        Snackbar
                .make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .addCallback(new BaseTransientBottomBar.BaseCallback<>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);

                if(!arrayCrypto.isEmpty()) convertModel(); // Veritabanında veri var [liste boş değil], veriyi recyclerview için modelle

                else checkDataFromApi(); // Veritabanında veri yok [liste boş], API'den veri almayı dene

            }
        }).show();

    }



    public void checkDataFromApi() {

        isFromApi = true; // API'DEN veri almaya çalışıldığı bildirildi

        compositeDisposable.clear(); // CompositeDisposable temizlendi

        compositeDisposable.add(cryptoAPI.getCryptoData() // Api'den List<CryptoModel> döndürmesi gereken observable fonksiyon
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                .subscribe(this::getDataFromApi, e-> getDataFromApi(new ArrayList<>())) // Dönen veriyi kontrol etmek için referans verilmiş method
        );

    }

    public void getDataFromApi(List<CryptoModel> cryptoList) {

        arrayCrypto = cryptoList; // API'den dönen liste diğer methodlara parametre verilmemesi için değişkene atıldı

        compositeDisposable.clear(); // CompositeDisposable temizlendi

        if (!cryptoList.isEmpty()) {

            Snackbar
                    .make(binding.getRoot(), Constant.CREATE_DB_DATA_QUESTION, Snackbar.LENGTH_SHORT)
                    .setAction("EVET", v -> saveDbDataFromApi()) // API sonrası veriler veritabanına yazılıyor
                    .show();

            convertModel(); // Veriyi recylerview için modelle

        }

        else {

            informationSnackbar(Constant.API_DATA_NOTEXIST); // UI bilgilendirmesi

            binding.swipeLayout.setEnabled(true); // Swipe açıldı

        }

    }



    public void convertModel() {

        // Veriler RecyclerView için modelleniyor

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
        arrayCryptoRecycler = new ArrayList<>();

        // Yeni modeller ile liste hazırlanıyor
        for (CryptoModel cryptoModel : arrayCrypto) arrayCryptoRecycler.add( new CryptoRecyclerModel(cryptoModel.currency, cryptoModel.price, isFromApi) );

        showData(); // Veri modelleme bitti veriyi recyclerview'da göster

        binding.swipeLayout.setEnabled(true); // Swipe yapma izni verildi

    }


    public void showData() {

        cryptoRecyclerAdapter.updateData(arrayCryptoRecycler); // RecyclerView'a data gönder DiffUtil kullanarak

    }


    public void saveDbDataFromApi() {

        compositeDisposable.add(
                cryptoDao.insert(arrayCrypto) // Verileri içeren liste
                        .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                        .observeOn(AndroidSchedulers.mainThread()) // UI gösterim
                        .subscribe(() ->informationSnackbar(Constant.CREATE_DB_DATA_SUCCESS), // Yazma başarılı olursa
                                throwable -> informationSnackbar(Constant.CREATE_DB_DATA_FAIL)) // Yazma başarısız olursa
        );

    }


    public void informationSnackbar(String message) { Snackbar.make(binding.getRoot(), message, Toast.LENGTH_SHORT).show(); }

}