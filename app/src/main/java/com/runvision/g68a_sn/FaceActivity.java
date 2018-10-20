package com.runvision.g68a_sn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.runvision.bean.AppData;
import com.runvision.bean.FaceInfo;
import com.runvision.bean.ImageStack;
import com.runvision.broadcast.NetWorkStateReceiver;
import com.runvision.core.Const;
import com.runvision.db.Record;
import com.runvision.db.User;
import com.runvision.gpio.GPIOHelper;
import com.runvision.gpio.SlecProtocol;
import com.runvision.myview.MyCameraSuf;
import com.runvision.thread.BatchImport;
import com.runvision.thread.FaceFramTask;
import com.runvision.thread.HeartBeatThread;
import com.runvision.thread.SocketThread;
import com.runvision.utils.CameraHelp;
import com.runvision.utils.DateTimeUtils;
import com.runvision.utils.FileUtils;
import com.runvision.utils.IDUtils;
import com.runvision.utils.SPUtil;
import com.runvision.utils.SendData;
import com.runvision.utils.TestDate;
import com.runvision.webcore.ServerManager;
import com.wits.serialport.SerialPortManager;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import android_serialport_api.SerialPort;

/**
 * 人脸验证
 */
public class FaceActivity extends Activity implements View.OnClickListener {
    private static String TAG = FaceActivity.class.getSimpleName();

    private Context mContext;
    //private ComperThread mComperThread;//1:n比对线程
    private MyRedThread mMyRedThread;//红外线程
    private UIThread uithread;//UI线程

    //////////////////////////////////////////////////视图控件
    public MyCameraSuf mCameraSurfView;
    private RelativeLayout home_layout;

    private View promptshow_xml;//提示框
    private TextView loadprompt;

    private View oneVsMoreView;  //1:N
    private ImageView oneVsMore_face, oneVsMore_temper;
    private TextView oneVsMore_userName, oneVsMore_userID, oneVsMore_userType;

    private View pro_xml;//刷卡标记

    public int logshowflag = 0;

    private MediaPlayer mPlayer;//音频

    private boolean TipsFlag = false;

    private FaceFramTask faceDetectTask = null;

    private boolean bStop = false;

    private boolean oneVsMoreThreadStauts = false;
    private boolean isOpenOneVsMore = true;
    private boolean Infra_red = true;
    private ImageStack imageStack;

    public boolean comparisonEnd = false;
    private int timingnum = 0;

    private MyApplication application;
    private SocketThread socketThread;
    private HeartBeatThread heartBeatThread;

    private Dialog dialog = null;
    private int templatenum = 0;

    private Boolean SysTimeflag = true;

    List<User> mList;

    /**
     * 消息响应
     */
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Const.UPDATE_UI://更新UI

