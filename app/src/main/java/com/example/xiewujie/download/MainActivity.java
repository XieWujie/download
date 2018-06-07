package com.example.xiewujie.download;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.xiewujie.download.download.DownloadService;
import com.example.xiewujie.download.download.DownloadUtil;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText editText;
    private Button commitButton;
    private Button stopButton;
    private Button continueButton;
    private Button cancelButtion;
    private Button testButton;
    private DownloadUtil downloadUtil;
    private List<String> mList = new ArrayList<>();
    private String[] apkUrl = new String[]{
            "http://down11.zol.com.cn/Game/A/BLOOD_GLORY_v1.0.0.apk",
            "http://down11.zol.com.cn/suyan/yingyongbao7.0.1.apk",
            "http://down11.zol.com.cn/suyan/qqpinyin5.19g.apk",
            "http://down11.zol.com.cn/photo/PCamera3.4.1y.apk",
            "http://down11.zol.com.cn/Game/CriticalStrikePortable3.587.apk"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       commitButton = (Button)findViewById(R.id.commit);
       editText = (EditText)findViewById(R.id.url_edit);
       stopButton = (Button)findViewById(R.id.stop_download);
       continueButton = (Button)findViewById(R.id.continue_download);
       cancelButtion = (Button)findViewById(R.id.cancel_download);
       testButton = (Button)findViewById(R.id.download_test);
       testButton.setOnClickListener(this);
       stopButton.setOnClickListener(this);
       continueButton.setOnClickListener(this);
       cancelButtion.setOnClickListener(this);
       getPermission();
       commitButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               startDownload();
           }
       });
    }
    private void startDownload(){
        String editUrl = editText.getText().toString();
        if (downloadUtil==null)
        downloadUtil = new DownloadUtil(this);
        downloadUtil.startDownload(editUrl);
        editText.setText("");
    }

    private void testDownload(){

    }

    private void getPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 0:
                if (!(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)){
                    Toast.makeText(this,"拒绝权限程序将不能运行",Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.stop_download:
                if (downloadUtil!=null){
                    downloadUtil.stopDownload();
                }
                break;
            case R.id.continue_download:
                if (downloadUtil!=null){
                    downloadUtil.continueDownload();
                }
                break;
            case R.id.cancel_download:
                downloadUtil.cancelDownload();
                break;
            case R.id.download_test:
                for (int i = 0;i<apkUrl.length;i++){
                    mList.add(apkUrl[i]);
                }
                if (downloadUtil==null){
                    downloadUtil = new DownloadUtil(mList,this);
                    downloadUtil.startService();
                }
        }
    }
}
