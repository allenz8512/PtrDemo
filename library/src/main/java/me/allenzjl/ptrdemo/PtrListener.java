package me.allenzjl.ptrdemo;

public interface PtrListener {

    void onPtrStateChanged(PtrState state);

    void onRefresh();

    void onPtrScroll(PtrState state, float positionOffset, int positionOffsetPixels);
}
