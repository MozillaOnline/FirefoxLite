package org.mozilla.focus.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.navigation.ScreenNavigator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozillabeijing on 2018/7/25.
 */

public class DisplayContentActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{
    private RecyclerView contentListView;
    private SwipeRefreshLayout refreshLayout;
    private ContentAdapter mAdapter;
    private int PAGE_COUNT = 5;
    private LinearLayoutManager mLayoutManager;
    private Context context;

    private List<Content> data = new ArrayList<>();
    private int lastVisibleItem = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_displaycontent);
        //initData();
        initViews();
    }

    private void initViews(){
        context = this;
        initData();
        contentListView = (RecyclerView) findViewById(R.id.content_recyclerview);
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        contentListView.setVisibility(View.VISIBLE);
        contentListView.setLayoutManager(mLayoutManager);


    }

    private void initData(){
        //JSONArray obj = sendRequest();
        JSONArray obj = null;
        new AsyncTaskSendRequest(obj).execute();
        Log.e("DisplayContent","execute");
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        Log.e("DisplayContent","mLayoutManager");
       // mAdapter = new ContentAdapter(getData(obj), this);
        new AsyncTaskUpdate(obj).execute();
    }

    @Override
    public void onRefresh() {
        // 设置可见
        refreshLayout.setRefreshing(true);
        // 重置adapter的数据源为空
        /*mAdapter.resetDatas();

        if(arr!=null){
            updateRecyclerView(arr);
        }*/

    }
    private List<Content> getData(JSONArray jsonArray) {
        Log.e("DisplayContent","getData");

        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json_content = (JSONObject)jsonArray.get(i);
                    final Content content = new Content();
                    String imgUrl = json_content.getJSONArray("image").get(0).toString();
                    imgUrl.replace("\\","");
                    Log.e("DisplayContent",imgUrl);
                    //Bitmap bmp = null;
                    //new AsyncTaskLoadImage(bmp).execute(imgUrl);
                    //content.setId(json_content.getLong("id"));
                    content.setTitle(json_content.getString("title"));
                    content.setUrl(json_content.getString("url"));
                    content.setContent(json_content.getString("summary"));
                    content.setImgUrl(imgUrl);
                    //content.setSource(json_content.getString("source"));
                    //content.setDate(json_content.getString("date"));
                    data.add(content);
                }
            }
        }  catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    private JSONArray sendRequest(){
        Log.e("DisplayContent","sendRequest");
        JSONArray arr = new JSONArray();
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL("http://api.tepintehui.com/index.php?m=firefox&c=article&a=get_article&token=a60a09b9ecaf074bc1cf657a75e7007e");
            //URL url = new URL("https://m.g-fox.cn/cnrocket.gif?clientID=526a8ef8-df23-4afc-812a-964177de67a2&device=HUAWEI-HUAWEI+NXT-AL10&type=start&documentID=ef3890df-175b-4743-b45b-2e9dcbb0ef90&version=2.3.0beta");
            connection = ((HttpURLConnection) url.openConnection());
            Log.e("DisplayContent","openConnection");
            //connection.setConnectTimeout(7000);
            //connection.setReadTimeout(7000);
            //connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            Log.e("DisplayContent","settings for connection");

            int response = connection.getResponseCode();
            Log.e("DisplayContent", String.valueOf(response));

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            Log.e("DisplayContent","connect");
            //StringBuilder responseStrBuilder = new StringBuilder();
            //JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String inputStr;
            /*while ((inputStr = in.readLine()) != null){
                Log.e("DisplayContent", inputStr);
                responseStrBuilder.append(inputStr);
            }*/
            inputStr = in.readLine();
            String str;
            str = inputStr.replace("\\","");
            str = str.substring(2);
            Log.e("DisplayContent",str);
            //str.replaceAll("},","}");
            //Log.e("DisplayContent",str);
            String [] strArr = str.split(",\\{\"t");
            Log.e("DisplayContent",strArr[0]);
            str = strArr[0];
            if(str.endsWith(",")){
                str =str.substring(0,str.length()-2);
            }
            Log.e("DisplayContent",str);
            JSONObject jsonObject = new JSONObject(str);
            Log.e("DisplayContent","jsonobj");
            arr = jsonObject.getJSONArray("data");
            Log.e("DisplayContent","arr");
            Log.e("DisplayContent", arr.get(0).toString());

            /*String[] array = str.split("u");
            String text = "";
            for(int i = 1; i < array.length; i++){
                int hexVal = Integer.parseInt(array[i], 16);
                text += (char)hexVal;
            }
            Log.e("DisplayContent",text);*/

        }catch (MalformedURLException var11) {
            Log.e("DisplayContent", "Could not upload telemetry due to malformed URL", var11);
            var11.printStackTrace();
        } catch (IOException var12) {
            Log.w("DisplayContent", "IOException while uploading ping", var12);
            var12.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if(connection != null) {
                connection.disconnect();
            }
            return arr;
        }
        //return obj;

    }

    private  class AsyncTaskUpdate extends android.os.AsyncTask<String,String,JSONArray>{
        JSONArray jsonArray;
        public AsyncTaskUpdate(JSONArray obj){this.jsonArray = obj;}
        @Override
        protected JSONArray doInBackground(String... params){
            return sendRequest();
        }
        @Override
        protected void onPostExecute(JSONArray obj){
            contentListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    // 在newState为滑到底部时
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateRecyclerView(obj);

                    }
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // 在滑动完成后，拿到最后一个可见的item的位置
                    lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                }
            });

            jsonArray = obj;
        }
    }
    private class AsyncTaskSendRequest extends android.os.AsyncTask<String,String,JSONArray> {
        JSONArray jsonArray;
        public AsyncTaskSendRequest(JSONArray obj){this.jsonArray = obj;}
        @Override
        protected JSONArray doInBackground(String... params){
            return sendRequest();
        }
        @Override
        protected void onPostExecute(JSONArray obj){
            Log.e("DisplayContent","onPost");
            try {
                Log.e("DisplayContent", obj.get(0).toString());
            }catch (Exception e){
                e.printStackTrace();
            }
            mAdapter = new ContentAdapter(getData(obj), context);
            contentListView.setAdapter(mAdapter);
            if(mAdapter == null){
                Log.e("DisplayContent","mAdapter is null");
            }

            mAdapter.setOnItemClickListener(new ContentAdapter.OnItemClickListener(){
                @Override
                public void onItemClick(View view , int position){
                    if (position != RecyclerView.NO_POSITION && position < mAdapter.getItemCount()) {
                        Content item = mAdapter.mItems.get(position);
                        Log.e("DisplayContent",item.getUrl());
                        //todo open new tab to load url
                        //ScreenNavigator.get(context).showBrowserScreen(item.getUrl(),true,false);
                    }
                }
            });


            jsonArray = obj;
        }
    }

    private void updateRecyclerView(JSONArray obj) {
        // 获取从fromIndex到toIndex的数据
        List<Content> newData = getData(obj);
        if (newData.size() > 0) {
            // 然后传给Adapter，并设置hasMore为true
            mAdapter.updateList(newData);
        } else {
            mAdapter.updateList(null);
        }
    }



}

