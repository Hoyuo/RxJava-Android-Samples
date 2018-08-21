package com.morihacky.android.rxjava.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;
import com.morihacky.android.rxjava.retrofit.User;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import static java.lang.String.format;

public class RetrofitAsyncTaskDeathFragment extends Fragment {

    @BindView(R.id.btn_demo_retrofit_async_death_username)
    EditText mUsername;

    @BindView(R.id.log_list)
    ListView mResultList;

    private GithubApi mGithubService;
    private ArrayAdapter<String> mAdapter;
    private Unbinder mUnbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String githubToken = getResources().getString(R.string.github_oauth_token);
        mGithubService = GithubService.createGithubService(githubToken);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_retrofit_async_task_death,
                container,
                false);
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

    @OnClick(R.id.btn_demo_retrofit_async_death)
    public void onGetGithubUserClicked() {
        mAdapter.clear();

    /*new AsyncTask<String, Void, User>() {
        @Override
        protected User doInBackground(String... params) {
            return mGithubService.getUser(params[0]);
        }

        @Override
        protected void onPostExecute(User user) {
            mAdapter.add(format("%s  = [%s: %s]", mUsername.getText(), user.name, user.email));
        }
    }.execute(mUsername.getText().toString());*/

        mGithubService.user(mUsername.getText().toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<User>() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(User user) {
                        mAdapter.add(format("%s  = [%s: %s]",
                                mUsername.getText(),
                                user.name,
                                user.email));
                    }
                });
    }

    // -----------------------------------------------------------------------------------

    private class GetGithubUser extends AsyncTask<String, Void, User> {

        @Override
        protected User doInBackground(String... params) {
            return mGithubService.getUser(params[0]);
        }

        @Override
        protected void onPostExecute(User user) {
            mAdapter.add(format("%s  = [%s: %s]", mUsername.getText(), user.name, user.email));
        }
    }
}
