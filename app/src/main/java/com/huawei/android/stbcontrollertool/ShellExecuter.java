package com.huawei.android.stbcontrollertool;

/**
 * Created by 47895 on 2017/1/7.
 */

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellExecuter {

    public ShellExecuter() {

    }

    public String Executer(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            Log.i("ShellExecuter","执行命令："+command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = output.toString();
        return response;

    }
}
