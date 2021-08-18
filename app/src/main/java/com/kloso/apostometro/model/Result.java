package com.kloso.apostometro.model;

import com.kloso.apostometro.R;

public enum Result {
    UNRESOLVED(R.string.unresolved),
    WON_BY_FAVOUR(R.string.won_by_favour),
    WON_BY_AGAINST(R.string.won_by_against);

    private int value;

    Result(int value){
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
