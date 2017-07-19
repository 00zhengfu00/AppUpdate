package com.vector.update_app;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vector.update_app.service.DownloadService;
import com.vector.update_app.utils.ColorUtil;
import com.vector.update_app.utils.DrawableUtil;
import com.vector.update_app.utils.Md5Util;
import com.vector.update_app.utils.Utils;
import com.vector.update_app.view.NumberProgressBar;

import java.io.File;

/**
 * Created by Vector
 * on 2017/7/19 0019.
 */

public class UpdateDialogFragment extends DialogFragment implements View.OnClickListener {
    public static boolean isShow = false;
    private TextView mContentTextView;
    private Button mUpdateOkButton;
    private UpdateAppBean mUpdateApp;
    private NumberProgressBar mNumberProgressBar;
    private ImageView mIvClose;
    private TextView mTitleTextView;
    /**
     * 回调
     */
    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            startDownloadApp((DownloadService.DownloadBinder) service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private LinearLayout mLlClose;
    //默认色
    private int mDefaultColor = 0xffe94339;
    private int mDefaultPicResId = R.mipmap.lib_update_app_top_bg;
    private ImageView mTopIv;
    private TextView mIgnore;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isShow = true;
//        setStyle(DialogFragment.STYLE_NO_TITLE | DialogFragment.STYLE_NO_FRAME, 0);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.UpdateAppDialog);


    }

