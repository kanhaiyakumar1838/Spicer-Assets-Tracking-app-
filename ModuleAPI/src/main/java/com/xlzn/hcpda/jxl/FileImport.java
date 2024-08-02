package com.xlzn.hcpda.jxl;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

import com.xlzn.hcpda.uhf.entity.UHFTagEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2021/6/22.
 */

public class FileImport {
    //    static String xlsFilePath = Environment.getExternalStorageDirectory() + "/Android/";
    @SuppressLint("SdCardPath")
    static String xlsFilePath = "/storage/emulated/0/Download/";

    public static boolean daochu(String tmpname, List<UHFTagEntity> lists2) {

        try {

            String file = xlsFilePath+tmpname;
//            if (tmpname.isEmpty())
//                file = xlsFilePath + "xls"
//                        + GetTimesyyyymmddhhmmss() + ".xls";
//            else
//                file = xlsFilePath + tmpname;
            File path2 = new File(xlsFilePath);
//            @SuppressLint("SdCardPath") File path2 = new File("/sdcard/Android/data/com.example.uhf/");
//            if (path2.mkdirs()) {
//                Log.e("TAG", "创建成功: ");
//            } else {
//                Log.e("TAG", "创建失败: ");
//            }

            if (path2.exists()) {
                path2.delete();
            }
            Log.e("TAG", "daochu: " + Environment.getExternalStorageDirectory());
            List<Object> al22 = new ArrayList<Object>();
            List<String> al2 = new ArrayList<String>();

            al2.add("编号");

            // al2.add("筛选栏");

            al22.add(al2);
            FileXls.writeXLS(file, al22);
            List<Object> ac = new ArrayList<Object>();
            for (int i = 0; i < lists2.size(); i++) {
                List<String> al = new ArrayList<String>();
                al.add( lists2.get(i).getEcpHex());
                // al.add(sxl);
                ac.add(al);
            }

            return FileXls.writeXLS(file, ac);
        } catch (Exception ex) {

            Log.e("TAG", "导出异常 = : " + ex.getMessage());
            return false;
        }
    }

    public static String GetTimesyyyymmdd() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String dt = formatter.format(curDate);

        return dt;

    }

    public static String GetTimesddMMyy() {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String dt = formatter.format(curDate);

        return dt;

    }

    public static String GetTimesyyyymmddhhmmss() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String dt = formatter.format(curDate);

        return dt;

    }
}
