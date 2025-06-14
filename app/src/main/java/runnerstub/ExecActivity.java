package runnerstub;

import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Locale;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import runner.stub.R;

public class ExecActivity extends Activity {

    TextView t1, t2;
    Process p;
    Thread h1;
    boolean br = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("执行中");

        //根据系统深色模式动态改变深色主题
        if (((UiModeManager) getSystemService(Service.UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);

        //半透明背景
        getWindow().getAttributes().alpha = 0.85f;
        setContentView(R.layout.dialog_exec);
        t1 = findViewById(R.id.t1);
        t2 = findViewById(R.id.t2);
        t2.requestFocus();
        t2.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                finish();
            return false;
        });
        //子线程执行命令，否则UI线程执行就会导致UI卡住动不了
        h1 = new Thread(() -> ShizukuExec(getIntent().getStringExtra("content")));
        h1.start();
    }

    public void ShizukuExec(String cmd) {
        try {

            //记录执行开始的时间
            long time = System.currentTimeMillis();

            //使用Shizuku执行命令
            p = newProcess(new String[]{"sh"});
            if (p == null) {
                runOnUiThread(() -> {
                    t1.setText("Shizuku 进程创建失败");
                    setTitle("执行失败");
                });
                return;
            }
            OutputStream out = p.getOutputStream();
            out.write((cmd + "\nexit\n").getBytes());
            out.flush();
            out.close();

            //实时读取命令输出
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String inline;
                while ((inline = inputReader.readLine()) != null || (inline = errorReader.readLine()) != null) {
                    //如果TextView的字符太多了（会使得软件非常卡顿），或者用户退出了执行界面（br为true），则停止读取
                    if (t2.length() > 2000 || br) break;
                    String finalInline = inline;
                    runOnUiThread(() -> {
                        t2.append(finalInline);
                        t2.append("\n");
                    });
                }
                inputReader.close();
            } catch (Exception ignored) {
            }

            //等待命令运行完毕
            p.waitFor();

            //获取命令返回值
            String exitValue = String.valueOf(p.exitValue());

            //显示命令返回值和命令执行时长
            t1.post(() -> {
                t1.setText(String.format(Locale.getDefault(), "返回值：%s\n执行用时：%.2f秒", exitValue, (System.currentTimeMillis() - time) / 1000f));
                setTitle("执行完毕");
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {

        //关闭所有输入输出流，销毁进程，防止内存泄漏等问题
        br = true;

        new Handler().postDelayed(() -> {
            try {
                p.destroy();
                h1.interrupt();
            } catch (Exception ignored) {
            }
        }, 1000);
        super.onDestroy();
    }

    public static ShizukuRemoteProcess newProcess(String[] cmd) {
        try {
            Method newProcess = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            return (ShizukuRemoteProcess) newProcess.invoke(null, cmd, null, null);
        } catch (Exception e) {
            Log.e("ExecActivity", "Error creating new process", e);
            return null;
        }
    }
}