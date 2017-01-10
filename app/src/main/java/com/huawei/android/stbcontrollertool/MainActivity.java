package com.huawei.android.stbcontrollertool;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    private Thread subscribeThread;
    private Thread publishThread;
    private final static String QUEUE_NAME_CMD = "queue_cmd";
    private final static String QUEUE_NAME_RESULT = "queue_result";
    private RabbitConnect mrabbitConnect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mrabbitConnect=new RabbitConnect("189.11.5.35","ccy","123456");
        setupPubButton();

        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                showInTextView(message);

            }
        };
        subscribe(incomingMessageHandler);


    }
    //显示结果
    private void showInTextView(String str){
        TextView tv = (TextView) findViewById(R.id.showMessage);
        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
//                tv.append(ft.format(now) + ' ' + message + '\n');
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(ft.format(now)+ ' ' + str +"\n" +tv.getText());
    }
    //设置按钮事件
    void setupPubButton() {
        Button buttonRunService = (Button) findViewById(R.id.runService);
        buttonRunService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.d("", "开启服务" );

                Intent intent = new Intent(getApplicationContext(),MainService.class);//.newIntent(this);
                startService(intent);
            }
        });

        Button buttonStopService = (Button) findViewById(R.id.stopService);
        buttonStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.d("", "停止服务" );
                Intent intent = new Intent(getApplicationContext(),MainService.class);//.newIntent(this);
                stopService(intent);
            }
        });


        Button buttonSend = (Button) findViewById(R.id.publish);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText et = (EditText) findViewById(R.id.inputMessage);
                Log.d("", "[q]发送消息 " + et.getText().toString());
                publishToAMQP(et.getText().toString(),QUEUE_NAME_CMD);
                et.setText("");
            }
        });

        Button buttonRun = (Button) findViewById(R.id.runShellCMD);
        buttonRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText et = (EditText) findViewById(R.id.inputMessage);
                Log.d("", "运行Shell命令: " + et.getText().toString());

                ShellExecuter shellExecuter=new ShellExecuter();
//                String result=ShellUtils.runShellForResult(new String[]{et.getText().toString()});
                String command=et.getText().toString();
                if(command!=null&&!command.equals("")){
                    showInTextView("执行命令："+et.getText().toString());
                    String result=shellExecuter.Executer(command);
//                    String result=ShellUtils.runShellForResult(new String[]{command});
                    showInTextView("执行结果：\n"+result);
                }else {
                    showInTextView("请输入shell命令：\n");
                }

//                et.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mrabbitConnect.disConnectRabbit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        subscribeThread.interrupt();
    }



    public void publishToAMQP(final String message,final String queueName) {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mrabbitConnect.publishMessage(queueName,message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

            }
        });
        publishThread.start();
    }

    void subscribe(final Handler handler) {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Channel channel=mrabbitConnect.getNewChannel(QUEUE_NAME_RESULT);
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                            String message = new String(body, "UTF-8");
                            Log.d("", "收到消息:" + message);
                            //

                            //将消息显示在界面
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    };
                    channel.basicConsume(QUEUE_NAME_RESULT, true, consumer);
                } catch (Exception e1) {
                    Log.d("", "Connection broken: " + e1.getClass().getName());
                }

            }
        });
        subscribeThread.start();
    }
}
