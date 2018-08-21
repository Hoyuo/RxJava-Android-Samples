package com.morihacky.android.rxjava.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.morihacky.android.rxjava.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ConcurrencyWithSchedulersDemoFragment extends BaseFragment {

    @BindView(R.id.progress_operation_running)
    ProgressBar mProgressBar;

    @BindView(R.id.list_threading_log)
    ListView mLogsList;

    private LogAdapter mLogAdapter;
    private List<String> mLogs;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private Unbinder mUnbinder;

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
        mCompositeDisposable.clear();
    }

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
        View layout = inflater.inflate(R.layout.fragment_concurrency_schedulers, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @OnClick(R.id.btn_start_operation)
    public void startLongOperation() {

        mProgressBar.setVisibility(View.VISIBLE);
        log("Button Clicked");

        DisposableObserver<Boolean> d = getDisposableObserver();

        getObservable().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d);

        mCompositeDisposable.add(d);
    }

    private Observable<Boolean> getObservable() {
        return Observable.just(true)
                .map(aBoolean -> {
                    log("Within Observable");
                    doSomeLongOperationThatBlocksCurrentThread();
                    return aBoolean;
                });
    }

    /**
     * Observer that handles the result through the 3 important actions:
     *
     * <p>1. onCompleted 2. onError 3. onNext
     */
    private DisposableObserver<Boolean> getDisposableObserver() {
        return new DisposableObserver<Boolean>() {

            @Override
            public void onComplete() {
                log("On complete");
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error in RxJava Demo concurrency");
                log(String.format("Boo! Error %s", e.getMessage()));
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNext(Boolean bool) {
                log(String.format("onNext with return value \"%b\"", bool));
            }
        };
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private void doSomeLongOperationThatBlocksCurrentThread() {
        log("performing long operation");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Timber.d("Operation was interrupted");
        }
    }

    private void log(String logMsg) {

        if (isCurrentlyOnMainThread()) {
            mLogs.add(0, logMsg + " (main thread) ");
            mLogAdapter.clear();
            mLogAdapter.addAll(mLogs);
        } else {
            mLogs.add(0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread.
            new Handler(Looper.getMainLooper())
                    .post(() -> {
                        mLogAdapter.clear();
                        mLogAdapter.addAll(mLogs);
                    });
        }
    }

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mLogAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogsList.setAdapter(mLogAdapter);
    }

    private boolean isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private class LogAdapter extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }
}
