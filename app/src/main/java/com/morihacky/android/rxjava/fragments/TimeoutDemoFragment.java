package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class TimeoutDemoFragment extends BaseFragment {

    @BindView(R.id.list_threading_log)
    ListView mLogsList;

    private LogAdapter mAdapter;
    private DisposableObserver<String> mDisposableObserver;
    private List<String> mLogs;

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDisposableObserver == null) {
            return;
        }

        mDisposableObserver.dispose();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_subject_timeout, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLogger();
    }

    @OnClick(R.id.btn_demo_timeout_1_2s)
    public void onStart2sTask() {
        mDisposableObserver = getEventCompletionObserver();

        getObservableTask2SToComplete()
                .timeout(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mDisposableObserver);
    }

    @OnClick(R.id.btn_demo_timeout_1_5s)
    public void onStart5sTask() {
        mDisposableObserver = getEventCompletionObserver();

        getObservableTask5SToComplete()
                .timeout(3, TimeUnit.SECONDS, onTimeoutObservable())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mDisposableObserver);
    }

    // -----------------------------------------------------------------------------------
    // Main Rx entities

    private Observable<String> getObservableTask5SToComplete() {
        return Observable.create(subscriber -> {
            log(String.format("Starting a 5s task"));
            subscriber.onNext("5 s");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            subscriber.onComplete();
        });
    }

    private Observable<String> getObservableTask2SToComplete() {
        return Observable.create(subscriber -> {
            log(String.format("Starting a 2s task"));
            subscriber.onNext("2 s");
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            subscriber.onComplete();
        });
    }

    private Observable<? extends String> onTimeoutObservable() {
        return Observable.create(subscriber -> {
            log("Timing out this task ...");
            subscriber.onError(new Throwable("Timeout Error"));
        });
    }

    private DisposableObserver<String> getEventCompletionObserver() {
        return new DisposableObserver<String>() {
            @Override
            public void onNext(String taskType) {
                log(String.format("onNext %s task", taskType));
            }

            @Override
            public void onError(Throwable e) {
                log(String.format("Dang a task timeout"));
                Timber.e(e, "Timeout Demo exception");
            }

            @Override
            public void onComplete() {
                log(String.format("task was completed"));
            }
        };
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogsList.setAdapter(mAdapter);
    }

    private void log(String logMsg) {

        if (isCurrentlyOnMainThread()) {
            mLogs.add(0, logMsg + " (main thread) ");
            mAdapter.clear();
            mAdapter.addAll(mLogs);
        } else {
            mLogs.add(0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread.
            new Handler(Looper.getMainLooper())
                    .post(() -> {
                        mAdapter.clear();
                        mAdapter.addAll(mLogs);
                    });
        }
    }

    private boolean isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
