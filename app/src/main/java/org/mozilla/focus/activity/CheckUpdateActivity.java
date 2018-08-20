package org.mozilla.focus.activity;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.mozilla.rocket.tabs.web.Download;
import org.mozilla.rocket.tabs.web.DownloadCallback;
import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.ProxySelector;
import org.mozilla.focus.utils.AppConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by mozillabeijing on 2018/5/11.
 */

public class CheckUpdateActivity extends AppCompatActivity{

    private Button checkUpdateButton;
    private Button cancelButton;
    private String updateUrl;
    private String cnSendStr;
    private DownloadManager dm;
    private long downloadId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkupdate);
        initViews();
    }

    private void initViews(){
        checkUpdateButton = (Button)findViewById(R.id.check_update);
        cancelButton = (Button)findViewById(R.id.cancel_update);
        checkUpdateButton.setTag(1);
        /*"https://aus2.mozilla.com.cn/update/4/" + AppConstants.MOZ_APP_BASENAME + "/" +
                AppConstants.MOZ_APP_VERSION         +
                "/%LOCALE%/"                         + AppConstants.MOZ_UPDATE_CHANNEL +
                "/%OS_VERSION%/%CHANNELID%/default/" +
                "/update.xml";*/
        cnSendStr ="https://aus3.mozilla.com.cn/update/4/" + "Lite" + "/" +
                BuildConfig.VERSION_NAME +
                "/" + "default/" + Build.VERSION.RELEASE +"/base/default/" +
                "update.xml";
        dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public void onCheckUpdateClicked(View v){
        if((Integer)checkUpdateButton.getTag() == 1){
            boolean isAvailable = checkUpdate();

            if(isAvailable){
            //if(true){ // for test
                checkUpdateButton.setText("有可用更新，安装更新");
                checkUpdateButton.setTag(2);
            }else{
                checkUpdateButton.setText("无可用更新");
                checkUpdateButton.setTag(3);
            }
        }else if((Integer)checkUpdateButton.getTag() == 2){
            String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
            String fileName = "Firefox-Lite-update.apk";
            destination += fileName;
            final Uri uri = Uri.parse("file://" + destination);
            Log.e("cnupdate","file://"+destination);

            //Delete update file if exists
            final File file = new File(destination);
            if (file.exists()) {
                file.delete();
            }
            //for test
            //updateUrl = "https://github.com/mozilla-tw/Rocket/releases/download/2.0/Firefox-Rocket-3584.apk";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateUrl));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDescription("下载更新中...");
            request.setTitle("Firefox Lite");
            //set destination
            request.setDestinationUri(uri);

            checkUpdateButton.setText("下载更新中");
            checkUpdateButton.setTag(4);
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.e("cnupdate", "Permission is granted");

                    downloadId = dm.enqueue(request);
                } else {
                    Log.e("cnupdate", "Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    checkUpdateButton.setText("请选择允许后重试");
                    checkUpdateButton.setTag(2);
                    downloadId = 0;
                }

            } else { //permission is automatically granted on sdk<23 upon installation
                downloadId = dm.enqueue(request);
            }


            //set BroadcastReceiver to install app when .apk is downloaded
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        Log.e("cnupdate",">= 24");
                        Uri apkUri = FileProvider.getUriForFile(ctxt, BuildConfig.APPLICATION_ID + ".provider.fileprovider", file);
                        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        install.setData(apkUri);
                        install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(install);
                    }else {
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        install.setDataAndType(uri,
                                "application/vnd.android.package-archive");
                        startActivity(install);
                    }

                    unregisterReceiver(this);
                    finish();
                }
            };
            //register receiver for when .apk download is compete
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        }

    }

    public void onCancelUpdateClicked(View v){
        dm.remove(downloadId);
        this.finish();
    }

    public boolean checkUpdate(){
        updateUrl = "";


        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    URLConnection connection = null;
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    URI cnSendUri = new URI(cnSendStr);
                    //connection = (HttpURLConnection)cnSendUrl.openConnection();
                    connection = ProxySelector.openConnectionWithProxy(cnSendUri);
                    Log.e("cnupdate",cnSendUri.toString());
                    Document dom = builder.parse(connection.getInputStream());

                    NodeList nodes = dom.getElementsByTagName("update");
                    if (nodes == null || nodes.getLength() == 0) {
                        return;
                    }

                    nodes = dom.getElementsByTagName("patch");
                    if (nodes == null || nodes.getLength() == 0) {
                        return;
                    }

                    Node patchNode = nodes.item(0);
                    Node urlNode = patchNode.getAttributes().getNamedItem("URL");
                    if(urlNode.getTextContent()!=null){
                        updateUrl = urlNode.getTextContent().toString();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try{
            thread.join();
        }catch(Exception e){
            e.printStackTrace();
        }

        Log.e("cnupdate",updateUrl);

        if (updateUrl.equals("")){
            return false;
        }else{
            return true;
        }
    }

}
