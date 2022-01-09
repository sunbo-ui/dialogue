package com.mybb.dialogue;

import static android.provider.Settings.System.getString;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mybb.dialogue.uilts.MD5;


import org.json.JSONObject;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import ai.olami.android.tts.TtsPlayer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements EventListener {
    //omal必须的参数(参考文档)
    private String api_key = "d69cdb980414463893ddf65cb7f7afc5";
    private String api_secret = "9ec52067a93d4191936410fdd194595b";
    private String url = "https://cn.olami.ai/cloudservice/api";
    private Long timestamp = new Date().getTime();
    private String sign = api_secret + "api=nliappkey=" + api_key + "timestamp=" + timestamp + api_secret;

    private TextView textView;//omal人机交互话语
    private TextView userText;//语音转文本
    private Button discern;//语音识别按钮
    private Button open;//语音唤醒按钮
    private Button stop;//语音停止按钮
    private ImageView imageView;//图片显示
    private TtsPlayer mTtsPlayer;//omal语音朗读核心库
    private EventManager asr;//百度语音识别核心库
    private EventManager wp;//百度语音唤醒核心库
    //逻辑使用到的全部变量
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        init();
        asr = EventManagerFactory.create(this, "asr");
        wp = EventManagerFactory.create(this, "wp");
        asr.registerListener(this);
        wp.registerListener(new WpEventListener());
        start();
    }

    /*初始化控件*/
    private void init() {
        textView = (TextView) findViewById(R.id.text);
        userText = (TextView) findViewById(R.id.userText);
        discern = (Button) findViewById(R.id.discern);
        open = (Button) findViewById(R.id.open);
        stop = (Button) findViewById(R.id.stop);
        imageView = (ImageView) findViewById(R.id.view);

        discern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //获取data
                asr.send(SpeechConstant.ASR_START, "{}", null, 0, 0);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                flag = false;
                                discern.setText("正在识别...");
                                asr.send(SpeechConstant.ASR_START, "{}", null, 0, 0);
                            }
                        });
                    }
                }).start();

            }
        });

        mTtsPlayer = new TtsPlayer(MainActivity.this, new MyITtsPlayerListener());
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = open.getText().toString();
                if ("唤醒关闭".equals(text)) {
                    stop();
                } else {
                    start();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
                userText.setText("");
                mTtsPlayer.stop(true);
                flag = true;
            }
        });
    }

    /**
     * 开启唤醒功能
     */
    public void start() {
        HashMap<String, String> params = new HashMap<String, String>();
        // 设置唤醒资源, 唤醒资源请到 http://yuyin.baidu.com/wake#m4 来评估和导出
        params.put(SpeechConstant.WP_WORDS_FILE, "assets://WakeUp.bin");
        params.put(SpeechConstant.APP_ID, "25306303");
        String json = new JSONObject(params).toString();
        wp.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
        Toast.makeText(MainActivity.this, "唤醒功能已开启", Toast.LENGTH_SHORT).show();
        /*子线程中更新ui*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        open.setText("唤醒关闭");
                    }
                });
            }
        }).start();
    }

    /**
     * 关闭唤醒功能
     */
    public void stop() {
        wp.send("wp.stop", null, null, 0, 0);
        Toast.makeText(MainActivity.this, "唤醒功能已关闭", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        open.setText("唤醒开启");
                    }
                });
            }
        }).start();
    }

    @Override
    public void onEvent(String name, String params, byte[] bytes, int i, int i1) {
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
            // 引擎就绪，可以说话，一般在收到此事件后通过UI通知用户可以说话了
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            // 一句话的临时结果，最终结果及语义结果
            if (params == null || params.isEmpty()) {
                return;
            }
            // 一句话的最终识别结果
            Map<String, Object> map = new Gson().fromJson(params, new TypeToken<Map<String, Object>>() {
            }.getType());
            String result = String.valueOf(map.get("best_result"));

            userText.setText("");
            userText.setText(userText.getText() + result);
            if (map.get("result_type").equals("final_result")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                discern.setText("语音识别");
                                getData(userText.getText().toString());
                            }
                        });
                    }
                }).start();
            }
        }
    }

    class MyCallBack implements Callback {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "请求网络数据失败", Toast.LENGTH_SHORT).show();
        }

        //请求成功时调用该方法
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String result = null;
            try {
                result = response.body().string();
                System.out.println(result);
                Entity bean = new Gson().fromJson(result, new TypeToken<Entity>() {
                }.getType());
                if (bean != null) {
                    Entity.OMLData data = bean.data;
                    if (data != null) {
                        List<Entity.OMLData.Nli> nliList = data.nli;
                        if (nliList != null && nliList.size() > 0) {
                            for (Entity.OMLData.Nli nli : nliList) {
                                if (nli != null) {
                                    //获取大概介绍
                                    Entity.OMLData.Nli.DescObj desc_obj = nli.desc_obj;
                                    String desc = desc_obj.result;
                                    textView.setText(desc + "\n");
                                    //获取详情
                                    List<Entity.OMLData.Nli.DataObj> data_obj = nli.data_obj;
                                    if (data_obj != null && data_obj.size() > 0) {
                                        for (Entity.OMLData.Nli.DataObj dataObj : data_obj) {
                                            String description = dataObj.description;
                                            String photo_url = dataObj.photo_url;
                                            textView.setText(textView.getText() + description + "\n");
                                            if (!"".equals(photo_url) && photo_url != null && !"null".equals(photo_url)) {
                                                //新建线程加载图片信息，发送到消息队列中
                                                new Thread(() -> {
                                                    getURLimage(photo_url);
                                                }).start();
                                            }
                                        }
                                    }
                                }
                            }
                            asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
                            mTtsPlayer.playText(textView.getText().toString(), true);
                            while (mTtsPlayer.isPlaying()) {
                            }
                            if (!flag) {
                                asr.send(SpeechConstant.ASR_START, "{}", null, 0, 0);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //在消息队列中实现对控件的更改
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    //加载网络成功进行UI的更新,处理得到的图片资源
                    case 200:
                        //通过message，拿到字节数组
                        byte[] Picture = (byte[]) msg.obj;
                        //使用BitmapFactory工厂，把字节数组转化为bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(Picture, 0, Picture.length);
                        //通过imageview，设置图片
                        imageView.setImageBitmap(bitmap);
                        break;
                    //当加载网络失败执行的逻辑代码
                    default:
                        Toast.makeText(MainActivity.this, "网络出现了问题", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        /**
         * 获取网落图片资源
         *
         * @return
         */
        public void getURLimage(String url) {
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    //得到从网上获取资源，转换成我们想要的类型
                    byte[] Picture_bt = response.body().bytes();
                    //通过handler更新UI
                    Message message = handler.obtainMessage();
                    message.obj = Picture_bt;
                    message.what = 200;
                    handler.sendMessage(message);
                }
            });
        }
    }

    //对话方法
    private void getData(String text) {
        //发送请求
        OkHttpClient okHttpClient = new OkHttpClient();
        //2.创建一个RequestBody,可以用add添加键值对
        RequestBody requestBody = new FormBody.Builder()
                .add("appkey", api_key)
                .add("api_secret", api_secret)
                .add("api", "nli")
                .add("timestamp", String.valueOf(timestamp))
                .add("sign", MD5.encrypt(sign))
                .add("rq", "{'data':{'input_type':1,'text':'" + text + "'},'data_type':'stt'}")
                .build();
        //3.创建Request对象，设置URL地址，将RequestBody作为post方法的参数传入
        Request request = new Request.Builder().url(url).post(requestBody).build();
        //4.创建一个call对象,参数就是Request请求对象
        okHttpClient.newCall(request).enqueue(new MyCallBack());

    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    private class WpEventListener implements EventListener {
        @Override
        public void onEvent(String name, String params, byte[] bytes, int i, int i1) {
            if ("wp.data".equals(name)) { // 每次唤醒成功, 将会回调name=wp.data的时间, 被激活的唤醒词在params的word字段
                try {
                    JSONObject json = new JSONObject(params);
                    String word = null; // 唤醒词
                    word = json.getString("word");
                    if ("亚历山大".equals(word)) {
                        mTtsPlayer.playText("我在", true);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        flag = false;
                                        discern.setText("正在识别...");
                                        asr.send(SpeechConstant.ASR_START, "{}", null, 0, 0);
                                    }
                                });
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("wp.exit".equals(name)) {
                // 唤醒已经停止
                Toast.makeText(MainActivity.this, "唤醒已经停止", Toast.LENGTH_SHORT).show();
            }
        }
    }
}