package com.basesoftware.cryptojava.architecture.mvvm.model;

import com.basesoftware.cryptojava.model.CryptoModel;
import java.util.List;

public class DataResult {

    private final List<CryptoModel> cryptoList;

    private final String message;

    public DataResult(List<CryptoModel> cryptoList, String message) {
        this.cryptoList = cryptoList;
        this.message = message;
    }

    public List<CryptoModel> getCryptoList() { return cryptoList; }

    public String getMessage() { return message; }

}
