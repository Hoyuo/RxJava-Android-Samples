package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Flowable;
import io.reactivex.subscribers.DefaultSubscriber;
import io.reactivex.subscribers.DisposableSubscriber;
import timber.log.Timber;

import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;

public class TimingDemoFragment extends BaseFragment {

    @BindView(R.id.list_threading_log)
    ListView mLogsList;

    private LogAdapter mAdapter;
    private List<String> mLogs;

    private DisposableSubscriber<Long> mSubscriber1;
    private DisposableSubscriber<Long> mSubscriber2;
    private Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_demo_timing, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLogger();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
    // -----------------------------------------------------------------------------------

    @OnClick(R.id.btn_demo_timing_1)
    public void btn1_RunSingleTaskAfter2s() {
        log(String.format("A1 [%s] --- BTN click", getCurrentTimestamp()));

        Flowable.timer(2, TimeUnit.SECONDS) //
                .subscribe(new DefaultSubscriber<Long>() {
                    @Override
                    public void onNext(Long number) {
                        log(String.format("A1 [%s]     NEXT", getCurrentTimestamp()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "something went wrong in TimingDemoFragment example");
                    }

                    @Override
                    public void onComplete() {
                        log(String.format("A1 [%s] XXX COMPLETE", getCurrentTimestamp()));
                    }
                });
    }

    @OnClick(R.id.btn_demo_timing_2)
    public void btn2_RunTask_IntervalOf1s() {
        if (mSubscriber1 != null && !mSubscriber1.isDisposed()) {
            mSubscriber1.dispose();
            log(String.format("B2 [%s] XXX BTN KILLED", getCurrentTimestamp()));
            return;
        }

        log(String.format("B2 [%s] --- BTN click", getCurrentTimestamp()));

        mSubscriber1 = new DisposableSubscriber<Long>() {
            @Override
            public void onComplete() {
                log(String.format("B2 [%s] XXXX COMPLETE", getCurrentTimestamp()));
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "something went wrong in TimingDemoFragment example");
            }

            @Override
            public void onNext(Long number) {
                log(String.format("B2 [%s]     NEXT", getCurrentTimestamp()));
            }
        };

        Flowable.interval(1, TimeUnit.SECONDS).subscribe(mSubscriber1);
    }

    @OnClick(R.id.btn_demo_timing_3)
    public void btn3_RunTask_IntervalOf1s_StartImmediately() {
        if (mSubscriber2 != null && !mSubscriber2.isDisposed()) {
            mSubscriber2.dispose();
            log(String.format("C3 [%s] XXX BTN KILLED", getCurrentTimestamp()));
            return;
        }

        log(String.format("C3 [%s] --- BTN click", getCurrentTimestamp()));

        mSubscriber2 = new DisposableSubscriber<Long>() {
            @Override
            public void onNext(Long number) {
                log(String.format("C3 [%s]     NEXT", getCurrentTimestamp()));
            }

            @Override
            public void onComplete() {
                log(String.format("C3 [%s] XXXX COMPLETE", getCurrentTimestamp()));
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "something went wrong in TimingDemoFragment example");
            }
        };

        Flowable.interval(0, 1, TimeUnit.SECONDS).subscribe(mSubscriber2);
    }

    @OnClick(R.id.btn_demo_timing_4)
    public void btn4_RunTask5Times_IntervalOf3s() {
        log(String.format("D4 [%s] --- BTN click", getCurrentTimestamp()));

        Flowable.interval(3, TimeUnit.SECONDS)
                .take(5)
                .subscribe(new DefaultSubscriber<Long>() {
                    @Override
                    public void onNext(Long number) {
                        log(String.format("D4 [%s]     NEXT", getCurrentTimestamp()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "something went wrong in TimingDemoFragment example");
                    }

                    @Override
                    public void onComplete() {
                        log(String.format("D4 [%s] XXX COMPLETE", getCurrentTimestamp()));
                    }
                });
    }

    @OnClick(R.id.btn_demo_timing_5)
    public void btn5_RunTask5Times_IntervalOf3s() {
        log(String.format("D5 [%s] --- BTN click", getCurrentTimestamp()));

        Flowable.just("Do task A right away")
                .doOnNext(input -> log(String.format("D5 %s [%s]", input, getCurrentTimestamp())))
                .delay(1, TimeUnit.SECONDS)
                .doOnNext(oldInput ->
                        log(String.format(
                                "D5 %s [%s]",
                                "Doing Task B after a delay",
                                getCurrentTimestamp())))
                .subscribe(new DefaultSubscriber<String>() {
                    @Override
                    public void onComplete() {
                        log(String.format("D5 [%s] XXX COMPLETE", getCurrentTimestamp()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "something went wrong in TimingDemoFragment example");
                    }

                    @Override
                    public void onNext(String number) {
                        log(String.format("D5 [%s]     NEXT", getCurrentTimestamp()));
                    }
                });
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    @OnClick(R.id.btn_clr)
    public void OnClearLog() {
        mLogs = new ArrayList<>();
        mAdapter.clear();
    }

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogsList.setAdapter(mAdapter);
    }

    private void log(String logMsg) {
        mLogs.add(0, String.format(logMsg + " [MainThread: %b]", getMainLooper() == myLooper()));

        // You can only do below stuff on main thread.
        new Handler(getMainLooper())
                .post(() -> {
                    mAdapter.clear();
                    mAdapter.addAll(mLogs);
                });
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("k:m:s:S a", Locale.getDefault()).format(new Date());
    }
}
