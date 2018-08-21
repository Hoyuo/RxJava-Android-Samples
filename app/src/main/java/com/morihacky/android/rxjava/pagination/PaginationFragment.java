package com.morihacky.android.rxjava.pagination;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.morihacky.android.rxjava.MainActivity;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;
import com.morihacky.android.rxjava.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;

public class PaginationFragment extends BaseFragment {

    @BindView(R.id.list_paging)
    RecyclerView mPagingList;

    @BindView(R.id.progress_paging)
    ProgressBar mProgressBar;

    private PaginationAdapter mPaginationAdapter;
    private RxBus mRxBus;
    private CompositeDisposable mCompositeDisposable;
    private PublishProcessor<Integer> mPaginator;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRxBus = ((MainActivity) getActivity()).getRxBusSingleton();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mPagingList.setLayoutManager(layoutManager);

        mPaginationAdapter = new PaginationAdapter(mRxBus);
        mPagingList.setAdapter(mPaginationAdapter);

        mPaginator = PublishProcessor.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCompositeDisposable = new CompositeDisposable();

        Disposable d2 = mPaginator.onBackpressureDrop()
                .concatMap(nextPage -> itemsFromNetworkCall(nextPage + 1, 10))
                .observeOn(AndroidSchedulers.mainThread())
                .map(items -> {
                    int start = mPaginationAdapter.getItemCount() - 1;

                    mPaginationAdapter.addItems(items);
                    mPaginationAdapter.notifyItemRangeInserted(start, 10);

                    mProgressBar.setVisibility(View.INVISIBLE);

                    return items;
                })
                .subscribe();

        // I'm using an Rxbus purely to hear from a nested button click
        // we don't really need Rx for this part. it's just easy ¯\_(ツ)_/¯
        Disposable d1 = mRxBus.asFlowable()
                .subscribe(event -> {
                    if (event instanceof PaginationAdapter.ItemBtnViewHolder.PageEvent) {

                        // trigger the paginator for the next event
                        int nextPage = mPaginationAdapter.getItemCount() - 1;
                        mPaginator.onNext(nextPage);
                    }
                });

        mCompositeDisposable.add(d1);
        mCompositeDisposable.add(d2);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCompositeDisposable.clear();
    }

    /** Fake Observable that simulates a network call and then sends down a list of items */
    private Flowable<List<String>> itemsFromNetworkCall(int start, int count) {
        return Flowable.just(true)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(dummy -> mProgressBar.setVisibility(View.VISIBLE))
                .delay(2, TimeUnit.SECONDS)
                .map(dummy -> {
                    List<String> items = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        items.add("Item " + (start + i));
                    }
                    return items;
                });
    }

    // -----------------------------------------------------------------------------------
    // WIRING up the views required for this example

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_pagination, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }
}
