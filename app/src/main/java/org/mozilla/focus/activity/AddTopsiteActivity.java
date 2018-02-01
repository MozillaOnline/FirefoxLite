package org.mozilla.focus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONArray;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.HotListAdapter.OnItemClickListener;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.HotListUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozillabeijing on 2017/12/7.
 */

public class AddTopsiteActivity extends AppCompatActivity{

    private EditText topsiteName;
    private EditText topsiteUrl;
    private View addTopsite;
    private View cancelTopsite;
    private View hotList;
    private RecyclerView topsiteListView;
    private long siteId = -11;
    private String favIcon = "";
    private String name;
    private String url;
    private HotListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtopsite);
        //initData();
        initViews();
    }

    private void initViews(){
        topsiteUrl = (EditText)findViewById(R.id.topsite_url);
        topsiteName = (EditText) findViewById(R.id.topsite_name);
        addTopsite = findViewById(R.id.add_topsite);
        cancelTopsite = findViewById(R.id.cancel_topsite);
        hotList = findViewById(R.id.title_hot_list);
        topsiteListView = (RecyclerView) findViewById(R.id.topsite_recyclerview);
        topsiteListView.setVisibility(View.VISIBLE);
        //historyFragment.getView().setVisibility(View.GONE);
        initData();
        topsiteListView.setLayoutManager(mLayoutManager);
        topsiteListView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(View view , int position){
                if (position != RecyclerView.NO_POSITION && position < mAdapter.getItemCount()) {
                    Site item = mAdapter.mItems.get(position);
                    topsiteName.setText(item.getTitle());
                    topsiteUrl.setText(item.getUrl());
                    siteId = item.getId();
                    //change the icons later
                    switch (String.valueOf(siteId)){
                        case "-11":
                            favIcon = "zhihu.png";
                            break;
                        case "-13":
                            favIcon = "58.png";
                            break;
                        case "-14":
                            favIcon = "vqq.png";
                            break;
                        case "-15":
                            favIcon = "dianping.png";
                            break;
                        case "-16":
                            favIcon = "zol.png";
                            break;
                        case "-17":
                            favIcon = "qidian.png";
                            break;
                        case "-18":
                            favIcon = "bilibili.png";
                            break;
                        case "-19":
                            favIcon = "4399.png";
                            break;
                        case "-31":
                            favIcon = "ximalaya.png";
                            break;
                        case "-39":
                            favIcon = "amazon.png";
                            break;
                        default:
                            break;
                    }
                }
            }
        });

    }

    private void initData(){
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mAdapter = new HotListAdapter(getData(), this);
    }

    public void onAddTopsiteClicked(View v){
        if (topsiteName.getText().toString().isEmpty() || topsiteUrl.getText().toString().isEmpty()){
            new  AlertDialog.Builder(this).setTitle("No Name or URL" ).setMessage("Please enter the Name and URL" ).setPositiveButton("OK" , null ).show();

        }else if(topsiteName.getText().toString().contains(" ")){
            new  AlertDialog.Builder(this).setTitle("No spaces are allowed in the name" ).setMessage("Please enter another name" ).setPositiveButton("OK" , null ).show();

        } else{
            name = topsiteName.getText().toString();
            url = topsiteUrl.getText().toString();
            String site = name+" "+url+" "+favIcon+" "+siteId;
            Intent intent = new Intent();
            intent.putExtra("result", site);
            setResult(1001, intent);
            this.finish();

        }
    }

    public void onCancelTopsiteClicked(View v){
        this.finish();

    }


    private List<Site> getData() {
        List<Site> data;
        JSONArray obj = HotListUtils.getHotSitesJsonArrayFromAssets(this);
        data = HotListUtils.paresJsonToList(this,obj);
        return data;
    }


}


