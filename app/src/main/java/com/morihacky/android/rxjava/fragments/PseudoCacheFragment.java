package com.morihacky.android.rxjava.fragments;

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
import android.widget.TextView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.Contributor;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PseudoCacheFragment extends BaseFragment {

    @BindView(R.id.info_pseudoCache_demo)
    TextView mInfoText;

    @BindView(R.id.info_pseudoCache_listSubscription)
    ListView mListSubscriptionInfo;

    @BindView(R.id.info_pseudoCache_listDtl)
    ListView mListDetail;

    private ArrayAdapter<String> mAdapterDetail, mAdapterSubscriptionInfo;
    private HashMap<String, Long> mContributionMap = null;
    private Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_pseudo_cache, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @OnClick(R.id.btn_pseudoCache_concat)
    public void onConcatBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_concat);
        wireupDemo();

        Observable.concat(getSlowCachedDiskData(), getFreshNetworkData())
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    @OnClick(R.id.btn_pseudoCache_concatEager)
    public void onConcatEagerBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_concatEager);
        wireupDemo();

        List<Observable<Contributor>> observables = new ArrayList<>(2);
        observables.add(getSlowCachedDiskData());
        observables.add(getFreshNetworkData());

        Observable.concatEager(observables)
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    @OnClick(R.id.btn_pseudoCache_merge)
    public void onMergeBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_merge);
        wireupDemo();

        Observable.merge(getCachedDiskData(), getFreshNetworkData())
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    @OnClick(R.id.btn_pseudoCache_mergeSlowDisk)
    public void onMergeSlowBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_mergeSlowDisk);
        wireupDemo();

        Observable.merge(getSlowCachedDiskData(), getFreshNetworkData())
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    @OnClick(R.id.btn_pseudoCache_mergeOptimized)
    public void onMergeOptimizedBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_mergeOptimized);
        wireupDemo();

        getFreshNetworkData() //
                .publish(network -> //
                        Observable.merge(
                                network, //
                                getCachedDiskData().takeUntil(network)))
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    @OnClick(R.id.btn_pseudoCache_mergeOptimizedSlowDisk)
    public void onMergeOptimizedWithSlowDiskBtnClicked() {
        mInfoText.setText(R.string.msg_pseudoCache_demoInfo_mergeOptimizedSlowDisk);
        wireupDemo();

        getFreshNetworkData() //
                .publish(network -> //
                        Observable.merge(
                                network, //
                                getSlowCachedDiskData().takeUntil(network)))
                .subscribeOn(Schedulers.io()) // we want to add a list item at time of subscription
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Contributor>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        mContributionMap.put(contributor.login, contributor.contributions);
                        mAdapterDetail.clear();
                        mAdapterDetail.addAll(mapAsList(mContributionMap));
                    }
                });
    }

    // -----------------------------------------------------------------------------------
    // WIRING for example

    private void wireupDemo() {
        mContributionMap = new HashMap<>();

        mAdapterDetail = new ArrayAdapter<>(
                getActivity(), R.layout.item_log_white, R.id.item_log, new ArrayList<>());
        mListDetail.setAdapter(mAdapterDetail);

        mAdapterSubscriptionInfo = new ArrayAdapter<>(
                getActivity(), R.layout.item_log_white, R.id.item_log, new ArrayList<>());
        mListSubscriptionInfo.setAdapter(mAdapterSubscriptionInfo);
    }

    private Observable<Contributor> getSlowCachedDiskData() {
        return Observable.timer(1, TimeUnit.SECONDS).flatMap(dummy -> getCachedDiskData());
    }

    private Observable<Contributor> getCachedDiskData() {
        List<Contributor> list = new ArrayList<>();
        Map<String, Long> map = dummyDiskData();

        for (String username : map.keySet()) {
            Contributor c = new Contributor();
            c.login = username;
            c.contributions = map.get(username);
            list.add(c);
        }

        return Observable.fromIterable(list) //
                .doOnSubscribe((data) -> new Handler(Looper.getMainLooper()) //
                        .post(() -> mAdapterSubscriptionInfo.add("(disk) cache subscribed"))) //
                .doOnComplete(() -> new Handler(Looper.getMainLooper()) //
                        .post(() -> mAdapterSubscriptionInfo.add("(disk) cache completed")));
    }

    private Observable<Contributor> getFreshNetworkData() {
        String githubToken = getResources().getString(R.string.github_oauth_token);
        GithubApi githubService = GithubService.createGithubService(githubToken);

        return githubService
                .contributors("square", "retrofit")
                .flatMap(Observable::fromIterable)
                .doOnSubscribe((data) -> new Handler(Looper.getMainLooper()) //
                        .post(() -> mAdapterSubscriptionInfo.add("(network) subscribed"))) //
                .doOnComplete(() -> new Handler(Looper.getMainLooper()) //
                        .post(() -> mAdapterSubscriptionInfo.add("(network) completed")));
    }

    private List<String> mapAsList(HashMap<String, Long> map) {
        List<String> list = new ArrayList<>();

        for (String username : map.keySet()) {
            String rowLog = String.format("%s [%d]", username, mContributionMap.get(username));
            list.add(rowLog);
        }

        return list;
    }

    private Map<String, Long> dummyDiskData() {
        Map<String, Long> map = new HashMap<>();
        map.put("JakeWharton", 0L);
        map.put("pforhan", 0L);
        map.put("edenman", 0L);
        map.put("swankjesse", 0L);
        map.put("bruceLee", 0L);
        return map;
    }
}
