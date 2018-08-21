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

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.flowables.ConnectableFlowable;

public class RxBusDemo_Bottom3Fragment extends BaseFragment {

    @BindView(R.id.demo_rxbus_tap_txt)
    TextView mTapEventTxtShow;

    @BindView(R.id.demo_rxbus_tap_count)
    TextView mTapEventCountShow;

    private RxBus mRxBus;
    private CompositeDisposable mCompositeDisposable;

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

        ConnectableFlowable<Object> tapEventEmitter = mRxBus.asFlowable().publish();

        mCompositeDisposable.add(tapEventEmitter.subscribe(event -> {
            if (event instanceof RxBusDemoFragment.TapEvent) {
                showTapText();
            }
        }));

        mCompositeDisposable.add(tapEventEmitter
                .publish(stream -> stream.buffer(stream.debounce(1, TimeUnit.SECONDS)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(taps -> showTapCount(taps.size())));

        mCompositeDisposable.add(tapEventEmitter.connect());
    }

    @Override
    public void onStop() {
        super.onStop();
        mCompositeDisposable.clear();
    }

    // -----------------------------------------------------------------------------------
    // Helper to show the text via an animation

    private void showTapText() {
        mTapEventTxtShow.setVisibility(View.VISIBLE);
        mTapEventTxtShow.setAlpha(1f);
        ViewCompat.animate(mTapEventTxtShow).alphaBy(-1f).setDuration(400);
    }

    private void showTapCount(int size) {
        mTapEventCountShow.setText(String.valueOf(size));
        mTapEventCountShow.setVisibility(View.VISIBLE);
        mTapEventCountShow.setScaleX(1f);
        mTapEventCountShow.setScaleY(1f);
        ViewCompat.animate(mTapEventCountShow)
                .scaleXBy(-1f)
                .scaleYBy(-1f)
                .setDuration(800)
                .setStartDelay(100);
    }
}
