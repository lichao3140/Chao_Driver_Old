package com.runvision.adapter;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.runvision.bean.Sign;
import com.runvision.g68a_sn.R;
import com.runvision.utils.TimeUtils;

import cn.lemon.view.adapter.BaseViewHolder;

public class CardRecordHolder extends BaseViewHolder<Sign> {

    private ImageView image;
    private TextView type;
    private TextView name;
    private TextView sex;
    private TextView cardNo;
    private TextView time;

    private TextView name_type, name_name, name_sex, name_card_no, name_time;

    public CardRecordHolder(ViewGroup parent) {
        super(parent, R.layout.holder_consume);
    }

    @Override
    public void setData(final Sign object) {
        super.setData(object);
        name_type.setText("打卡类型:");
        name_name.setText("姓名:");
        name_sex.setText("性别:");
        name_card_no.setText("身份证号:");
        name_time.setText("打卡时间:");

        image.setImageBitmap(object.getImageId());
        name.setText(object.getName());
        type.setText("签到");
        sex.setText(object.getGender());
        cardNo.setText(object.getCardNo());
        time.setText(TimeUtils.getYearMonth() + "\t" + object.getSigntime());
    }

    @Override
    public void onInitializeView() {
        super.onInitializeView();
        image = findViewById(R.id.iv_sign_image);
        type = findViewById(R.id.tv_sign_type);
        name = findViewById(R.id.tv_sign_name);
        sex = findViewById(R.id.tv_sign_sex);
        cardNo = findViewById(R.id.tv_sign_card_no);
        time = findViewById(R.id.tv_sign_time);

        name_type = findViewById(R.id.name_type);
        name_name = findViewById(R.id.name_name);
        name_sex = findViewById(R.id.name_sex);
        name_card_no = findViewById(R.id.name_card_no);
        name_time = findViewById(R.id.name_time);
    }

    @Override
    public void onItemViewClick(Sign object) {
        super.onItemViewClick(object);
        //点击事件
        Log.i("CardRecordHolder","onItemViewClick");
    }
}
