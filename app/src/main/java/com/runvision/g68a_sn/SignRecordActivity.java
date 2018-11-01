package com.runvision.g68a_sn;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import com.runvision.adapter.CardRecordHolder;
import com.runvision.bean.DaoSession;
import com.runvision.bean.IDCard;
import com.runvision.bean.IDCardDao;
import com.runvision.bean.Sign;
import com.runvision.utils.CameraHelp;
import java.util.ArrayList;
import java.util.List;
import cn.lemon.view.RefreshRecyclerView;
import cn.lemon.view.adapter.MultiTypeAdapter;

/**
 * 签到记录
 * Created by ChaoLi on 2018/10/14 0014 - 19:46
 * Email: lichao3140@gmail.com
 * Version: v1.0
 */
public class SignRecordActivity extends AppCompatActivity {
    private String TAG = "SignRecordActivity";
    private RefreshRecyclerView mRecyclerView;
    private MultiTypeAdapter mAdapter;
    private int mPage = 0;

    private IDCardDao idCardDao;
    private List<IDCard> idCards;
    private List<Sign> signList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_record);

        DaoSession daoSession = MyApplication.getDaoSession();
        idCardDao = daoSession.getIDCardDao();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setSwipeRefreshColors(0xFF437845, 0xFFE44F98, 0xFF2FAC21);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MultiTypeAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addRefreshAction(() -> getData(true));
        mRecyclerView.addLoadMoreAction(() -> getData(false));
        mRecyclerView.addLoadMoreErrorAction(() -> getData(false));
        mRecyclerView.post(() -> {
            mRecyclerView.showSwipeRefresh();
            getData(true);
        });

        FloatingActionButton mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(v -> Log.i(TAG, "FloatingActionButton"));
    }

    public void getData(final boolean isRefresh) {
        if (isRefresh) {
            mPage = 1;
        } else {
            mPage++;
        }
        if (mPage == 3) {
            mAdapter.showLoadMoreError();
            return;
        }
        mRecyclerView.postDelayed(() -> {
            if (isRefresh) {
                mAdapter.clear();
                mRecyclerView.dismissSwipeRefresh();
            }
            mAdapter.addAll(CardRecordHolder.class, initData());
            if (mPage >= 5) {
                mAdapter.showNoMore();
            }
            if (isRefresh) {
                mRecyclerView.getRecyclerView().scrollToPosition(0);
            }
        }, 1000);
    }

    private List<Sign> initData() {
        idCards = new ArrayList<>();
        signList = new ArrayList<>();
        idCards = idCardDao.loadAll();
        for (int i = 0; i < idCards.size(); i++) {
            IDCard idCard = idCards.get(i);
            String idnum = idCard.getId_card().substring(0, 6) + "*********" + idCard.getId_card().substring(16, 18);
            Sign sd = new Sign(idCard.getName(),  CameraHelp.getSmallBitmap(idCard.getIdcardpic()), idCard.getGender(), idnum, idCard.getSign_in());
            signList.add(sd);
        }
        return signList;
    }
}
