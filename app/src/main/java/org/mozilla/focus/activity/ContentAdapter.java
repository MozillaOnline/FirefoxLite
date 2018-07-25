package org.mozilla.focus.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozillabeijing on 2018/7/25.
 */

public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{
    public List<Content> mItems = new ArrayList();
    private Context mContext;
    private ContentAdapter.OnItemClickListener mOnItemClickListener = null;
    //private Handler mHandler = new Handler(Looper.getMainLooper());

    public ContentAdapter(List<Content> data, Context context) {
        //this.mRecyclerView = recyclerView;
        this.mItems = data;
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content, parent, false);
        ContentViewHolder viewHolder = new ContentViewHolder(v);
        v.setOnClickListener(this);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Content item = (Content) mItems.get(position);
        holder.itemView.setTag(position);

        if(item != null) {
            final ContentViewHolder contentVH = (ContentViewHolder) holder;
            //siteVH.rootView.setOnClickListener(this);
            contentVH.textMain.setText(item.getTitle().toString());
            Log.e("Topsite",item.getTitle());
            contentVH.textSecondary.setText(item.getContent());
            Bitmap bmpImg = null;
            String imgUrl = item.getImgUrl();
            new AsyncTaskLoadImage(contentVH.img).execute(imgUrl);

            contentVH.textSource.setText(item.getSource());
            contentVH.textDate.setText(item.getDate());
        }
    }

    private class AsyncTaskLoadImage  extends android.os.AsyncTask<String, String, Bitmap> {
        private final static String TAG = "AsyncTaskLoadImage";
        private final WeakReference<ImageView> imageViewReference;

        public AsyncTaskLoadImage(ImageView imv) {
            imageViewReference = new WeakReference<ImageView>(imv);
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                String str = params[0];
                Log.e("DisplayContentAdapter",str);
                URL url = new URL(str);
                bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static class ContentViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup rootView;
        private ImageView img;
        private TextView textMain, textSecondary, textSource, textDate;

        public ContentViewHolder(View itemView) {
            super(itemView);
            rootView = (ViewGroup) itemView.findViewById(R.id.content_item_root_view);
            img = (ImageView) itemView.findViewById(R.id.content_item_img);
            textMain = (TextView) itemView.findViewById(R.id.content_item_title);
            textSecondary = (TextView) itemView.findViewById(R.id.content_item_text);
            textSource = (TextView) itemView.findViewById(R.id.content_item_source);
            textDate = (TextView) itemView.findViewById(R.id.content_item_date);
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
    public void updateList(List<Content> newData){
        if (newData != null) {
            mItems.addAll(newData);
        }
        notifyDataSetChanged();
    }
}
