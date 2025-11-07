package com.auxilliumhealth.woundtissueclassification.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CalibrationViewModel extends ViewModel {
    private final MutableLiveData<String> selectedCoinName = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedPosition = new MutableLiveData<>();

    public void setSelectedCoin(String coinName, int position) {
        selectedCoinName.setValue(coinName);
        selectedPosition.setValue(position);
    }

    public LiveData<String> getSelectedCoinName() {
        return selectedCoinName;
    }

    public LiveData<Integer> getSelectedPosition() {
        return selectedPosition;
    }
}
