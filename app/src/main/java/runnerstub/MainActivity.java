package runnerstub;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    //shizuku监听授权结果
    private final Shizuku.OnRequestPermissionResultListener RL = (requestCode, grantResult) -> check();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //根据系统深色模式自动切换软件的深色/亮色主题
        if (((UiModeManager) getSystemService(UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //shizuku返回授权结果时将执行RL函数
        Shizuku.addRequestPermissionResultListener(RL);

        //检查Shizuku是否运行，并申请Shizuku权限
        check();
    }

    private void check() {
        //本函数用于检查shizuku状态，b代表shizuk是否运行，c代表shizuku是否授权

        var shizukuStatus = true;
        var shizukuPermission = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else shizukuPermission = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                shizukuPermission = true;
            if (e instanceof IllegalStateException) {
                shizukuStatus = false;
                Toast.makeText(this, "Shizuku未运行", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        //在APP退出时，取消注册Shizuku授权结果监听，这是Shizuku的要求
        Shizuku.removeRequestPermissionResultListener(RL);
        super.onDestroy();
    }
}