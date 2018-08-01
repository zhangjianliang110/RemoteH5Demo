package com.example.remoteh5;

import com.remote.binder.webview.IWebBinderCallback;
import com.remote.webmode.WebConst;

import org.json.JSONObject;

import android.os.RemoteException;

/**
 * 给web端调用的方法
 * Created by zhangjianliang on 2018/5/22
 */
public class JsNativeInterface {

    /**
     * 提供给web端获取客户端提供的所有api
     * param 格式 "{'func' : '回调需要执行的js方法'}"
     */
    public static void supportApis(JSONObject param, IWebBinderCallback callback) {
        if (callback != null) {//回调给子进程调用js
            try {
                if (param == null) {
                    return;
                }
                String callBackFunc = param.optString("func");
                String jsFunc = "javascript:" + callBackFunc;
                String supportApis = "response to web page success!";
                String js = jsFunc + "(\"" + supportApis.replaceAll("\"", "\\\\\"") + "\")";
                callback.onResult(WebConst.MSG_TYPE_JS, js);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
