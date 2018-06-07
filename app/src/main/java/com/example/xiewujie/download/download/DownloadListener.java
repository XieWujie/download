package com.example.xiewujie.download.download;

public interface DownloadListener {
    void onFail(int progress,String url);
    void onProgress(int progress,String url);
    void onSucceed(String url);
}