                    if (Const.DELETETEMPLATE == true) {
                        isOpenOneVsMore = false;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Const.DELETETEMPLATE = false;
                                isOpenOneVsMore = true;
                            }
                        }, 2000);
                    }

                    /*显示逻辑*/
                    if (promptshow_xml.getVisibility() == View.VISIBLE) {
                        oneVsMoreView.setVisibility(View.GONE);
                        pro_xml.setVisibility(View.GONE);
                        // home_layout.setVisibility(View.GONE);
                    }
                    if (isOpenOneVsMore == false) {
                        mHandler.removeMessages(Const.COMPER_END);
                        mHandler.removeMessages(Const.MSG_FACE);
                    }
                    if (faceDetectTask != null) {
                        if (faceDetectTask.faceflag == true)//检测到有人脸
                        {
                            logshowflag = 0;
                            if (SerialPort.Fill_in_light == false) {
                                SerialPort.openLED();
                            }
                        }
                    }
                    if (SerialPort.Fill_in_light == true) {   //补光灯
                        timingnum++;
                        if (timingnum >= 100) {
                            Log.i("zhuhuilong", "Fill_in_light:" + SerialPort.Fill_in_light);
                            SerialPort.Fill_in_light = false;
                            timingnum = 0;
                        }
                    }

                    if ((templatenum == 20) || (Const.VMS_TEMPLATE == true)) {
                        Const.VMS_TEMPLATE = false;
                        Log.i("Gavin_debug", "templatenum==20");
                        promptshow_xml.setVisibility(View.GONE);
                        isOpenOneVsMore = true;//1:n
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = true;//人脸框
                        }
                        Infra_red = true;
                        if (mMyRedThread != null) {
                            mMyRedThread.startredThread();
                        }
                    }
                    break;

                case Const.MSG_FACE://开启一比n处理
                    FaceInfo info = (FaceInfo) msg.obj;
                    openOneVsMoreThread(info);
                    break;
                case Const.COMPER_END://1:n比对显示
                    showAlert();
                    break;
                case Const.TEST_INFRA_RED://红外处理
                    int count1 = (Integer) msg.obj;
                    if (count1 > 0) {
                        Message msg3 = obtainMessage();
                        msg3.what = Const.TEST_INFRA_RED;
                        msg3.obj = count1 - 1;
                        sendMessageDelayed(msg3, 1000);
                    }
                    if (count1 == 0) {
                        home_layout.setVisibility(View.GONE);
                        // 开启人脸比对线程
                        stratThread();
                        Infra_red = true;
                        bStop = false;

                        if (uithread == null) {
                            uithread = new UIThread();
                            uithread.start();
                        }
                    }
                    break;
                case Const.FLAG_SHOW_LOG://待机处理
                    int count4 = (Integer) msg.obj;
                    oneVsMoreView.setVisibility(View.GONE);
                    promptshow_xml.setVisibility(View.GONE);
                    pro_xml.setVisibility(View.GONE);
                    Infra_red = false;
                    if (count4 > 0) {
                        Message msgb = obtainMessage();
                        msgb.what = Const.FLAG_SHOW_LOG;
                        msgb.obj = count4 - 1;
                        sendMessageDelayed(msgb, 1000);
                    }
                    if (count4 == 0) {
                        home_layout.setVisibility(View.VISIBLE);
                        mCameraSurfView.releaseCamera();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * ACTIVITY周期
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 全屏代码
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        hideBottomUIMenu();
        initView();
        mContext = this;

        application = (MyApplication) getApplication();
        application.init();
        application.addActivity(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBottomUIMenu();

        if (uithread == null) {
            uithread = new UIThread();
            uithread.start();
        }

        if (mMyRedThread == null) {
            mMyRedThread = new MyRedThread();  //红外
            mMyRedThread.start();
        }
        mMyRedThread.startredThread();
        isOpenOneVsMore = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //关闭相机线程
        Infra_red = false;
        mCameraSurfView.releaseCamera();
        //关闭红外
        mMyRedThread.closeredThread();
        if (mMyRedThread != null) {
            mMyRedThread.interrupt();
            mMyRedThread = null;
        }
        //关闭人脸框线程
        if (faceDetectTask != null) {
            faceDetectTask.setRuning(false);
            faceDetectTask.cancel(false);
            faceDetectTask = null;
        }
        //关闭未播报完语音
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.release();
                mPlayer = null;
            }
        }
        isOpenOneVsMore = false;
        bStop = true;
    }

    @Override
    protected void onDestroy() {
        MyApplication.mFaceLibCore.UninitialAllEngine();
        super.onDestroy();
    }

    /**
     * 初始化视图控件
     */
    private void initView() {
        mCameraSurfView = (MyCameraSuf) findViewById(R.id.myCameraView);
        imageStack = mCameraSurfView.getImgStack();
        home_layout = (RelativeLayout) findViewById(R.id.home_layout);//待机界面

        // 提示框
        promptshow_xml = findViewById(R.id.promptshow_xml);
        loadprompt = promptshow_xml.findViewById(R.id.loadprompt);

        //1:N
        oneVsMoreView = findViewById(R.id.onevsmore);
        oneVsMore_face = oneVsMoreView.findViewById(R.id.onevsmore_face);
        oneVsMore_temper = oneVsMoreView.findViewById(R.id.onevsmore_temper);
        oneVsMore_userName = oneVsMoreView.findViewById(R.id.onevsmore_userName);
        oneVsMore_userID = oneVsMoreView.findViewById(R.id.onevsmore_userID);
        oneVsMore_userType = oneVsMoreView.findViewById(R.id.onevsmore_userType);

        //刷卡标记
        pro_xml = findViewById(R.id.pro);
    }

    /**
     * 开启画人脸框线程
     */
    private void stratThread() {
        if (faceDetectTask != null) {
            faceDetectTask.setRuning(false);
            faceDetectTask = null;
        }
        faceDetectTask = new FaceFramTask(mHandler, mCameraSurfView);
        faceDetectTask.setRuning(true);
        faceDetectTask.execute();
    }

    /**
     * 开启一个1：N的线程
     */
    private void openOneVsMoreThread(FaceInfo info) {
        if (!oneVsMoreThreadStauts && isOpenOneVsMore && Infra_red) {
            oneVsMoreThreadStauts = true;
            OneVsMoreThread thread = new OneVsMoreThread(info);
            thread.start();
        }
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    @SuppressLint("NewApi")
    protected void hideBottomUIMenu() {
        // 隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 提示显示框
     */
    private void ShowPromptMessage(String showmessage, int audionum) {
        if (audionum == 1) {
            playMusic(R.raw.burlcard);
        }
        if (audionum == 3) {
            playMusic(R.raw.blacklist);
        }
        loadprompt.setText(showmessage);
        promptshow_xml.setVisibility(View.VISIBLE);
        if (audionum != 2) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    promptshow_xml.setVisibility(View.GONE);
                }
            }, 1500);
        }
    }

    /**
     * 1vsn显示对比后成功是否窗口
     */
    private void showAlert() {
        if ((isOpenOneVsMore != false) || (Const.DELETETEMPLATE == false)) {
            if (AppData.getAppData().getCompareScore() <= SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE) && Const.ONE_VS_MORE_TIMEOUT_NUM >= Const.ONE_VS_MORE_TIMEOUT_MAXNUM) {
                if (promptshow_xml.getVisibility() != View.VISIBLE) {
                    Const.ONE_VS_MORE_TIMEOUT_NUM = 0;
                    ShowPromptMessage("请刷身份证", 1);
                }
            } else if (AppData.getAppData().getCompareScore() > SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE) && AppData.getAppData().getNFaceBmp() != null) {
                String sdCardDir = null;
                Const.ONE_VS_MORE_TIMEOUT_NUM = 0;
                String snapImageID = IDUtils.genImageName();
                oneVsMore_face.setImageBitmap(AppData.getAppData().getNFaceBmp());
                //保存抓拍照片，用来显示对比结果
                FileUtils.saveFile(AppData.getAppData().getNFaceBmp(), snapImageID, TestDate.DGetSysTime() + "_Face");
                User user = MyApplication.faceProvider.getUserByUserId(AppData.getAppData().getUser().getId());
                AppData.getAppData().setUser(user);
                if (user.getTemplateImageID() != null) {
                    sdCardDir = Environment.getExternalStorageDirectory() + "/FaceAndroid/FaceTemplate/" + user.getTemplateImageID() + ".jpg";
                }
                try {
                    if (sdCardDir != null) {
                        Bitmap bmp = BitmapFactory.decodeFile(sdCardDir);
                        AppData.getAppData().setCardBmp(bmp);
                        oneVsMore_temper.setImageBitmap(bmp);
                    }
                } catch (Exception e) {
                    oneVsMore_temper.setImageResource(R.mipmap.ic_launcher);
                }
                if (user.getType().equals("黑名单")) {
                    ShowPromptMessage("黑名单", 3);
                    return;
                }
                GPIOHelper.openDoor(true);
                //串口开门

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        GPIOHelper.openDoor(false);
                    }
                }, SPUtil.getInt(Const.KEY_OPENDOOR, Const.CLOSE_DOOR_TIME) * 1000);

                oneVsMore_userName.setText(user.getName());
                oneVsMore_userType.setText(user.getType());
                oneVsMore_userID.setText(user.getWordNo());
                com.runvision.core.LogToFile.e("1:N", "1:N成功: 姓名：" + user.getName() + ",分数：" + AppData.getAppData().getCompareScore());
                user.setTime(DateTimeUtils.getTime());
                Record record = new Record(AppData.getAppData().getCompareScore() + "", "成功", Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Face" + "/" + snapImageID, "1:N");
                user.setRecord(record);
                MyApplication.faceProvider.addRecord(user);

                oneVsMoreView.setVisibility(View.VISIBLE);
                playMusic(R.raw.success);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        oneVsMoreView.setVisibility(View.GONE);
                    }
                }, 1000);


                if (socketThread != null) {
                    SendData.sendComperMsgInfo(socketThread, true, Const.TYPE_ONEVSMORE);
                } else {
                    AppData.getAppData().clean();
                }
            } else if (AppData.getAppData().getCompareScore() != 0) {
                Const.ONE_VS_MORE_TIMEOUT_NUM++;
            }
        }
    }

    /**
     * 播放语音
     */
    public void playMusic(int msuicID) {
        if (!SPUtil.getBoolean(Const.KEY_ISOPENMUSIC, Const.OPEN_MUSIC)) {
            return;
        }
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.release();
            }
        }
        mPlayer = MediaPlayer.create(mContext, msuicID);
        mPlayer.start();
    }

    /**
     * 1：N比对操作线程
     */
    class OneVsMoreThread extends Thread {
        private FaceInfo info;
        AFR_FSDKFace face;
        AFR_FSDKMatching score;
        User user;

        public OneVsMoreThread(FaceInfo info) {
            this.info = info;
        }

        @Override
        public void run() {
            if (isOpenOneVsMore != false) {
                if (face == null) {
                    face = new AFR_FSDKFace();
                }
                int ret = MyApplication.mFaceLibCore.FaceFeature(info.getDes(), 480, 640, info.getFace().getRect(), info.getFace().getDegree(), face);
                if (ret == 0) {
                    AppData.getAppData().SetNFaceBmp(CameraHelp.getFaceImgByInfraredJpg(info.getFace().getRect().left, info.getFace().getRect().top, info.getFace().getRect().right, info.getFace().getRect().bottom, CameraHelp.getBitMap(info.getDes())));
                    float fenshu = SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE);
                    if (score == null) {
                        score = new AFR_FSDKMatching();
                    }
                    Log.i("GavinTest", "for前" + System.currentTimeMillis());
                    if (MyApplication.mList.size() > 0) {

                        Log.i("Gavin0903", "for");

                        for (Map.Entry<String, byte[]> entry : MyApplication.mList.entrySet()) {
                            if ((isOpenOneVsMore == false) || (Const.BATCH_IMPORT_TEMPLATE == true) || (Const.DELETETEMPLATE == true)) {
                                //  AppData.getAppData().setCompareScore(0);
                                continue;
                            }
                            String fileName = (String) entry.getKey();
                            byte[] mTemplate = (byte[]) entry.getValue();
                            AFR_FSDKFace face3 = new AFR_FSDKFace(mTemplate);
                            ret = MyApplication.mFaceLibCore.FacePairMatching(face3, face, score);
                            if (score.getScore() >= fenshu) {
                                if (user == null) {
                                    user = new User();
                                }
                                if (MyApplication.faceProvider.quaryUserTableRowCount("select count(id) from tUser") != 0) {
                                    if ((MyApplication.faceProvider.getUserByUserpath(fileName)) != null) {
                                        user.setId(MyApplication.faceProvider.getUserByUserpath(fileName).getId());
                                        AppData.getAppData().setUser(user);
                                    } else {

                                    }
                                }
                                fenshu = score.getScore();
                                continue;
                            }
                        }
                        Log.i("GavinTest", "for后" + System.currentTimeMillis());
                        Log.i("GavinTest", "fenshu:" + fenshu);
                        AppData.getAppData().setCompareScore(fenshu);
                    }
                } else {
                    AppData.getAppData().setCompareScore(0);
                }
                if (isOpenOneVsMore != false) {
                    Message msg = new Message();
                    msg.what = Const.COMPER_END;
                    mHandler.sendMessage(msg);
                }

                if (MyApplication.mList.size() < 1000) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    oneVsMoreThreadStauts = false;
                } else if (MyApplication.mList.size() < 3000) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    oneVsMoreThreadStauts = false;
                } else {
                    oneVsMoreThreadStauts = false;
                }
            }
        }

    }

    /**
     * 红外线程
     */
    private class MyRedThread extends Thread {

        public boolean redflag = false;

        @Override
        public void run() {
            super.run();
            while (true) {
                //Log.i("Gavin","redflag:" +redflag);
                // G68A设备红外接口不一样
                int status = GPIOHelper.readStatus();
                status = 1;
                if (redflag == true) {
                    try {
                        Thread.sleep(1500);
                        if (status == 1) {
                            mCameraSurfView.openCamera();
                            Message msg4 = new Message();
                            msg4.what = Const.TEST_INFRA_RED;
                            msg4.obj = 1;
                            mHandler.sendMessage(msg4);
                            logshowflag = 0;
                        }
                        if (status == 0) {
                            logshowflag++;
                            if (logshowflag == ((SPUtil.getInt(Const.KEY_BACKHOME, Const.CLOSE_HOME_TIMEOUT)) / 1.5)) {
                                logshowflag = 0;
                                Message msg4 = new Message();
                                msg4.what = Const.FLAG_SHOW_LOG;
                                msg4.obj = 2;
                                mHandler.sendMessage(msg4);
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }

        public void closeredThread() {
            this.redflag = false;
        }

        public void startredThread() {
            this.redflag = true;
        }

    }

    /**
     * 更新UI标志线程
     */
    private class UIThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(250);
                    Message msg = new Message();
                    msg.what = Const.UPDATE_UI;
                    mHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            default:
                break;
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

}