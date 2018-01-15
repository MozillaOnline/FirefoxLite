package org.mozilla.focus.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.history.HistoryItemAdapter;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.widget.FragmentListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozillabeijing on 2017/12/14.
 */

public class HotListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{

    public List<Site> mItems = new ArrayList();
    //private RecyclerView mRecyclerView;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener = null;

    public HotListAdapter(List<Site> data, Context context) {
        //this.mRecyclerView = recyclerView;
        this.mItems = data;
        mContext = context;
    }

    public void updateData(List<Site> data) {
        this.mItems = data;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hotlist, parent, false);
        SiteViewHolder viewHolder = new SiteViewHolder(v);
        v.setOnClickListener(this);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //holder.mTv.setText(mData.get(position));
        final Site item = (Site) mItems.get(position);
        holder.itemView.setTag(position);

        if(item != null) {
            final HotListAdapter.SiteViewHolder siteVH = (HotListAdapter.SiteViewHolder) holder;
            //siteVH.rootView.setOnClickListener(this);
            siteVH.textMain.setText(item.getTitle().toString());
            Log.e("Topsite",item.getTitle());
            Log.e("Topsite",siteVH.textMain.getText().toString());
            Log.e("Topsite",siteVH.textMain.getTextColors().toString());
            siteVH.textSecondary.setText(item.getUrl());
            Bitmap bmpFav = item.getFavIcon();
            if (bmpFav != null) {
                siteVH.imgFav.setImageBitmap(bmpFav);
            } else {
                siteVH.imgFav.setImageResource(R.drawable.ic_globe);
            }
        }
    }

   private static class SiteViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup rootView;
        private ImageView imgFav;
        private TextView textMain, textSecondary;
        private FrameLayout btnMore;

        public SiteViewHolder(View itemView) {
            super(itemView);
            rootView = (ViewGroup) itemView.findViewById(R.id.hotlist_item_root_view);
            imgFav = (ImageView) itemView.findViewById(R.id.hotlist_item_img_fav);
            textMain = (TextView) itemView.findViewById(R.id.hotlist_item_text_main);
            textSecondary = (TextView) itemView.findViewById(R.id.hotlist_item_text_secondary);
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    public static interface OnItemClickListener {
        void onItemClick(View view , int position);
    }



    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(v,(int)v.getTag());
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    /*public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTv;

        public ViewHolder(View itemView) {
            super(itemView);
            mTv = (TextView) itemView.findViewById(R.id.item_tv);
        }
    }*/
}