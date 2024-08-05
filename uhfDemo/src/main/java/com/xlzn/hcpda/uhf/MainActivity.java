package com.xlzn.hcpda.uhf;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.xlzn.hcpda.DeviceConfigManage;
import com.xlzn.hcpda.ModuleAPI;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFVersionInfo;
import com.xlzn.hcpda.uhf.enums.InventoryModeForPower;
import com.xlzn.hcpda.uhf.module.UHFReaderSLR;
import com.xlzn.hcpda.uhf.ui.main.ConfigFragment;
import com.xlzn.hcpda.uhf.ui.main.FragmentPagerAdapter;
import com.xlzn.hcpda.uhf.ui.main.InventoryFragment;
import com.xlzn.hcpda.uhf.ui.main.KillFragment;
import com.xlzn.hcpda.uhf.ui.main.LockFragment;
import com.xlzn.hcpda.uhf.ui.main.MyFragment;
import com.xlzn.hcpda.uhf.ui.main.ReadFragment;
import com.xlzn.hcpda.uhf.ui.main.WriteFragment;
import com.xlzn.hcpda.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "TAG";
    private List<Fragment> datas = new ArrayList<>();
    private List<String> titles = new ArrayList<>();

    FragmentPagerAdapter fragmentPagerAdapter;
    ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.loadSoundPool(this);
        Intent intent = getIntent();
        int fetchValue = intent.getIntExtra("fetch", 0);
        String category = intent.getStringExtra("category");
        String mode = intent.getStringExtra("mode"); // Retrieve the mode value

        Log.d(TAG, "Fetch Value in MainActivity: " + fetchValue);
        if (category != null) {
            Log.d(TAG, "Category in MainActivity: " + category);
        }
        if (mode != null) {
            Log.d(TAG, "Mode in MainActivity: " + mode); // Log mode for debugging
        }

        Log.e(TAG, "onCreate: " +Build.FINGERPRINT );
          viewPager = findViewById(R.id.view_pager);
        TabLayout tabs = findViewById(R.id.tabs);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.item_UHFVersion:
                        UHFReaderResult<UHFVersionInfo> uhfReaderResult = UHFReader.getInstance().getVersions();
                        if (uhfReaderResult.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
                            Toast.makeText(MainActivity.this, getString(R.string.firmware_ver) + uhfReaderResult.getData().getFirmwareVersion(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.item_Temperature:
                        UHFReaderResult<Integer> result = UHFReader.getInstance().getTemperature();
                        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
                            Toast.makeText(MainActivity.this, getString(R.string.temperature) + result.getData(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.item_apiVersion:
                        Toast.makeText(MainActivity.this, getString(R.string.api) + ModuleAPI.getVersionCode, Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.item_appVersion:
//                        Toast.makeText(MainActivity.this, getString(R.string.app) + BuildConfig.VERSION_NAME, Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, getString(R.string.app) +":"+ DeviceConfigManage.module_type, Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
        //把fragment添加到list里
        // Add fragments to the list
        //datas.add(new InventoryFragment());
        InventoryFragment inventoryFragment = new InventoryFragment();
        Bundle args = new Bundle();
        if (fetchValue == 1) {

            args.putInt("fetch", fetchValue);

        }
        // Conditionally add category and mode to the bundle
        if (category != null) {
            args.putString("category", category);
        }
        if (mode != null) {
            args.putString("mode", mode);
        }
        inventoryFragment.setArguments(args);
        /*InventoryFragment inventoryFragment = new InventoryFragment();
        Bundle args = new Bundle();
        args.putInt("fetch", fetchValue);
        if (category != null) {
            args.putString("category", category);
        }
        if (mode != null) {
            args.putString("mode", mode); // Pass the mode value to InventoryFragment
        }
        inventoryFragment.setArguments(args);
        datas.add(inventoryFragment);*/
        datas.add(inventoryFragment);
        datas.add(new ConfigFragment());
        datas.add(new ReadFragment());
        datas.add(new WriteFragment());
        datas.add(new LockFragment());

        datas.add(new KillFragment());

        //tabtitle添加数据
        titles.add(getString(R.string.inventory));
        titles.add(getString(R.string.set));
        titles.add(getString(R.string.read));
        titles.add(getString(R.string.write));
        titles.add(getString(R.string.lock));
        titles.add(getString(R.string.destory));
        //自定义类传递数据
         fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager(), datas, titles);
        //设置配适器
        viewPager.setAdapter(fragmentPagerAdapter);
        //绑定tab和frment
        tabs.setupWithViewPager(viewPager);
        LoggerUtils.d(TAG, "demo 启动");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.releaseSoundPool();
        close();
        Log.e(TAG, "onDestroy: ");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown:  " + keyCode);
        if (event.getRepeatCount() == 0 && keyCode == 293 || keyCode == 290 || keyCode == 287|| keyCode == 286) {

                MyFragment myFragment = (MyFragment) fragmentPagerAdapter.getItem(viewPager.getCurrentItem());
                myFragment.onKeyDownTo(keyCode);

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }



    @Override
    protected void onPause() {
        super.onPause();
        close();

        Log.e(TAG, "onPause: ");
    }


    @Override
    protected void onResume() {
        super.onResume();

        new OpenTask().execute();
    }


    private void close() {

        UHFReader.getInstance().disConnect();

    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                //自动获取权限
            } else {
                //跳转到设置界面引导用户打开
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        } else {
            //自动获取权限
        }
    }

    public class OpenTask extends AsyncTask<String, Integer, UHFReaderResult> {
        ProgressDialog progressDialog;

        @Override
        protected UHFReaderResult doInBackground(String... params) {
            return UHFReader.getInstance().connect(MainActivity.this);
        }

        @Override
        protected void onPostExecute(UHFReaderResult result) {
            super.onPostExecute(result);
            if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
//                UHFReader.getInstance().setSession(UHFSession.S1);
                if (UHFReaderSLR.getInstance().is5300) {
                    Log.e(TAG, "onPostExecute: 省电模式");
                    UHFReader.getInstance().setInventoryModeForPower(InventoryModeForPower.POWER_SAVING_MODE);
                }
                UHFReader.getInstance().setPower(HcPreferences.getInstance().getInt(getApplicationContext(), "pda", "power"));
                Toast.makeText(MainActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
            }
            progressDialog.cancel();
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.start_connect));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }



}