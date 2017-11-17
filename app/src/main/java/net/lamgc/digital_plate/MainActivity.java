package net.lamgc.digital_plate;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    //坐标显示标签
    private TextView tv_X;
    private TextView tv_Y;
    //连接状态标签
    private TextView tv_CNStats;
    //触摸状态标签
    private TextView tv_DTStats;

    private Handler sendHd;


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
                }
            }
        };
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
     * 由于仅仅是记录和传输触摸坐标，所以记录后向下传递即可
     * @param event 参数
     * @return 是否消费事件,true不消费,false消费
     */
    @SuppressLint("SetTextI18n")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //创建一个消息
        Message msg = new Message();
        //设置消息类型
        msg.what = 0;
        msg.obj = event;
        sendHd.dispatchMessage(msg);
        return super.dispatchTouchEvent(event);
    }
}