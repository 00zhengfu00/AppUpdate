package com.vector.appupdatedemo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.UpdateCallback;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import rx.functions.Action1;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OkHttpUtils.getInstance()
                .init(this)
                .debug(true, "vetor")
                .timeout(20 * 1000);

    }

    public void updateApp(View view) {
        String url = "http://47.94.102.201/mobileCard/com/mobile/updateVersion.html";
        String appKey = "ab55ce55Ac4bcP408cPb8c1Aaeac179c5f6f";

        final String targetPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        final UpdateAppHttpUtil httpManager = new UpdateAppHttpUtil();

        final UpdateAppManager updateAppManager = UpdateAppManager.getInstance();

        updateAppManager.updateApp(this, httpManager, url, appKey, new UpdateCallback() {
            @Override
            public void isHasNewApp(boolean hasNewApp, UpdateAppBean updateApp) {
                Log.d(TAG, "isHasNewApp() called with: hasNewApp = [" + hasNewApp + "], updateApp = [" + updateApp + "]");
                if (hasNewApp) {
                    updateAppManager.showUpdatedDialog(httpManager, MainActivity.this, targetPath, updateApp);
                } else {
                    Toast.makeText(MainActivity.this, "没有新版本", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAfter() {
                Log.d(TAG, "onAfter() called");
                CProgressDialogUtils.cancelProgressDialog(MainActivity.this);
            }

            @Override
            public void onBefore() {
                Log.d(TAG, "onBefore() called");
                CProgressDialogUtils.showProgressDialog(MainActivity.this);
            }

        });
    }

    public void getPermission(View view) {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(WRITE_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            Toast.makeText(MainActivity.this, "已授权", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "未授权", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    public void downloadApp(View view) {
        String url = "http://47.94.102.201/online/appDownFile/yimiao-0.3.0-2017-06-19-release.apk";
        String fileName = "test.app";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        OkHttpUtils.get()
                .url(url)
                .build()
                .execute(new FileCallBack(path, fileName) {
                    @Override
                    public void inProgress(float progress, long total, int id) {
                        super.inProgress(progress, total, id);
                        Log.d(TAG, "inProgress() called with: progress = [" + progress + "], total = [" + total + "], id = [" + id + "]");
                    }

                    @Override
                    public void onError(Call call, Response response, Exception e, int id) {
                        Log.d(TAG, validateError(e, response));
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        Log.d(TAG, response.getAbsolutePath());

                    }

                    @Override
                    public void onBefore(Request request, int id) {
                        super.onBefore(request, id);
                        Log.d(TAG, "onBefore() called with: request = [" + request + "], id = [" + id + "]");
                    }
                });

    }
}
