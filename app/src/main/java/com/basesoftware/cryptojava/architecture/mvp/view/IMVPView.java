package com.basesoftware.cryptojava.architecture.mvp.view;

import com.basesoftware.cryptojava.model.CryptoModel;
import com.basesoftware.cryptojava.model.CryptoRecyclerModel;

import java.util.ArrayList;
import java.util.List;

public interface IMVPView {

    void updateRecyclerData(ArrayList<CryptoRecyclerModel> arrayCryptoRecycler);

    void checkDataFromDbInfo(List<CryptoModel> cryptoList, String message);

    void informationForData(String message);

    void questionSaveDbData(List<CryptoModel> cryptoList, String question);

    void swipeEnabled(boolean isEnabled);

}
