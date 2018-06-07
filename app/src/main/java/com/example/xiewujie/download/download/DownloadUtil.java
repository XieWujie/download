package com.example.xiewujie.download.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class DownloadUtil {
    private List<String> mList;
    private Context context;
    private int numOfThread = 1;
    public DownloadUtil(List<String> mList, Context context) {
        this.mList = mList;
        this.context = context;
    }

    public DownloadUtil(Context context){
        this.context = context;
    }

    private DownloadService.DownloadBinder binder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (DownloadService.DownloadBinder)service;
            if (binder!=null){
                if (mList==null){
                    mList = new ArrayList<>();
                }
                binder.startDownload(mList, numOfThread);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public void startService(){
        Intent intent = new Intent(context,DownloadService.class);
        context.startService(intent);
        context.bindService(intent,connection,Context.BIND_AUTO_CREATE);
    }

    public void stopDownload(){
        binder.stopAllDownload();
    }

    public void continueDownload(){
        if (binder==null)return;
        binder.continueAllDownload();
        binder.startDownload(mList, numOfThread);
    }

    public boolean cancelDownload(){
        if (binder!=null) {
            binder.cancelAllDownload();
            context.unbindService(connection);
            binder = null;
        }
        return true;
    }

    public void startDownload(String address){
        if (mList==null){
            mList = new ArrayList<>();
            mList.add(address);
            startService();
        }else{
            if (binder!=null) {
                binder.startDownload(address, numOfThread);
            }
        }
    }

    public void startDownload(String[] addresses){
        if (mList==null){
            mList = new ArrayList<>();
            for (int i = 0;i<addresses.length;i++){
                mList.add(addresses[i]);
                startService();
            }
        }else {
            if (binder!=null){
                for (String address:addresses){
                    startDownload(address);
                }
            }
        }
    }

    public void setNumOfThread(int number){
        this.numOfThread = number;
    }
}
