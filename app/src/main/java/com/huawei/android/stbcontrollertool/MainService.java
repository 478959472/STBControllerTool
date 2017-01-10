package com.huawei.android.stbcontrollertool;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.rabbitmq.client.QueueingConsumer;

/**
 * Created by 47895 on 2017/1/8.
 */

public class MainService extends Service {
    private static final String TAG ="MainService";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private RabbitConnect mrabbitConnect;
    private final static String QUEUE_NAME_CMD = "queue_cmd";
    private final static String QUEUE_NAME_RESULT = "queue_result";
    private static boolean receiveFlag=true;

    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }
    // 处理从线程接收的消息
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // 通常我们在这里执行一些工作，比如下载文件。
            for(int i=0;i<5;i++){
                synchronized (this) {
                    try {
                        wait(1000);
                    } catch (Exception e) {
                    }
                }
                Log.i(TAG, "服务运行中： " + i);
            }
            // 根据startId终止服务，这样我们就不会在处理其它工作的过程中再来终止服务
//            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // 启动运行服务的线程。
        // 请记住我们要创建一个单独的线程，因为服务通常运行于进程的主线程中，可我们不想阻塞主线程。
        // 我们还要赋予它后台运行的优先级，以便计算密集的工作不会干扰我们的UI。
        HandlerThread thread = new HandlerThread(TAG,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // 获得HandlerThread的Looper队列并用于Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String rabbitHost = "192.168.1.101";
        if(intent.getStringExtra("rabbitHost")!=null&&!intent.getStringExtra("rabbitHost").equals("")){
            rabbitHost=intent.getStringExtra("rabbitHost");
            Log.i(TAG,"外部参数rabbitHost："+rabbitHost);
        }
        mrabbitConnect=new RabbitConnect(rabbitHost,"ccy","123456");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // 对于每一个启动请求，都发送一个消息来启动一个处理
        // 同时传入启动ID，以便任务完成后我们知道该终止哪一个请求。
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    QueueingConsumer consumer=mrabbitConnect.getConsumer(QUEUE_NAME_CMD);
                    while (receiveFlag) {
                        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                        String message = new String(delivery.getBody());
                        System.out.println(" [x] Received '" + message + "'");
                        ShellExecuter shellExecuter=new ShellExecuter();
                        String command=message;
                        if(command!=null&&!command.equals("")){
                            Log.i("","执行命令："+command);
                            String result=shellExecuter.Executer(command);
                            Log.i("","执行结果："+result);
                            mrabbitConnect.publishMessage(QUEUE_NAME_RESULT,result);
                        }
                    }
                    mrabbitConnect.disConnectRabbit();
                } catch (Exception e1) {
                    Log.d("", "Connection broken: " + e1.getClass().getName());
                }

            }
        });
        // 如果我们被杀死了，那从这里返回之后被重启
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 我们不支持绑定，所以返回null
        return null;
    }

    @Override
    public void onDestroy() {
        receiveFlag=false;
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
