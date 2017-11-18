package net.lamgc.digital_plate;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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

    //蓝牙
    //蓝牙适配器对象
    private BluetoothAdapter bluetoothAp;
    //蓝牙广播收听者对象
    private BroadcastReceiver mBluetoothReceiver;

    //退出计时
    private long mExitTime;

    //先用wifi测试一下
    private Socket socket;
    private OutputStream os;




    /**
     * Activity创建事件
     *
     * @param savedInstanceState 参数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //向上传递事件
        super.onCreate(savedInstanceState);
        //创建布局
        setContentView(R.layout.activity_main);

        //设置代码
        setCode();

        //创建消息对象
        Message bltmsg = new Message();
        bltmsg.what = 2;
        //进行蓝牙连接操作(异步,不影响后面处理)
        sendHd.handleMessage(bltmsg);
        //获取组件对象
        tv_X = findViewById(R.id.TextView_X);
        tv_Y = findViewById(R.id.TextView_Y);
        tv_CNStats = findViewById(R.id.TextView_CNstats);
        tv_DTStats = findViewById(R.id.TextView_DTstats);
        //获取屏幕尺寸
        DisplayMetrics metric = getDisplayMetrics();
        System.out.println(
                          "WidthPix:[" + Integer.toString(metric.widthPixels) +
                        "] HeightPix:[" + Integer.toString(metric.heightPixels) +
                        "] Density:[" + Float.toString(metric.density) +
                        "] Dpi:[" + Integer.toString(metric.densityDpi) + "]"
        );
    }

    /**
     * 获取屏幕尺寸对象
     * @return 返回尺寸对象
     */
    private DisplayMetrics getDisplayMetrics(){
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric;
    }

    /**
     * 创建菜单事件
     * @param menu 菜单对象
     * @return ...
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //载入菜单布局
        getMenuInflater().inflate(R.menu.menu01, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 菜单项被点击
     * @param item 被操作菜单
     * @return 是否消费事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //注销广播收听者
        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * 设置代码块
     * 特别乱- -
     */
    @SuppressLint("HandlerLeak")
    private void setCode(){
        //异步处理数据
        sendHd = new Handler(){
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0){
                    //触摸数据处理
                    MotionEvent event = (MotionEvent) msg.obj;
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
                    System.out.println("Stats:[" + tv_DTStats.getText().toString() + "] RawXY:[" + Float.toString(event.getRawX()) + " " + Float.toString(event.getRawY()) + "] XY:[" + Float.toString(event.getX()) + " " + Float.toString(event.getY()) + "]");
                    if(msg.arg1 == 1){
                        if(socket != null && !socket.isOutputShutdown()){
                            try {
                                os.write(Motion2byte(event));
                                os.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            System.out.println("Socket未连接，拒绝发送");
                        }
                    }
                }else if(msg.what == 1){
                    //蓝牙连接操作
                    if(bluetoothAp == null){
                        //防止空指针(虽然不太可能，但还是做个防备)
                        System.out.println("[错误] 蓝牙适配器对象为Null");
                        return;
                    }

                    if(!bluetoothAp.isEnabled()){
                        System.out.println("蓝牙为关闭状态，请求打开蓝牙...");
                        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enabler, 25542);
                    }
                }else if(msg.what == 2){
                    new Thread(){
                        public void run(){
                            System.out.println("正在连接电脑端...");
                            try {
                                socket = new Socket();
                                socket.connect(new InetSocketAddress("192.168.1.104", 25565));
                                System.out.println("已连接电脑端,置OutputStream对象");
                                os = socket.getOutputStream();
                                System.out.println("连接完成");
                            } catch (IOException e) {
                                System.out.println("Socket连接出现错误:");
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        };

        //获取蓝牙适配器对象
        bluetoothAp = BluetoothAdapter.getDefaultAdapter();
        //创建事件集
        IntentFilter filter = new IntentFilter();
        //发现设备 - 已使用
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //蓝牙设备状态被改变
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //搜索状态被改变
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //设备搜索开始 - 已使用
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //设备搜索结束 - 已使用
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //设备名称被改变 - 已使用
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        //连接状态被改变
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        //注册在下面

        //蓝牙事件广播收听者代码
        mBluetoothReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                //获取状态
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //发现设备
                    //获取蓝牙设备对象
                    BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (scanDevice == null || scanDevice.getName() == null) {
                        System.out.println("获取到无效蓝牙设备对象，忽略处理");
                        return;
                    }
                    //蓝牙设备名称
                    System.out.println("发现蓝牙设备! 设备名:[" + scanDevice.getName() + "] 设备地址:[" + scanDevice.getAddress() + "]");
                }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    //蓝牙搜索开始
                    System.out.println("蓝牙扫描已开始");
                }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    //蓝牙搜索结束
                    System.out.println("蓝牙扫描已结束");
                }else if(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED.equals(action)){
                    //蓝牙设备名被更改
                    System.out.println("蓝牙设备名已更改 新名称:[" + bluetoothAp.getName() + "]");
                }

            }

        };

        //注册蓝牙广播收听者
        //registerReceiver(mBluetoothReceiver, filter);
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
    @SuppressLint("HardwareIds")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("requestCode:[" + Integer.toString(requestCode) + "] resultCode:[" + Integer.toString(resultCode) + "]");
        if(requestCode == 25542){
            //确定是蓝牙请求
            if(resultCode == 0){
                //如果被拒绝打开蓝牙，丢出提示
                System.out.println("蓝牙打开请求被拒绝");
                Toast.makeText(MainActivity.this, "必须打开蓝牙并连接到电脑才能使用!", Toast.LENGTH_LONG).show();
            }else if(resultCode == -1){
                System.out.println("蓝牙打开请求已通过 设备名:[" + bluetoothAp.getName() + "] 地址:[" + bluetoothAp.getAddress() + "]");
                /*
                if (bluetoothAp.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    startActivity(discoverableIntent);
                }*/
                //设置蓝牙可见(120s)
                setDiscoverableTimeout();
                //开始搜索蓝牙设备
                bluetoothAp.startDiscovery();
                System.out.println("蓝牙设备搜索已启动");
                Toast.makeText(MainActivity.this, "蓝牙已可见，请在电脑连接本设备", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 120s蓝牙可见
     */
    public void setDiscoverableTimeout() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
            setScanMode.setAccessible(true);
            setDiscoverableTimeout.invoke(adapter, 120);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        msg.arg1 = 1;
        msg.obj = event;
        //投递消息
        sendHd.handleMessage(msg);
        //消费事件
        return super.dispatchTouchEvent(event);
    }

    /**
     * 将触摸数据转为字节数组
     * @param event 事件参数
     * @return 字节
     */
    private byte[] Motion2byte(MotionEvent event){
        //状态[byte][1] + X坐标[float][4] + Y坐标[float][4] + 结束标识[byte][3]
        //有效数据长度9 无效数据长度3 总长度12
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

    /**
     * 生成尺寸数据
     * @param dm
     * @return
     */
    private byte[] Display2byte(DisplayMetrics dm){

        return null;
    }
}