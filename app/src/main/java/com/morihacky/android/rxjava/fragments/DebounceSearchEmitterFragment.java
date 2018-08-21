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
import android.widget.EditText;
import android.widget.ListView;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent;
import com.morihacky.android.rxjava.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

import static com.morihacky.android.rxjava.util.CoreNullnessUtils.isNotNullOrEmpty;
import static java.lang.String.format;

public class DebounceSearchEmitterFragment extends BaseFragment {

    @BindView(R.id.list_threading_log)
    ListView mLogsList;

    @BindView(R.id.input_txt_debounce)
    EditText mInputSearchText;

    private LogAdapter mAdapter;
    private List<String> mLogs;

    private Disposable mDisposable;
    private Unbinder mUnbinder;

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
        mUnbinder.unbind();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_debounce, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @OnClick(R.id.clr_debounce)
    public void onClearLog() {
        mLogs = new ArrayList<>();
        mAdapter.clear();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        setupLogger();

        mDisposable = RxTextView.textChangeEvents(mInputSearchText)
                .debounce(400, TimeUnit.MILLISECONDS) // default Scheduler is Computation
                .filter(changes -> isNotNullOrEmpty(changes.text().toString()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getSearchObserver());
    }

    // -----------------------------------------------------------------------------------
    // Main Rx entities

    private DisposableObserver<TextViewTextChangeEvent> getSearchObserver() {
        return new DisposableObserver<TextViewTextChangeEvent>() {
            @Override
            public void onComplete() {
                Timber.d("--------- onComplete");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "--------- Woops on error!");
                log("Dang error. check your logs");
            }

            @Override
            public void onNext(TextViewTextChangeEvent onTextChangeEvent) {
                log(format("Searching for %s", onTextChangeEvent.text().toString()));
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

    private class LogAdapter extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }
}
