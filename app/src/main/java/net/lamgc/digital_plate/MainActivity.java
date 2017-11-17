package net.lamgc.digital_plate;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.TextView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    //坐标显示标签
    private TextView tv_X;
    private TextView tv_Y;
    //连接状态标签
    private TextView tv_CNStats;
    //触摸状态标签
    private TextView tv_DTStats;
    //异步处理
    private Handler sendHd;
    private BluetoothAdapter bluetoothAp;

    private long aLong;

    /**
     * Activity创建事件
     *
     * @param savedInstanceState 参数
     */
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //向上传递事件
        super.onCreate(savedInstanceState);
        //创建布局
        setContentView(R.layout.activity_main);
        //异步处理数据
        sendHd = new Handler(){
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0){
                    //触摸数据处理
                    MotionEvent event = (MotionEvent) msg.obj;
                    aLong++;
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        tv_DTStats.setText("Down");
                    }else if(event.getAction() == MotionEvent.ACTION_UP){
                        tv_DTStats .setText("Up");
                    }else if(event.getAction() == MotionEvent.ACTION_MOVE){
                        tv_DTStats .setText("Move");
                    }else{
                        tv_DTStats .setText("Null");
                    }
                    tv_X.setText(Float.toString(event.getRawX()) + " [X]");
                    tv_Y.setText(Float.toString(event.getRawY()) + " [Y]");
                    System.out.println("[" + Long.toString(aLong) + "] Stats:[" + tv_DTStats.getText().toString() + "] RawXY:[" + Float.toString(event.getRawX()) + " " + Float.toString(event.getRawY()) + "] XY:[" + Float.toString(event.getX()) + " " + Float.toString(event.getY()) + "]");
                }else if(msg.what == 1){
                    //蓝牙连接操作
                    //防止空指针(虽然不太可能，但还是做个防备)
                    if(bluetoothAp == null){
                        System.out.println("[错误] 蓝牙适配器对象为Null");
                        return;
                    }

                    if(!bluetoothAp.isEnabled()){
                        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enabler, 2);
                    }
                }
            }
        };
        //获取蓝牙适配器对象
        bluetoothAp = BluetoothAdapter.getDefaultAdapter();
        //创建消息对象
        Message bltmsg = new Message();
        bltmsg.what = 1;
        //进行蓝牙连接操作(异步,不影响后面处理)
        sendHd.handleMessage(bltmsg);
        //获取组件对象
        tv_X = findViewById(R.id.TextView_X);
        tv_Y = findViewById(R.id.TextView_Y);
        tv_CNStats = findViewById(R.id.TextView_CNstats);
        tv_DTStats = findViewById(R.id.TextView_DTstats);
        //获取屏幕尺寸
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        System.out.println(
                "WidthPix:[" + Integer.toString(metric.widthPixels) +
                        "] HeightPix:[" + Integer.toString(metric.heightPixels) +
                        "] Density:[" + Float.toString(metric.density) +
                        "] Dpi:[" + Integer.toString(metric.densityDpi) + "]"
        );
    }

    /**
     * 用来接收屏幕旋转事件
     * 以防止重启Activity
     *
     * @param newConfig 参数,会向上传递
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * startActivityForResult回传方法
     * @param requestCode 请求码，识别操作
     * @param resultCode 返回值
     * @param data 数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

        /**
         * 由于仅仅是记录和传输触摸坐标，所以记录后向下传递即可
         * @param event 参数
         * @return 是否消费事件,true不消费,false消费
         */
    @SuppressLint("SetTextI18n")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //创建一个消息
        Message msg = new Message();
        //设置消息类型，及设置类对象
        msg.what = 0;
        msg.obj = event;
        //投递消息
        sendHd.handleMessage(msg);
        //消费事件
        return true;
    }

    /**
     * 将触摸数据转为字节数组
     * @param event 事件参数
     * @return 字节
     */
    private byte[] 包装数据(MotionEvent event){
        //状态[byte][1] + X坐标[float][4] + Y坐标[float][4] + 结束标识[byte][3]
        //状态 1/Down 2/Move 3/Up 0/Null
        //结束标识 {-1 -1 -1}
        ByteBuffer date = ByteBuffer.allocate(1 + 4 + 4 + 3);
        //置状态
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            date.put((byte)1);
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            date.put((byte)2);
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            date.put((byte)3);
        }else{
            date.put((byte)0);
        }
        //置坐标
        date.putFloat(event.getX());
        date.putFloat(event.getY());
        //置结束标识
        date.put(new byte[]{-1, -1, -1});

        return date.array();
    }
}