    @Override
    public void onStart() {
        super.onStart();
        //点击window外的区域 是否消失
        getDialog().setCanceledOnTouchOutside(false);
        //是否可以取消,会影响上面那条属性
//        setCancelable(false);
        //window外可以点击,不拦截窗口外的事件
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    //禁用
                    if (mUpdateApp != null && mUpdateApp.isConstraint()) {
                        //返回桌面
                        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lib_update_app_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    private void initView(View view) {
        //提示内容
        mContentTextView = (TextView) view.findViewById(R.id.tv_update_info);
        //标题
        mTitleTextView = (TextView) view.findViewById(R.id.tv_title);
        //更新按钮
        mUpdateOkButton = (Button) view.findViewById(R.id.btn_ok);
        //进度条
        mNumberProgressBar = (NumberProgressBar) view.findViewById(R.id.npb);
        //关闭按钮
        mIvClose = (ImageView) view.findViewById(R.id.iv_close);
        //关闭按钮+线 的整个布局
        mLlClose = (LinearLayout) view.findViewById(R.id.ll_close);
        //顶部图片
        mTopIv = (ImageView) view.findViewById(R.id.iv_top);
        //忽略
        mIgnore = (TextView) view.findViewById(R.id.tv_ignore);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
    }

    private void initData() {
        mUpdateApp = (UpdateAppBean) getArguments().getSerializable(UpdateAppManager.INTENT_KEY);
        //设置主题色
        initTheme();


        if (mUpdateApp != null) {
            //弹出对话框
            String newVersion = mUpdateApp.getNewVersion();
            String targetSize = mUpdateApp.getTargetSize();
            String updateLog = mUpdateApp.getUpdateLog();

            String msg = "";

            if (!TextUtils.isEmpty(targetSize)) {
                msg = "新版本大小：" + targetSize + "\n\n";
            }

            if (!TextUtils.isEmpty(updateLog)) {
                msg += updateLog;
            }

            //更新内容
            mContentTextView.setText(msg);
            //标题
            mTitleTextView.setText(String.format("是否升级到%s版本？", newVersion));
            //强制更新
            if (mUpdateApp.isConstraint()) {
                mLlClose.setVisibility(View.GONE);
            } else {
                //不是强制更新时，才生效
                if (mUpdateApp.isShowIgnoreVersion()) {
                    mIgnore.setVisibility(View.VISIBLE);
                }
            }

            initEvents();
        }
    }

    /**
     * 初始化主题色
     */
    private void initTheme() {


        final int color = getArguments().getInt(UpdateAppManager.THEME_KEY, -1);

        final int topResId = getArguments().getInt(UpdateAppManager.TOP_IMAGE_KEY, -1);


        if (-1 == topResId) {
            if (-1 == color) {
                //默认红色
                setDialogTheme(mDefaultColor, mDefaultPicResId);
            } else {
                setDialogTheme(color, mDefaultPicResId);
            }

        } else {
            if (-1 == color) {
                //自动提色
                Palette.from(Utils.drawableToBitmap(this.getResources().getDrawable(topResId))).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int mDominantColor = palette.getDominantColor(mDefaultColor);
                        setDialogTheme(mDominantColor, topResId);
                    }
                });
            } else {
                //更加指定的上色
                setDialogTheme(color, topResId);
            }
        }


    }

    /**
     * 设置
     *
     * @param color    主色
     * @param topResId 图片
     */
    private void setDialogTheme(int color, int topResId) {
        mTopIv.setImageResource(topResId);
        mUpdateOkButton.setBackgroundDrawable(DrawableUtil.getDrawable(Utils.dip2px(4, getActivity()), color));
        mNumberProgressBar.setProgressTextColor(color);
        mNumberProgressBar.setReachedBarColor(color);
        //随背景颜色变化
        mUpdateOkButton.setTextColor(ColorUtil.isTextColorDark(color) ? Color.BLACK : Color.WHITE);
    }

    private void initEvents() {
        mUpdateOkButton.setOnClickListener(this);
        mIvClose.setOnClickListener(this);
        mIgnore.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btn_ok) {
            installApp();
            mUpdateOkButton.setVisibility(View.GONE);
        } else if (i == R.id.iv_close) {
//            if (mNumberProgressBar.getVisibility() == View.VISIBLE) {
//                Toast.makeText(getApplicationContext(), "后台更新app", Toast.LENGTH_LONG).show();
//            }
            dismiss();
        } else if (i == R.id.tv_ignore) {
            Utils.saveIgnoreVersion(getActivity(), mUpdateApp.getNewVersion());
            dismiss();
        }
    }

    private void installApp() {
        String apkUrl = mUpdateApp.getApkFileUrl();
        final String appName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1, apkUrl.length());
        File appFile = new File(mUpdateApp.getTargetPath()
                .concat(File.separator + mUpdateApp.getNewVersion())
                .concat(File.separator + appName));
        //md5不为空
        //文件存在
        //md5只一样
        if (!TextUtils.isEmpty(mUpdateApp.getNewMd5())
                && appFile.exists()
                && Md5Util.getFileMD5(appFile).equalsIgnoreCase(mUpdateApp.getNewMd5())) {
            Uri fileUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".fileProvider", appFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(appFile), "application/vnd.android.package-archive");
            }
            if (getActivity().getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                startActivity(intent);
            }
            //安装完自杀
            dismiss();
        } else {
            downloadApp();
            if (mUpdateApp.isHideDialog()) {
                dismiss();
            }

        }
    }


    /**
     * 开启后台服务下载
     */
    private void downloadApp() {
        //使用ApplicationContext延长他的生命周期
        DownloadService.bindService(getActivity().getApplicationContext(), conn);
    }

    /**
     * 回调监听下载
     */
    private void startDownloadApp(DownloadService.DownloadBinder binder) {
        // 开始下载，监听下载进度，可以用对话框显示
        if (mUpdateApp != null) {

            binder.start(mUpdateApp, new DownloadService.DownloadCallback() {
                @Override
                public void onStart() {
                    if (!UpdateDialogFragment.this.isRemoving()) {
                        mNumberProgressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onProgress(float progress, long totalSize) {
                    if (!UpdateDialogFragment.this.isRemoving()) {
                        mNumberProgressBar.setProgress(Math.round(progress * 100));
                        mNumberProgressBar.setMax(100);
                    }
                }

                @Override
                public void setMax(long total) {

                }

                @Override
                public void onFinish() {
                    if (!UpdateDialogFragment.this.isRemoving()) {
                        dismiss();
                    }
                }

                @Override
                public void onError(String msg) {
                    if (!UpdateDialogFragment.this.isRemoving()) {
                        dismiss();
                    }
                }
            });
        }
    }


    @Override
    public void onDestroyView() {
        isShow = false;
        super.onDestroyView();
    }
}
