package org.mozilla.focus.telemetry;

/**
 * Created by mozillabeijing on 2018/4/9.
 */

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.net.TelemetryClient;
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder;
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder;
import org.mozilla.telemetry.util.IOUtils;

public class HttpURLConnectionTelemetryClientCN implements TelemetryClient {
    private static final String LOG_TAG = "HttpURLTelemetryClientCN";

    public HttpURLConnectionTelemetryClientCN() {
    }

    public boolean uploadPing(TelemetryConfiguration configuration, String path, String serializedPing) {
        HttpURLConnection connection = null;

        boolean var6;
        try {
            connection = this.openConnectionConnection(configuration.getServerEndpoint(), path);
            connection.setConnectTimeout(configuration.getConnectTimeout());
            connection.setReadTimeout(configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("User-Agent", configuration.getUserAgent());
            connection.setRequestProperty("Date", this.createDateHeaderValue());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            int responseCode = this.upload(connection, serializedPing);
            Log.d("HttpURLTelemetryClient", "Ping upload: " + responseCode);
            if(responseCode >= 200 && responseCode <= 299) {
                var6 = true;
                return var6;
            }

            if(responseCode >= 400 && responseCode <= 499) {
                Log.e("HttpURLTelemetryClient", "Server returned client error code: " + responseCode);
                var6 = true;
                return var6;
            }

            Log.w("HttpURLTelemetryClient", "Server returned response code: " + responseCode);
            var6 = false;
        } catch (MalformedURLException var11) {
            Log.e("HttpURLTelemetryClient", "Could not upload telemetry due to malformed URL", var11);
            var6 = true;
            return var6;
        } catch (IOException var12) {
            Log.w("HttpURLTelemetryClient", "IOException while uploading ping", var12);
            var6 = false;
            return var6;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }

        }

        return var6;
    }

    public boolean uploadPing(TelemetryConfiguration configuration, String path, String serializedPing,String pingType) {
        HttpURLConnection connection = null;
        HttpURLConnection connectionCN = null;

        boolean var6;
        try {
             if(pingType == TelemetryChinaPingBuilder.TYPE){
                String str = serializedPing.replaceAll("\"","");
                String str2 = str.substring(0,str.length()-3);
                String info[] = str2.split(",");
                String pathCN[] = path.split("/");
                String clientID = info[1].substring(9);
                String device = URLEncoder.encode(info[2].substring(7),"UTF-8");
                if(info[7].equals("top_site")) {
                    String url = info[8].substring(4,info[8].length()-1).replace("\\","");
                    String urlStr = URLEncoder.encode(url, "UTF-8");
                    Log.e("HttpCnTracking",url);
                    Log.e("HttpCnTracking",urlStr);
                    connectionCN = this.openConnectionConnection("https://m.g-fox.cn/cnrocket.gif?" + "clientID=" + clientID + "&device=" + device + "&type=" + info[7] + "&url=" + urlStr + "&documentID=", pathCN[3] + "&version=" + pathCN[6] + pathCN[7]);
                }else{
                    connectionCN = this.openConnectionConnection("https://m.g-fox.cn/cnrocket.gif?" + "clientID=" + clientID + "&device=" + device + "&type=" + info[7] + "&documentID=", pathCN[3] + "&version=" + pathCN[6] + pathCN[7]);
                }
                connectionCN.setConnectTimeout(configuration.getConnectTimeout());
                connectionCN.setReadTimeout(configuration.getReadTimeout());
                connectionCN.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connectionCN.setRequestProperty("User-Agent", configuration.getUserAgent());
                connectionCN.setRequestProperty("Date", this.createDateHeaderValue());
                connectionCN.setRequestMethod("GET");
                Log.e("HttpURLTelemetryClient","ChinaPing");
                int responseCodeCN;
                if(info[7].equals("top_site")){
                    responseCodeCN = this.uploadUsingGet(connectionCN,info[1]+"&"+info[7]+"&"+info[8]);
                }else{
                    responseCodeCN = this.uploadUsingGet(connectionCN, info[1]+"&"+info[7]);
                }

                Log.d("HttpURLTelemetryClient", "Ping upload: " + responseCodeCN);
                if (responseCodeCN >= 200 && responseCodeCN <= 299) {
                    var6 = true;
                    Log.e("HttpURLTelemetryClient", " CN Server returned client error code: " + responseCodeCN);
                    return var6;
                }

                if (responseCodeCN >= 400 && responseCodeCN <= 499) {
                    Log.e("HttpURLTelemetryClient", "CN Server returned client error code: " + responseCodeCN);
                    var6 = true;
                    return var6;
                }

                Log.w("HttpURLTelemetryClient", "CN Server returned response code: " + responseCodeCN);
                var6 = false;
            }else{
                 connection = this.openConnectionConnection(configuration.getServerEndpoint(), path);
                 connection.setConnectTimeout(configuration.getConnectTimeout());
                 connection.setReadTimeout(configuration.getReadTimeout());
                 connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                 connection.setRequestProperty("User-Agent", configuration.getUserAgent());
                 connection.setRequestProperty("Date", this.createDateHeaderValue());
                 connection.setRequestMethod("POST");
                 connection.setDoOutput(true);
                Log.e("HttpURLTelemetryClient","CorePing or EventPing");
                int responseCode = this.upload(connection, serializedPing);

                Log.d("HttpURLTelemetryClient", "Ping upload: " + responseCode);
                if (responseCode >= 200 && responseCode <= 299) {
                    var6 = true;
                    Log.e("HttpURLTelemetryClient", "tw Server returned client error code: " + responseCode);
                    return var6;
                }

                if (responseCode >= 400 && responseCode <= 499) {
                    Log.e("HttpURLTelemetryClient", "tw Server returned client error code: " + responseCode);
                    var6 = true;
                    return var6;
                }

                Log.w("HttpURLTelemetryClient", "tw Server returned response code: " + responseCode);
                var6 = false;
                return var6;
            }
        } catch (MalformedURLException var11) {
            Log.e("HttpURLTelemetryClient", "Could not upload telemetry due to malformed URL", var11);
            var6 = true;
            return var6;
        } catch (IOException var12) {
            Log.w("HttpURLTelemetryClient", "IOException while uploading ping", var12);
            var6 = false;
            return var6;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
            if(connectionCN != null){
                connectionCN.disconnect();
                Log.e("HttpCNTracking","connectionCN Disconnect");
            }

        }

        return var6;
    }
    @VisibleForTesting
    int upload(HttpURLConnection connection, String serializedPing) throws IOException {
        OutputStream stream = null;

        int var5;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream = connection.getOutputStream()));
            writer.write(serializedPing);
            writer.flush();
            writer.close();
            var5 = connection.getResponseCode();
        } catch (ArrayIndexOutOfBoundsException var9) {
            throw new IOException(var9);
        } finally {
            IOUtils.safeClose(stream);
        }

        return var5;
    }

    int uploadUsingGet(HttpURLConnection connection, String serializedPing) throws IOException {
        BufferedReader in = null;
        int var5 = 0;
        try {
            System.out.println(serializedPing);
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            line = in.readLine();
            var5 = connection.getResponseCode();
            System.out.println(line);
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return var5;
    }


    @VisibleForTesting
    HttpURLConnection openConnectionConnection(String endpoint, String path) throws IOException {
        URL url = new URL(endpoint + path);
        Log.e("HttpCnTracking",url.toString());
        return (HttpURLConnection)url.openConnection();
    }

    @VisibleForTesting
    String createDateHeaderValue() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}

