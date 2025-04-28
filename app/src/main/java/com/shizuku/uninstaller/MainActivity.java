package com.shizuku.uninstaller;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.LayoutAnimationController;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.shizuku.uninstaller.databinding.ActivityMainBinding;
import com.shizuku.uninstaller.databinding.DialogHelpBinding;
import com.shizuku.uninstaller.databinding.DialogSettingsBinding;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    ActivityMainBinding binding;
    int shizukuStatusTextCurrentTextColor;
    SharedPreferences sp;
    //shizuku监听授权结果
    private final Shizuku.OnRequestPermissionResultListener RL = this::onRequestPermissionsResult;


    private void onRequestPermissionsResult(int i, int i1) {
        check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //根据系统深色模式自动切换软件的深色/亮色主题
        if (((UiModeManager) getSystemService(Service.UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);
        sp = getSharedPreferences("data", 0);
        //如果是初次开启，则展示help界面
        if (sp.getBoolean("first", true)) {
            showHelp();
            sp.edit().putBoolean("first", false).apply();
        }
        //读取用户设置“是否隐藏后台”，并进行隐藏后台
        ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(sp.getBoolean("hide", true));
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        binding.logo.setOnClickListener(this::change);
        binding.shizukuStatus.setOnClickListener(v -> check());
        binding.exec.setOnClickListener(this::exe);

        //限定一下横屏时的窗口宽度,让其不铺满屏幕。否则太丑
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            getWindow().getAttributes().width = (getWindowManager().getDefaultDisplay().getHeight());

        //设置猫猫图案的长按事件为展示帮助界面
        binding.logo.setOnLongClickListener(view -> {
            showHelp();
            return false;
        });

        //shizuku返回授权结果时将执行RL函数
        Shizuku.addRequestPermissionResultListener(RL);

        //m用于保存shizuku状态显示按钮的初始颜色（int类型哦），为的是适配安卓12的莫奈取色，方便以后恢复颜色时用
        shizukuStatusTextCurrentTextColor = binding.shizukuStatus.getCurrentTextColor();

        //检查Shizuku是否运行，并申请Shizuku权限
        check();

        //为两列listView适配每个item的具体样式和总item数
        initlist();
    }

    private void showHelp() {
        //展示帮助界面
        DialogHelpBinding helpBinding = DialogHelpBinding.inflate(LayoutInflater.from(this));
        helpBinding.preDec.setText(Html.fromHtml("&nbsp;&nbsp;本应用<u><b><big>不会</big></b></u>收集您的任何信息，且完全不包含任何联网功能。继续使用则代表您同意上述隐私政策。<br>&nbsp;&nbsp;使用本应用需要您的设备已安装并激活Shizuku。<br>&nbsp;&nbsp;在后续的使用中，您可以<u><b><big>长按</big></b></u>主界面标题上的猫猫图案(如下图所示)来打开此帮助界面。", 0));
        helpBinding.usage.setText(Html.fromHtml("&nbsp;&nbsp;--点击编辑某个栏目；长按复制该栏目中保存的命令。<br><br>&nbsp;&nbsp;--<u><b><big>单击</big></b></u>标题上的猫猫图案来切换APP为一次性运行命令的模式。<br><br>&nbsp;&nbsp;--点击主界面标题上的两个显示Shizuku状态的按钮中的任意一个，均可<u><b><big>刷新Shiuzku状态</big></b></u>。当然，关闭再打开本APP也是不错的刷新方法。<br><br>&nbsp;&nbsp;--如果您设备上的Shizuku服务是由root权限启动的，那么本APP在执行命令时也将具有root权限。假如您不希望<u><b><big>以如此高的权限执行命令</big></b></u>(大材小用)，您可以勾选命令编辑界面的“将root权限降至Shell”来让APP仅使用Shell权限执行命令。<br><br>&nbsp;&nbsp;--您可以点击本界面下方的设置按钮探索更多功能哦！", 0));
        new AlertDialog.Builder(this)
                .setTitle("使用帮助")
                .setView(helpBinding.getRoot())
                .setNegativeButton("OK", null)
                .setNeutralButton("设置", (dialogInterface, i) -> {
                    final AlertDialog dialog = new AlertDialog.Builder(this).create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().getAttributes().alpha = 0.85f;
                        dialog.getWindow().setGravity(Gravity.BOTTOM);
                    }

                    DialogSettingsBinding settingsBinding = DialogSettingsBinding.inflate(LayoutInflater.from(this));

                    settingsBinding.notShowBackground.setChecked(sp.getBoolean("hide", true));
                    settingsBinding.notShowBackground.setOnCheckedChangeListener((compoundButton, b) -> {
                        sp.edit().putBoolean("hide", b).apply();
                        ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(b);
                    });

                    settingsBinding.moreCommands.setChecked(sp.getBoolean("20", false));
                    settingsBinding.moreCommands.setOnCheckedChangeListener((compoundButton, b) -> {
                        sp.edit().putBoolean("20", b).apply();
                        Toast.makeText(this, "重启APP后生效", Toast.LENGTH_SHORT).show();
                    });
                    dialog.setView(settingsBinding.getRoot());
                    dialog.show();
                })
                .create().show();

    }

    private void check() {

        //本函数用于检查shizuku状态，b代表shizuk是否运行，c代表shizuku是否授权
        boolean shizukuStatus = true;
        boolean shizukuPermission = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else shizukuPermission = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                shizukuPermission = true;
            if (e.getClass() == IllegalStateException.class) {
                shizukuStatus = false;
                Toast.makeText(this, "Shizuku未运行", Toast.LENGTH_SHORT).show();
            }
        }
        binding.shizukuStatus.setText(shizukuStatus ? "Shizuku\n已运行" : "Shizuku\n未运行");
        binding.shizukuStatus.setTextColor(shizukuStatus ? shizukuStatusTextCurrentTextColor : 0x77ff0000);
        binding.shizukuPermission.setText(shizukuPermission ? "Shizuku\n已授权" : "Shizuku\n未授权");
        binding.shizukuPermission.setTextColor(shizukuPermission ? shizukuStatusTextCurrentTextColor : 0x77ff0000);
    }

    @Override
    protected void onDestroy() {
        //在APP退出时，取消注册Shizuku授权结果监听，这是Shizuku的要求
        Shizuku.removeRequestPermissionResultListener(RL);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //在点击返回键时直接退出APP，因为APP比较轻量，没必要双击返回退出或者设置什么退出限制
        finish();
    }

    public void change(View view) {
        //单击猫猫头像的点击事件，让list变不可见，让EditText可见。

        flipAnimation(view);
        binding.listLeft.setVisibility(View.INVISIBLE);
        binding.listRight.setVisibility(View.INVISIBLE);
        binding.listLeft.setAdapter(new ListAdapter(this, new int[]{}));
        binding.listRight.setAdapter(new ListAdapter(this, new int[]{}));
        binding.execCommandRoot.setVisibility(View.VISIBLE);
        binding.commandEdit.setEnabled(true);
        binding.commandEdit.requestFocus();
        binding.commandEdit.postDelayed(() -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(binding.commandEdit, 0), 200);
        binding.commandEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                exe(v);
            }
            return false;
        });
        binding.commandEdit.setOnKeyListener((view2, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                exe(view2);
            return false;
        });

        view.setOnClickListener(view3 -> {
            flipAnimation(view3);
            binding.listLeft.setVisibility(View.VISIBLE);
            binding.listRight.setVisibility(View.VISIBLE);
            binding.commandEdit.setEnabled(false);
            initlist();
            binding.execCommandRoot.setVisibility(View.GONE);
            view3.setOnClickListener(this::change);
        });
    }


    private void flipAnimation(View view) {
        //flipAnimation是一个轻量级的翻转动画，很有趣哦
        ObjectAnimator a2 = ObjectAnimator.ofFloat(view, "rotationY", 90f, 0f);
        a2.setDuration(300).setInterpolator(new LinearInterpolator());
        a2.start();

    }


    public void exe(View view) {

        //EditText右边的执行按钮，点击后的事件
        if (binding.commandEdit.getText().length() > 0)
            startActivity(new Intent(this, ExecActivity.class).putExtra("content", binding.commandEdit.getText().toString()));
    }


    public void initlist() {
        //根据用户设置，选择展示10个格子或者更多格子
        int[] e1 = sp.getBoolean("20", false) ? new int[]{5, 6, 7, 8, 9, 15, 16, 17, 18, 19, 25, 26, 27, 28, 29, 35, 36, 37, 38, 39, 45, 46, 47, 48, 49} : new int[]{5, 6, 7, 8, 9};
        int[] d1 = sp.getBoolean("20", false) ? new int[]{0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34, 40, 41, 42, 43, 44} : new int[]{0, 1, 2, 3, 4};
        binding.listRight.setAdapter(new ListAdapter(this, e1));
        binding.listLeft.setAdapter(new ListAdapter(this, d1));

        //加一点动画，非常的丝滑~~
        TranslateAnimation animation = new TranslateAnimation(-50f, 0f, -30f, 0f);
        animation.setDuration(500);
        LayoutAnimationController controller = new LayoutAnimationController(animation, 0.1f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        binding.listLeft.setLayoutAnimation(controller);
        animation = new TranslateAnimation(50f, 0f, -30f, 0f);
        animation.setDuration(500);
        controller = new LayoutAnimationController(animation, 0.1f);
        binding.listRight.setLayoutAnimation(controller);
    }
}
