package com.morihacky.android.rxjava.pagination;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;

class PaginationAutoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_LOG = 0;

    private final List<String> mItems = new ArrayList<>();
    private final RxBus mRxBus;

    PaginationAutoAdapter(RxBus bus) {
        mRxBus = bus;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ItemLogViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ItemLogViewHolder) holder).bindContent(mItems.get(position));

        boolean lastPositionReached = position == mItems.size() - 1;
        if (lastPositionReached) {
            mRxBus.send(new PageEvent());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return ITEM_LOG;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    void addItems(List<String> items) {
        mItems.addAll(items);
    }

    private static class ItemLogViewHolder extends RecyclerView.ViewHolder {

        ItemLogViewHolder(View itemView) {
            super(itemView);
        }

        static ItemLogViewHolder create(ViewGroup parent) {
            return new ItemLogViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false));
        }

        void bindContent(String content) {
            ((TextView) itemView).setText(content);
        }
    }

    static class PageEvent {

    }
}
