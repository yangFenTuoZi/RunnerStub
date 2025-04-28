package com.shizuku.uninstaller;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.shizuku.uninstaller.databinding.ItemCommandBinding;

public class ListAdapter extends BaseAdapter {
    private final int[] data;
    private final Context mContext;

    public ListAdapter(Context mContext, int[] data) {

        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
    }


    //固定的写法
    public int getCount() {
        return data.length;
    }

    //固定的写法
    @Override
    public Object getItem(int position) {
        return null;
    }

    //固定的写法
    @Override
    public long getItemId(int position) {
        return position;
    }


    //此函数定义每一个item的显示
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemCommandBinding binding;
        if (convertView == null) {
            binding = ItemCommandBinding.inflate(LayoutInflater.from(mContext), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
//            convertView.setOnKeyListener((view, i, keyEvent) -> {
//                Toast.makeText(mContext, "sdfsdf", Toast.LENGTH_SHORT).show();
//                return false;
//            });
        } else {

            //对于已经加载过的item就直接使用，不需要再次加载了，这就是ViewHolder的作用
            binding = (ItemCommandBinding) convertView.getTag();
        }

        //获得用户对于这个格子的设置
        SharedPreferences b = mContext.getSharedPreferences(String.valueOf(data[position]), 0);
        init(binding, b);
        return convertView;
    }

    void init(ItemCommandBinding binding, SharedPreferences sp) {


        //用户是否设置了命令内容
        boolean existc = sp.getString("content", null) == null || sp.getString("content", null).isEmpty();

        //用户是否设置了命令名称
        boolean existn = sp.getString("name", null) == null || sp.getString("name", null).isEmpty();

        //这个点击事件是点击编辑命令
        View.OnClickListener voc = view -> {
            View v = View.inflate(mContext, R.layout.dialog_edit, null);
            final CheckBox cb = v.findViewById(R.id.cb);
            cb.setChecked(sp.getBoolean("shell", false));
            final EditText editText = v.findViewById(R.id.e);
            editText.setText(sp.getString("content", null));

            editText.setOnKeyListener((view1, i, keyEvent) -> {

                if (keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER&&keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                    sp.edit().putString("content", editText.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                    init(binding, sp);
                }


                return false;
            });
            final EditText editText1 = v.findViewById(R.id.title);
            editText1.setText(sp.getString("name", null));

            editText1.setOnKeyListener((view2, i, keyEvent) -> {

                    if (keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER&&keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                        sp.edit().putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                        init(binding, sp);
                    }


                return false;
            });
            editText.requestFocus();
            editText.postDelayed(() -> ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0), 200);
            new AlertDialog.Builder(mContext).setTitle("编辑命令").setView(v).setPositiveButton("完成", (dialog, which) -> {
                sp.edit().putString("content", editText.getText().toString()).putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                init(binding, sp);
            }).show();
        };

        //如果用户还没设置命令内容，则显示加号，否则显示运行符号
        binding.exec.setImageResource(existc ? R.drawable.add : R.drawable.run);

        //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
        binding.exec.setOnClickListener(!existc ? view -> {

            //这里会根据用户是否勾选了降权，来执行不同的命令
            mContext.startActivity(new Intent(mContext, ExecActivity.class).putExtra("content", sp.getBoolean("shell", false) ? "whoami|grep root &> /dev/null && echo '提示:已将root降权至shell' 1>&2;" + mContext.getApplicationInfo().nativeLibraryDir + "/libchid.so 2000 " + sp.getString("content", " ") + " || " + sp.getString("content", " ") : sp.getString("content", " ")));
        } : voc);
        binding.title.setText(existn ? "空" : sp.getString("name", "空"));
        binding.title.setTextColor(existc ? mContext.getResources().getColor(R.color.filled) : mContext.getResources().getColor(R.color.empty));
        binding.command.setText(existc ? "空" : sp.getString("content", "空"));
        binding.msgRoot.setOnClickListener(voc);
        binding.msgRoot.setOnLongClickListener(existc ? null : view -> {
            ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", sp.getString("content", "ls -l")));
            Toast.makeText(mContext, "已复制该条命令至剪贴板:\n" + sp.getString("content", "ls -l"), Toast.LENGTH_SHORT).show();
            return true;
        });

    }

}
