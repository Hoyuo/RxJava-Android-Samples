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

import com.morihacky.android.rxjava.R;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import timber.log.Timber;

public class PollingFragment extends BaseFragment {

    private static final int INITIAL_DELAY = 0;
    private static final int POLLING_INTERVAL = 1000;
    private static final int POLL_COUNT = 8;

    @BindView(R.id.list_threading_log)
    ListView mLogsList;

    private LogAdapter mLogAdapter;
    private int mCounter = 0;
    private CompositeDisposable mDisposable;
    private List<String> mLogs;
    private Unbinder mUnbinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisposable = new CompositeDisposable();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_polling, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLogger();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.clear();
        mUnbinder.unbind();
    }

    @OnClick(R.id.btn_start_simple_polling)
    public void onStartSimplePollingClicked() {

        final int pollCount = POLL_COUNT;

        Disposable d = Flowable.interval(INITIAL_DELAY, POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .map(this::doNetworkCallAndGetStringResult)
                .take(pollCount)
                .doOnSubscribe(subscription ->
                        log(String.format("Start simple polling - %s", mCounter))
                )
                .subscribe(taskName ->
                        log(String.format(Locale.US,
                                "Executing polled task [%s] now time : [xx:%02d]",
                                taskName,
                                getSecondHand()))
                );

        mDisposable.add(d);
    }

    @OnClick(R.id.btn_start_increasingly_delayed_polling)
    public void onStartIncreasinglyDelayedPolling() {
        setupLogger();

        final int pollingInterval = POLLING_INTERVAL;
        final int pollCount = POLL_COUNT;

        log(String.format(
                Locale.US,
                "Start increasingly delayed polling now time: [xx:%02d]",
                getSecondHand()));

        mDisposable.add(Flowable.just(1L)
                .repeatWhen(new RepeatWithDelay(pollCount, pollingInterval))
                .subscribe(o -> log(
                        String.format(
                                Locale.US,
                                "Executing polled task now time : [xx:%02d]",
                                getSecondHand())),
                        e -> Timber.d(e, "arrrr. Error")
                )
        );
    }

    // -----------------------------------------------------------------------------------

    // CAUTION:
    // --------------------------------------
    // THIS notificationHandler class HAS NO BUSINESS BEING non-static
    // I ONLY did this cause i wanted access to the `log` method from inside here
    // for the purpose of demonstration. In the real world, make it static and LET IT BE!!

    // It's 12am in the morning and i feel lazy dammit !!!

    private String doNetworkCallAndGetStringResult(long attempt) {
        try {
            if (attempt == 4) {
                // randomly make one event super long so we test that the repeat logic waits
                // and accounts for this.
                Thread.sleep(9000);
            } else {
                Thread.sleep(3000);
            }

        } catch (InterruptedException e) {
            Timber.d("Operation was interrupted");
        }
        mCounter++;

        return String.valueOf(mCounter);
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private int getSecondHand() {
        long millis = System.currentTimeMillis();
        return (int) (TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
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
        mCounter = 0;
    }

    private boolean isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    //public static class RepeatWithDelay
    public class RepeatWithDelay implements Function<Flowable<Object>, Publisher<Long>> {

        private final int mRepeatLimit;
        private final int mPollingInterval;
        private int mRepeatCount = 1;

        RepeatWithDelay(int repeatLimit, int pollingInterval) {
            mPollingInterval = pollingInterval;
            mRepeatLimit = repeatLimit;
        }

        // this is a notificationhandler, all we care about is
        // the emission "type" not emission "content"
        // only onNext triggers a re-subscription

        @Override
        public Publisher<Long> apply(Flowable<Object> inputFlowable) throws Exception {
            // it is critical to use inputObservable in the chain for the result
            // ignoring it and doing your own thing will break the sequence

            return inputFlowable.flatMap((Function<Object, Publisher<Long>>) o -> {
                if (mRepeatCount >= mRepeatLimit) {
                    // terminate the sequence cause we reached the limit
                    log("Completing sequence");
                    return Flowable.empty();
                }

                // since we don't get an input
                // we store state in this handler to tell us the point of time we're firing
                mRepeatCount++;

                return Flowable.timer(mRepeatCount * mPollingInterval,
                        TimeUnit.MILLISECONDS);
            });
        }
    }

    private class LogAdapter extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }
}
