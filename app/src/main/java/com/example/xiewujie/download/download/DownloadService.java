package com.example.xiewujie.download.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.xiewujie.download.MainActivity;
import com.example.xiewujie.download.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadService extends Service {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 3;
    private int contentLength = 0;
    private static DownloadService downloadService;
    private Bitmap largeBitmap;
    private HashMap<String, DownloadRunable> runnableMap = new HashMap<>();
    private ThreadPoolExecutor executor;
    private String oldUrl;
    private int notificationCode = 0;
    private HashMap<String,Integer> urlMap = new HashMap<>();
    DownloadBinder binder;
    private HashMap<String,Boolean> clickState = new HashMap<>();
    public DownloadService() {
    }

    @Override
    public void onCreate() {
        downloadService = this;
        super.onCreate();
    }

    private static DownloadService getInstance(){
        return downloadService;
    }

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress, String url) {
            if (!urlMap.containsKey(url)){
                urlMap.put(url,notificationCode);
                notificationCode++;
            }
            int code = urlMap.get(url);
            getNotificationManager().notify(code, getNotification(url, progress));
            Log.d("DownloadService", url + ":   " + progress);
            if (progress == 100) {
                getNotificationManager().notify(code, getNotification("下载完成" + url, 100));
                urlMap.remove(url);
                }
        }

        @Override
        public void onSucceed(String url) {
          Toast.makeText(DownloadService.this,"文件下载成功",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFail(int progress,String url) {
            if (!urlMap.containsKey(url)){
                urlMap.put(url,notificationCode);
                notificationCode++;
            }
            int code = urlMap.get(url);
            getNotificationManager().notify(code, getNotification(url, progress));
            Log.d("DownloadService", url + ":   " + progress);
            if (progress == 0) {
                getNotificationManager().notify(code, getNotification(url, 100));
                urlMap.remove(url);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        binder = new DownloadBinder();
       return binder;
    }

    class DownloadBinder extends Binder {
        public void startDownload(List<String> list, int numOfThread) {
            largeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.downloads);
            executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
            for (int i = 0; i < list.size(); i++) {
                String address = list.get(i);
                getContentLengt(address);
                while (true) {
                    if (contentLength != 0) {
                        int one = contentLength / 3;
                        for (int k = 0; k < numOfThread; k++) {
                            if (i < numOfThread - 1) {
                                DownloadRunable downloadRunable = new DownloadRunable(address, one * k, one * (k + 1) - 1, contentLength, listener);
                                runnableMap.put(address, downloadRunable);
                                executor.execute(downloadRunable);
                            } else {
                                DownloadRunable downloadRunable = new DownloadRunable(address, one * k, contentLength, contentLength, listener);
                                runnableMap.put(address, downloadRunable);
                                executor.execute(downloadRunable);
                            }
                        }
                        break;
                    }
                }
                contentLength = 0;
            }
        }

        public void startDownload(String address,int numOfThread){
            getContentLengt(address);
            while (true) {
                if (contentLength != 0) {
                    int one = contentLength / 3;
                    for (int k = 0; k < numOfThread; k++) {
                        if (k < numOfThread - 1) {
                            DownloadRunable downloadRunable = new DownloadRunable(address, one * k, one * (k + 1) - 1, contentLength, listener);
                            runnableMap.put(address, downloadRunable);
                            executor.execute(downloadRunable);
                        } else {
                            DownloadRunable downloadRunable = new DownloadRunable(address, one * k, contentLength, contentLength, listener);
                            runnableMap.put(address, downloadRunable);
                            executor.execute(downloadRunable);
                        }
                    }
                    break;
                }
            }
            contentLength = 0;
        }

        public void stopDownload(String address) {
            if (runnableMap == null) return;
            DownloadRunable downloadRunable = runnableMap.get(address);
            if (downloadRunable != null) {
                downloadRunable.setPause(true);
                executor.remove(downloadRunable);

            }
        }

            public void stopAllDownload () {
                if (runnableMap == null) return;
                DownloadRunable downloadRunable = null;
                Iterator iterator = runnableMap.entrySet().iterator();
                Map.Entry entry = null;
                while (iterator.hasNext()) {
                    entry = (Map.Entry) iterator.next();
                    downloadRunable = (DownloadRunable) entry.getValue();
                    downloadRunable.setPause(true);
                    executor.remove(downloadRunable);
                }
                executor.shutdownNow();
            }

            public void continueAllDownload () {
                if (runnableMap == null) return;
                DownloadRunable downloadRunable = null;
                Iterator iterator = runnableMap.entrySet().iterator();
                Map.Entry entry = null;
                while (iterator.hasNext()) {
                    entry = (Map.Entry) iterator.next();
                    downloadRunable = (DownloadRunable) entry.getValue();
                    downloadRunable.setPause(false);
                    downloadRunable.setCancel(false);
                }
            }

            public void continueDownload (String address){
                DownloadRunable downloadRunable = runnableMap.get(address);
                if (downloadRunable != null) {
                    downloadRunable.setPause(false);
                    downloadRunable.setCancel(false);
                    executor.execute(downloadRunable);
                }
            }

            public void cancelAllDownload () {
                DownloadRunable downloadRunable = null;
                Iterator iterator = runnableMap.entrySet().iterator();
                Map.Entry entry = null;
                while (iterator.hasNext()) {
                    entry = (Map.Entry) iterator.next();
                    downloadRunable = (DownloadRunable) entry.getValue();
                    downloadRunable.setCancel(true);
                }
                executor.shutdownNow();
            }

            public void cancelDownload (String address){
                DownloadRunable downloadRunable = runnableMap.get(address);
                if (downloadRunable != null) {
                    downloadRunable.setCancel(true);
                    executor.remove(downloadRunable);
                }
            }
        }

        private void getContentLengt(final String address) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(address);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        connection.connect();
                        contentLength = connection.getContentLength();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private NotificationManager getNotificationManager() {
            return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        private Notification getNotification(String address, int progress) {
            Notification.Builder builder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setLargeIcon(largeBitmap)
                            .setContentTitle(address);
            if (progress > 0) {
                builder.setContentText(progress + "%")
                        .setProgress(100, progress, false);
            }
            RemoteViews remoteView = new RemoteViews(getPackageName(),R.layout.notification_layout);
            remoteView.setImageViewResource(R.id.notification_img,R.drawable.downloads);
            remoteView.setProgressBar(R.id.notification_progress,100,progress,false);
            remoteView.setTextViewText(R.id.progress_text,progress+"%");
            if (!clickState.containsKey(address)){
                clickState.put(address,true);
            }
            if (clickState.get(address)) {
                remoteView.setImageViewResource(R.id.notification_click, R.drawable.pause);
            }else{
                remoteView.setImageViewResource(R.id.notification_click,R.drawable.start);
            }
            Intent intent = new Intent("com.example.xiewujie.download.MYBROADCAST");
            intent.putExtra("address",address);
            intent.putExtra("clickState",clickState.get(address));
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.notification_click,pi);
            Notification notification = builder.build();
            notification.contentView = remoteView;
            return notification;
        }

       public static class MyBroadCast extends BroadcastReceiver{
            @Override
            public void onReceive(Context context, Intent intent) {
                DownloadService service = DownloadService.getInstance();
                String address = intent.getStringExtra("address");
                 boolean state = intent.getBooleanExtra("clickState",true);
                if (state){
                service.binder.stopDownload(address);
                Toast.makeText(context,"cancelDownload"+address,Toast.LENGTH_LONG).show();
                service.clickState.put(address,false);
                }else {
                    service.binder.continueDownload(address);
                    Toast.makeText(context,"continueDownload"+address,Toast.LENGTH_LONG).show();
                    service.clickState.put(address,true);
                }
            }
        }
    }
