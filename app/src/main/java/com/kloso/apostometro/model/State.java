package com.kloso.apostometro.model;


import com.kloso.apostometro.R;

public enum State {
    OPEN(R.string.pending),
    WON_BY_FAVOUR(R.string.won_by_favour),
    WON_BY_AGAINST(R.string.won_by_against),
    RESOLVED(R.string.bet_resolved),
    PAID(R.string.paid);

    private int value;

    State(int value){
        this.value = value;
    }

    @Override
    public String toString() {
     return String.valueOf(value);
    }
}
