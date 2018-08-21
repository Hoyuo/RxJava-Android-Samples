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

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hu.akarnokd.rxjava2.math.MathFlowable;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.subscribers.DisposableSubscriber;
import timber.log.Timber;

import static android.os.Looper.getMainLooper;

public class ExponentialBackoffFragment extends BaseFragment {

    @BindView(R.id.list_threading_log)
    ListView mLogList;

    private LogAdapter mLogAdapter;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private List<String> mLogs;
    Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_exponential_backoff, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLogger();
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

    // -----------------------------------------------------------------------------------

    @OnClick(R.id.btn_eb_retry)
    public void startRetryingWithExponentialBackoffStrategy() {
        mLogs = new ArrayList<>();
        mLogAdapter.clear();

        DisposableSubscriber<Object> disposableSubscriber = new DisposableSubscriber<Object>() {
            @Override
            public void onNext(Object aVoid) {
                Timber.d("on Next");
            }

            @Override
            public void onComplete() {
                Timber.d("on Completed");
            }

            @Override
            public void onError(Throwable e) {
                log("Error: I give up!");
            }
        };

        Flowable.error(new RuntimeException("testing")) // always fails
                .retryWhen(new RetryWithDelay(5,
                        1000)) // notice this is called only onError (onNext
                // values sent are ignored)
                .doOnSubscribe(subscription -> log(
                        "Attempting the impossible 5 times in intervals of 1s"))
                .subscribe(disposableSubscriber);

        mCompositeDisposable.add(disposableSubscriber);
    }

    @OnClick(R.id.btn_eb_delay)
    public void startExecutingWithExponentialBackoffDelay() {

        mLogs = new ArrayList<>();
        mLogAdapter.clear();

        DisposableSubscriber<Integer> disposableSubscriber = new DisposableSubscriber<Integer>() {
            @Override
            public void onNext(Integer integer) {
                Timber.d("executing Task %d [xx:%02d]", integer, getSecondHand());
                log(String.format("executing Task %d  [xx:%02d]", integer, getSecondHand()));
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e, "arrrr. Error");
                log("Error");
            }

            @Override
            public void onComplete() {
                Timber.d("onCompleted");
                log("Completed");
            }
        };

        Flowable.range(1, 4)
                .delay(integer -> {
                    // Rx-y way of doing the Fibonnaci :P
                    return MathFlowable.sumInt(Flowable.range(1, integer))
                            .flatMap(targetSecondDelay ->
                                    Flowable.just(integer)
                                            .delay(targetSecondDelay, TimeUnit.SECONDS));
                })
                .doOnSubscribe(s ->
                        log(String.format("Execute 4 tasks with delay - time now: [xx:%02d]",
                                getSecondHand()))
                )
                .subscribe(disposableSubscriber);

        mCompositeDisposable.add(disposableSubscriber);
    }

    // -----------------------------------------------------------------------------------

    private int getSecondHand() {
        long millis = System.currentTimeMillis();
        return (int) (TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    // -----------------------------------------------------------------------------------

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mLogAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogList.setAdapter(mLogAdapter);
    }

    private void log(String logMsg) {
        mLogs.add(logMsg);

        // You can only do below stuff on main thread.
        new Handler(getMainLooper())
                .post(() -> {
                    mLogAdapter.clear();
                    mLogAdapter.addAll(mLogs);
                });
    }

    // -----------------------------------------------------------------------------------

    // CAUTION:
    // --------------------------------------
    // THIS notificationHandler class HAS NO BUSINESS BEING non-static
    // I ONLY did this cause i wanted access to the `log` method from inside here
    // for the purpose of demonstration. In the real world, make it static and LET IT BE!!

    // It's 12am in the morning and i feel lazy dammit !!!

    //public static class RetryWithDelay
    public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Publisher<?>> {

        private final int mMaxRetries;
        private final int mRetryDelayMillis;
        private int mRetryCount;

        public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
            mMaxRetries = maxRetries;
            mRetryDelayMillis = retryDelayMillis;
            mRetryCount = 0;
        }

        // this is a notificationhandler, all that is cared about here
        // is the emission "type" not emission "content"
        // only onNext triggers a re-subscription (onError + onComplete kills it)

        @Override
        public Publisher<?> apply(Flowable<? extends Throwable> inputObservable) {

            // it is critical to use inputObservable in the chain for the result
            // ignoring it and doing your own thing will break the sequence

            return inputObservable.flatMap((Function<Throwable, Publisher<?>>) throwable -> {
                if (++mRetryCount < mMaxRetries) {

                    // When this Observable calls onNext, the original
                    // Observable will be retried (i.e. re-subscribed)

                    Timber.d("Retrying in %d ms", mRetryCount * mRetryDelayMillis);
                    log(String.format("Retrying in %d ms", mRetryCount * mRetryDelayMillis));

                    return Flowable.timer(mRetryCount * mRetryDelayMillis, TimeUnit.MILLISECONDS);
                }

                Timber.d("Argh! i give up");

                // Max retries hit. Pass an error so the chain is forcibly completed
                // only onNext triggers a re-subscription (onError + onComplete kills it)
                return Flowable.error(throwable);
            });
        }
    }
}
