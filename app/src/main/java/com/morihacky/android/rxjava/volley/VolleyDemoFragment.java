package com.morihacky.android.rxjava.volley;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;
import timber.log.Timber;

public class VolleyDemoFragment extends BaseFragment {

    public static final String TAG = "VolleyDemoFragment";

    @BindView(R.id.list_threading_log)
    ListView mLogsListView;

    private List<String> mLogs;
    private LogAdapter mLogAdapter;
    private Unbinder mUnbinder;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_volley, container, false);
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

    /**
     * Creates and returns an observable generated from the Future returned from {@code
     * getRouteData()}. The observable can then be subscribed to as shown in {@code
     * startVolleyRequest()}
     *
     * @return Observable<JSONObject>
     */
    public Flowable<JSONObject> newGetRouteData() {
        return Flowable.defer(() -> {
            try {
                return Flowable.just(getRouteData());
            } catch (InterruptedException | ExecutionException e) {
                Timber.tag("routes").e(e);
                return Flowable.error(e);
            }
        });
    }

    @OnClick(R.id.btn_start_operation)
    void startRequest() {
        startVolleyRequest();
    }

    private void startVolleyRequest() {
        DisposableSubscriber<JSONObject> d = new DisposableSubscriber<JSONObject>() {
            @Override
            public void onNext(JSONObject jsonObject) {
                Timber.tag(TAG).e("onNext %s", jsonObject.toString());
                log("onNext " + jsonObject.toString());
            }

            @Override
            public void onError(Throwable e) {
                VolleyError cause = (VolleyError) e.getCause();
                String s = new String(cause.networkResponse.data, Charset.forName("UTF-8"));
                Log.e(TAG, s);
                Log.e(TAG, cause.toString());
                log("onError " + s);
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onCompleted");
                Timber.d("----- onCompleted");
                log("onCompleted ");
            }
        };

        newGetRouteData().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d);

        mCompositeDisposable.add(d);
    }

    /**
     * Converts the Asynchronous Request into a Synchronous Future that can be used to block via
     * {@code Future.get()}. Observables require blocking/synchronous functions
     *
     * @return JSONObject
     */
    private JSONObject getRouteData() throws ExecutionException, InterruptedException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        String url = "http://www.weather.com.cn/adat/sk/101010100.html";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, future, future);
        MyVolley.getRequestQueue().add(req);
        return future.get();
    }

    // -----------------------------------------------------------------------------------
    // Methods that help wiring up the example (irrelevant to RxJava)

    private void setupLogger() {
        mLogs = new ArrayList<>();
        mLogAdapter = new LogAdapter(getActivity(), new ArrayList<>());
        mLogsListView.setAdapter(mLogAdapter);
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

    private boolean isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
