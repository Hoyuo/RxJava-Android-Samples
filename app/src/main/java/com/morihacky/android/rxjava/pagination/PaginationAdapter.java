package com.morihacky.android.rxjava.pagination;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;

/** There isn't anything specific to Pagination here. Just wiring for the example */
class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_LOG = 0;
    private static final int ITEM_BTN = 1;

    private final List<String> mItems = new ArrayList<>();
    private final RxBus mRxBus;

    PaginationAdapter(RxBus bus) {
        mRxBus = bus;
    }

    void addItems(List<String> items) {
        mItems.addAll(items);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mItems.size()) {
            return ITEM_BTN;
        }

        return ITEM_LOG;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_BTN:
                return ItemBtnViewHolder.create(parent);
            default:
                return ItemLogViewHolder.create(parent);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_LOG:
                ((ItemLogViewHolder) holder).bindContent(mItems.get(position));
                return;
            case ITEM_BTN:
                ((ItemBtnViewHolder) holder).bindContent(mRxBus);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size() + 1; // add 1 for paging button
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

    static class ItemBtnViewHolder extends RecyclerView.ViewHolder {

        ItemBtnViewHolder(View itemView) {
            super(itemView);
        }

        static ItemBtnViewHolder create(ViewGroup parent) {
            return new ItemBtnViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_btn, parent, false));
        }

        void bindContent(RxBus bus) {
            ((Button) itemView).setText(R.string.btn_demo_pagination_more);
            itemView.setOnClickListener(v -> bus.send(new ItemBtnViewHolder.PageEvent()));
        }

        static class PageEvent {

        }
    }
}
