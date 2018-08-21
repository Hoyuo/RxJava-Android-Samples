package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subscribers.DisposableSubscriber;
import timber.log.Timber;

import static android.os.Looper.getMainLooper;

public class RotationPersist1Fragment extends BaseFragment
        implements RotationPersist1WorkerFragment.IAmYourMaster {

    public static final String TAG = RotationPersist1Fragment.class.toString();

    @BindView(R.id.list_threading_log)
    ListView mLogList;

    private LogAdapter mAdapter;
    private List<String> mLogs;
    private Unbinder mUnbinder;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    // -----------------------------------------------------------------------------------

    @OnClick(R.id.btn_rotate_persist)
    public void startOperationFromWorkerFrag() {
        mLogs = new ArrayList<>();
        mAdapter.clear();

        FragmentManager fm = getActivity().getSupportFragmentManager();
        RotationPersist1WorkerFragment frag =
                (RotationPersist1WorkerFragment) fm.findFragmentByTag(RotationPersist1WorkerFragment.TAG);

        if (frag == null) {
            frag = new RotationPersist1WorkerFragment();
            fm.beginTransaction().add(frag, RotationPersist1WorkerFragment.TAG).commit();
        } else {
            Timber.d("Worker frag already spawned");
        }
    }

    @Override
    public void observeResults(Flowable<Integer> intsFlowable) {

        DisposableSubscriber<Integer> d = new DisposableSubscriber<Integer>() {
            @Override
            public void onNext(Integer integer) {
                log(String.format("Worker frag spits out - %d", integer));
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error in worker demo frag observable");
                log("Dang! something went wrong.");
            }

            @Override
            public void onComplete() {
                log("Observable is complete");
            }
        };

        intsFlowable.doOnSubscribe(subscription -> log("Subscribing to intsObservable"))
                .subscribe(d);

        mCompositeDisposable.add(d);
    }

    // -----------------------------------------------------------------------------------
    // Boilerplate
    // -----------------------------------------------------------------------------------

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLogger();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_rotation_persist, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();
        mCompositeDisposable.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogList.setAdapter(mAdapter);
    }

    private void log(String logMsg) {
        mLogs.add(0, logMsg);

        // You can only do below stuff on main thread.
        new Handler(getMainLooper())
                .post(() -> {
                    mAdapter.clear();
                    mAdapter.addAll(mLogs);
                });
    }
}
