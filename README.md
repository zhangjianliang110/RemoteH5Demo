
注意：
    与web交互相关的类不能混淆
    -keep class com.remote.webmode.interfaces.BaseRemoteJsInterface{*;}
    -keep class com.xxx.JsNativeInterface{*;}
========原理简述========

   主交互流程：html -> web进程 -> app主进程 -> 回调到web进程
   辅交互流程(非必要的)：通过广播，也可以直接 app主进程 -> web进程

   1、html->web进程，这一步不用说了，webview.addJavascriptInterface(obj, "webview");
   2、web进程 -> app主进程  通过aidl实现
   3、app主进程 -> 回调到web进程  也是通过aidl接口实现
   4、广播本身跨进程的
   5、为了让组件中供web调用的 JsInterface 与业务分离，增加了一个H5Bridge.getInstance().register(JsInterfaceImpl.class)
      H5Bridge内部解析JsInterfaceImpl.class的所有方法，缓存到map里，web进程接收到web请求时，把web请求的数据通过aidl传到主进程，然后主进程中根据web传回的数据，
      从H5Bridge的map缓存中找到对应的方法，通过反射调用执行，执行后通过传入AidlCallback接口回调到子进程。
   6、web端的调用
     webview.jsFunc("methodName", "{'key':'value', 'key2':'value2'}")
     webview是addJavascriptInterface(, "webview")中定义的别名
     jsFunc()是中转接口中@JavascriptInterface注解的统一接收web调用的函数名
     methodName是客户端与web端协商的调用的方法名，第二个参数是该方法调用需要的参数

========使用方式========
1、项目中添加aidl文件
2、启动/在启动WebView之前，在主进程中注册给网页调用的接口
    如：
        private void initH5Bridge() {
            ThreadPoolFactory.instance().fixExecutor(new Runnable() {
                @Override
                public void run() {
                    H5Bridge.getInstance().register(JsNativeInterface.class);
                }
            });
        }
3、在主进程中注册 MainRemoteService服务，供子进程连接
    <service android:name="com.silvrr.common.module.h5.aidl.mainprocess.MainRemoteService"/>
4、创建WebViewActivity 继承 BaseWebActivity
   如果不想继承BaseWebActivity也可以，需要在自定义的子进程Activity的onCreate中添加以下方法
    //开始连接主进程
    WebRemoteControl remoteControl = new WebRemoteControl(this);
    remoteControl.setServiceConnectListener(new IServiceConnectCallback() {
        @Override
        public void onServiceConnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl(getWebPresenter().getUrl());
                }
            });
        }
    });
    remoteControl.setWebView(mWebView);
    //完成连接主进程
5、在Manifest中注册WebActivity时，标明子进程
    <activity
        android:name="xxx.view.WebActivity"
        android:screenOrientation="portrait"
        android:configChanges="orientation|keyboardHidden|screenSize"
        android:process=":h5"/>

6、接收主进程消息，并做相应处理(这个可要可不要，aidl提供的web进程调用主进程的方法，然后在主进程执行完后可以回调到web进程，因此已经基本够用了)
    此处通过子进程接收广播来实现，需要标明广播所在进程与web进程在同一个进程 android:process=":h5"
    1)注册广播
    <receiver android:name=".module.h5.receiver.WebRemoteReciver"
              android:process=":h5">
        <intent-filter>
            <action android:name="com.silvrr.common.web_remote_action"/>
        </intent-filter>
    </receiver>

    2)在WebRemoteReciver中，对接收到的广播做相应处理
    3)主进程中发送广播
        RemoteMessage msg = new RemoteMessage();
        msg.mMsgType = RemoteMessage.MSG_TYPE_KILL;
        Intent intent = new Intent(WebRemoteReciver.WEB_REMOTE_ACTION);
        intent.putExtra(WebRemoteReciver.REMOTE_MESSAGE_KEY, msg);
        getContext().sendBroadcast(intent);

7、第1点中提到的给web调用的接口
    如：
    public class JsNativeInterface {

        public static void jsTestFunction(JSONObject param, IWebBinderCallback callback) {
            //操作主进程UI
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MarketApplication.getInstance(), param.toString(), Toast.LENGTH_SHORT).show();
                }
            });
            //回调给子进程调用js
            if (callback != null) {
                try {
                    callback.onResult(0, "javascript:alert('CallBack');");
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Logger.d(WebRemoteControl.TAG,"=======JsNativeInterface.callback.onResult() exception:" + e.getMessage());
                }
            }
        }
    }
    规则：
    1)方法为静态方法 static
    2)方法有两个参数
        参数一：JSONObject
                这是web端传过来的数据，格式为json格式的字符串，回调到主进程中时已经封装成JSONObject
        参数二：IWebBinderCallback
                这是h5进程中提供的，回调函数，主进程处理完web请求的方法后，可以通过该回调函数回调到子进程中
                该回调函数有两个参数(int msgType, String message)
                int msgType 第一个是消息类型，传0,表示回调的消息是要子进程webview执行js代码
                String message 第二个是消息内容，如果消息类型是0，这个message就是要执行的js代码内容

8、关于web端调用native方法的调用方式
   webview.jsFunc( "JsNativeInterface中定义的方法名" , "{'key': 'value', 'key2':'value2'}" );