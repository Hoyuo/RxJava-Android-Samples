package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.Contributor;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PseudoCacheMergeFragment extends BaseFragment {

    @BindView(R.id.log_list)
    ListView mResultList;

    private ArrayAdapter<String> mArrayAdapter;
    private HashMap<String, Long> mContributionMap = null;
    private HashMap<Contributor, Long> mResultAgeMap = new HashMap<>();
    private Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_pseudo_cache_concat, container, false);
        mUnbinder = ButterKnife.bind(this, layout);
        initializeCache();
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @OnClick(R.id.btn_start_pseudo_cache)
    public void onDemoPseudoCacheClicked() {
        mArrayAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.item_log, R.id.item_log, new ArrayList<>());

        mResultList.setAdapter(mArrayAdapter);
        initializeCache();

        Observable.merge(getCachedData(), getFreshData())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Pair<Contributor, Long>>() {
                    @Override
                    public void onComplete() {
                        Timber.d("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Pair<Contributor, Long> contributorAgePair) {
                        Contributor contributor = contributorAgePair.first;

                        if (mResultAgeMap.containsKey(contributor)
                                && mResultAgeMap.get(contributor)
                                > contributorAgePair.second) {
                            return;
                        }

                        mContributionMap.put(contributor.login, contributor.contributions);
                        mResultAgeMap.put(contributor, contributorAgePair.second);

                        mArrayAdapter.clear();
                        mArrayAdapter.addAll(getListStringFromMap());
                    }
                });
    }

    private List<String> getListStringFromMap() {
        List<String> list = new ArrayList<>();

        for (String username : mContributionMap.keySet()) {
            String rowLog = String.format("%s [%d]", username, mContributionMap.get(username));
            list.add(rowLog);
        }

        return list;
    }

    private Observable<Pair<Contributor, Long>> getCachedData() {

        List<Pair<Contributor, Long>> list = new ArrayList<>();

        Pair<Contributor, Long> dataWithAgePair;

        for (String username : mContributionMap.keySet()) {
            Contributor c = new Contributor();
            c.login = username;
            c.contributions = mContributionMap.get(username);

            dataWithAgePair = new Pair<>(c, System.currentTimeMillis());
            list.add(dataWithAgePair);
        }

        return Observable.fromIterable(list);
    }

    private Observable<Pair<Contributor, Long>> getFreshData() {
        String githubToken = getResources().getString(R.string.github_oauth_token);
        GithubApi githubService = GithubService.createGithubService(githubToken);

        return githubService
                .contributors("square", "retrofit")
                .flatMap(Observable::fromIterable)
                .map(contributor -> new Pair<>(contributor, System.currentTimeMillis()));
    }

    private void initializeCache() {
        mContributionMap = new HashMap<>();
        mContributionMap.put("JakeWharton", 0L);
        mContributionMap.put("pforhan", 0L);
        mContributionMap.put("edenman", 0L);
        mContributionMap.put("swankjesse", 0L);
        mContributionMap.put("bruceLee", 0L);
    }
}
