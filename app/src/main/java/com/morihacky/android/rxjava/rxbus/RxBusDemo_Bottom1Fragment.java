package com.morihacky.android.rxjava.rxbus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.morihacky.android.rxjava.MainActivity;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

public class RxBusDemo_Bottom1Fragment extends BaseFragment {

    @BindView(R.id.demo_rxbus_tap_txt)
    TextView mTapEventTxtShow;

    private CompositeDisposable mCompositeDisposable;
    private RxBus mRxBus;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_rxbus_bottom, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRxBus = ((MainActivity) getActivity()).getRxBusSingleton();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCompositeDisposable = new CompositeDisposable();

        mCompositeDisposable.add(mRxBus.asFlowable()
                .subscribe(event -> {
                    if (event instanceof RxBusDemoFragment.TapEvent) {
                        showTapText();
                    }
                }));
    }

    @Override
    public void onStop() {
        super.onStop();
        mCompositeDisposable.clear();
    }

    private void showTapText() {
        mTapEventTxtShow.setVisibility(View.VISIBLE);
        mTapEventTxtShow.setAlpha(1f);
        ViewCompat.animate(mTapEventTxtShow).alphaBy(-1f).setDuration(400);
    }
}
