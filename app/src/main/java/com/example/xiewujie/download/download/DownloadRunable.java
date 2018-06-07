package com.example.xiewujie.download.download;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadRunable implements Runnable {
    private String address;
    private long start;
    private long end;
    private RandomAccessFile file;
    private DownloadListener listener;
    private boolean isPause = false;
    private int totalLength;
    private boolean isCancel = false;

    public DownloadRunable(String address, long start, long end, int totalLength, DownloadListener listener) {
       this.address = address;
        this.start = start;
        this.end = end;
        this.listener = listener;
        this.totalLength = totalLength;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        File f = getFile(address);
        if (f==null)return;
        try {
            URL url = new URL(address);
            file = new RandomAccessFile(f,"rw");
            if (file==null){
                listener.onFail(0,address);
                return;
            }
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            int haveDownload = 0;
            file.seek(start);
            byte[] b = new byte[1024];
            int len = 0;
            while ((len = file.read(b))!=-1){
                haveDownload+=len;
            }
            if (haveDownload>=end-start){
                listener.onSucceed(address);
                return;
            }
            connection.setRequestProperty("Range","bytes="+(start+haveDownload)+"-"+end);
            inputStream = connection.getInputStream();
            long downloadLength = haveDownload;
            int progress = 0;
            if (inputStream!=null){
                file.seek(downloadLength+start);
                int oldprogress = 0;
                b = new byte[1024];
                while ((len = inputStream.read(b))!=-1){
                    if (isPause) {
                        listener.onProgress(progress,address);
                        break;
                    }else if (isCancel){
                        f.delete();
                        break;
                    }else {
                        downloadLength += len;
                        file.write(b, 0, len);
                        progress = (int) (downloadLength * 100 / (totalLength));
                        if (progress - oldprogress >= 1) {
                            listener.onProgress(progress,address);
                        }
                        oldprogress = progress;
                    }
                    }
                }
                file.close();
                if (downloadLength>=end-start){
                    listener.onSucceed(address);
                }else {
                    listener.onFail(progress,address);
                }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (connection!=null){
                connection.disconnect();
            }
            if (inputStream!=null){
                try {
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private RandomAccessFile getRandomAccessFile(String address){
        File file = getFile(address);
        try {
            return new RandomAccessFile(file, "rw");
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    private File getFile(String address){
        String fileName = address.substring(address.lastIndexOf("/"));
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        File file = new File(directory+fileName);
        return file;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public DownloadListener getListener() {
        return listener;
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    public boolean isPause() {
        return isPause;
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    public boolean isCancel() {
        return isCancel;
    }

    public void setCancel(boolean cancel) {
        isCancel = cancel;
    }
}
