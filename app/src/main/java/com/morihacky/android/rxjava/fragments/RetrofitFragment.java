package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.Contributor;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;
import com.morihacky.android.rxjava.retrofit.User;

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

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

public class RetrofitFragment extends Fragment {

    @BindView(R.id.demo_retrofit_contributors_username)
    EditText mUserName;

    @BindView(R.id.demo_retrofit_contributors_repository)
    EditText mRepo;

    @BindView(R.id.log_list)
    ListView mResultList;

    private ArrayAdapter<String> mAdapter;
    private GithubApi mGithubService;
    private CompositeDisposable mCompositeDisposable;
    private Unbinder mUnbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String githubToken = getResources().getString(R.string.github_oauth_token);
        mGithubService = GithubService.createGithubService(githubToken);

        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_retrofit, container, false);
        mUnbinder = ButterKnife.bind(this, layout);

        mAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.item_log, R.id.item_log, new ArrayList<>());
        //mAdapter.setNotifyOnChange(true);
        mResultList.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    @OnClick(R.id.btn_demo_retrofit_contributors)
    public void onListContributorsClicked() {
        mAdapter.clear();

        mCompositeDisposable.add( //
                mGithubService
                        .contributors(mUserName.getText().toString(), mRepo.getText().toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<List<Contributor>>() {

                            @Override
                            public void onComplete() {
                                Timber.d("Retrofit call 1 completed");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Timber.e(e,
                                        "woops we got an error while getting the list of contributors");
                            }

                            @Override
                            public void onNext(List<Contributor> contributors) {
                                for (Contributor c : contributors) {
                                    mAdapter.add(
                                            format("%s has made %d contributions to %s",
                                                    c.login,
                                                    c.contributions,
                                                    mRepo.getText().toString()));

                                    Timber.d("%s has made %d contributions to %s",
                                            c.login,
                                            c.contributions,
                                            mRepo.getText().toString());
                                }
                            }
                        }));
    }

    @OnClick(R.id.btn_demo_retrofit_contributors_with_user_info)
    public void onListContributorsWithFullUserInfoClicked() {
        mAdapter.clear();

        mCompositeDisposable.add(mGithubService
                .contributors(mUserName.getText().toString(), mRepo.getText().toString())
                .flatMap(Observable::fromIterable)
                .flatMap(contributor -> {
                    Observable<User> userObservable =
                            mGithubService.user(contributor.login)
                                    .filter(user -> !isEmpty(user.name) && !isEmpty(user.email));

                    return Observable.zip(userObservable, Observable.just(contributor), Pair::new);
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Pair<User, Contributor>>() {
                    @Override
                    public void onComplete() {
                        Timber.d("Retrofit call 2 completed ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e,
                                "error while getting the list of contributors along with full names");
                    }

                    @Override
                    public void onNext(Pair<User, Contributor> pair) {
                        User user = pair.first;
                        Contributor contributor = pair.second;

                        mAdapter.add(format("%s(%s) has made %d contributions to %s",
                                user.name,
                                user.email,
                                contributor.contributions,
                                mRepo.getText().toString()));

                        mAdapter.notifyDataSetChanged();

                        Timber.d("%s(%s) has made %d contributions to %s",
                                user.name,
                                user.email,
                                contributor.contributions,
                                mRepo.getText().toString());
                    }
                }));
    }
}
