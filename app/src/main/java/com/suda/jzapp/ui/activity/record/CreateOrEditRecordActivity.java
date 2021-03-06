package com.suda.jzapp.ui.activity.record;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.suda.jzapp.BaseActivity;
import com.suda.jzapp.R;
import com.suda.jzapp.dao.greendao.Account;
import com.suda.jzapp.dao.greendao.Record;
import com.suda.jzapp.dao.greendao.RecordType;
import com.suda.jzapp.dao.greendao.RemarkTip;
import com.suda.jzapp.manager.AccountManager;
import com.suda.jzapp.manager.RecordManager;
import com.suda.jzapp.misc.Constant;
import com.suda.jzapp.misc.IntentConstant;
import com.suda.jzapp.ui.activity.account.SelectAccountActivity;
import com.suda.jzapp.ui.activity.system.SettingsActivity;
import com.suda.jzapp.ui.adapter.RecordTypeAdapter;
import com.suda.jzapp.util.IconTypeUtil;
import com.suda.jzapp.util.KeyBoardUtils;
import com.suda.jzapp.util.MoneyUtil;
import com.suda.jzapp.util.SPUtils;
import com.suda.jzapp.util.SnackBarUtil;
import com.suda.jzapp.util.ThemeUtil;
import com.suda.jzapp.view.MyCircleRectangleTextView;
import com.suda.jzapp.view.draggrid.DragGridView;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.drakeet.materialdialog.MaterialDialog;


public class CreateOrEditRecordActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMyContentView(false, R.layout.activity_create_or_edit_record);
        recordManager = new RecordManager(this);
        accountManager = new AccountManager(this);

        useVibrator = (boolean) SPUtils.get(this, true, SettingsActivity.VIBRATOR_SETTINGS, true);
        if (useVibrator)
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initWidget();

        recordTypes = new ArrayList<>();

        oldRecord = (Record) getIntent().getSerializableExtra(IntentConstant.OLD_RECORD);

        voiceRecord = (Record) getIntent().getSerializableExtra(IntentConstant.VOICE_RECORD);

        newRecord = new Record();

        if (oldRecord == null) {
            if (voiceRecord != null) {
                zhiChu = voiceRecord.getRecordType() == Constant.RecordType.ZUICHU.getId();
                tvMoneyCount.setText(MoneyUtil.getFormatMoneyStr(this, voiceRecord.getRecordMoney()));
                mCurRecordType = (RecordType) getIntent().getSerializableExtra(IntentConstant.VOICE_RECORD_TYPE);
                tvTypeTitle.setText(mCurRecordType.getRecordDesc());
                typeIcon.setImageResource(IconTypeUtil.getTypeIcon(mCurRecordType.getRecordIcon()));
            }

            resetRecordTypeList();
            if (voiceRecord == null) {
                setCurRecordType(0);
            }
            setRecordDate(Calendar.getInstance().getTime());
        } else {
            mAccountTv.setText(accountManager.getAccountByID(oldRecord.getAccountID()).getAccountName());
            newRecord.setId(oldRecord.getId());
            newRecord.setRecordId(oldRecord.getRecordId());
            newRecord.setAccountID(oldRecord.getAccountID());
            newRecord.setRecordTypeID(oldRecord.getRecordTypeID());
            newRecord.setRecordMoney(oldRecord.getRecordMoney());
            newRecord.setRemark(oldRecord.getRemark());
            newRecord.setObjectID(oldRecord.getObjectID());
            setRecordDate(oldRecord.getRecordDate());
            mDateTv.setText(fmDate(newRecord.getRecordDate()));
            mOldRecordType = recordManager.getRecordTypeByID(oldRecord.getRecordTypeID());
            setCurRecordType(0, mOldRecordType);
            tvMoneyCount.setText(String.format(getResources().getString(R.string.record_money_format), Math.abs(oldRecord.getRecordMoney())));
            zhiChu = oldRecord.getRecordType() < 0;
            etRemark.setText(oldRecord.getRemark());
            resetRecordTypeList();
        }

        recordTypeAdapter = new RecordTypeAdapter(this, recordTypes, mRecordDr);

        mRecordDr.setAdapter(recordTypeAdapter);

        mRecordDr.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (recordTypeAdapter.ismShake()) {
                    recordTypeAdapter.setShake(false);
                    return;
                }

                if (position == recordTypes.size() - 1) {
                    Intent intent = new Intent(CreateOrEditRecordActivity.this, CreateNewRecordTypeActivity.class);
                    intent.putExtra(IntentConstant.RECORD_TYPE, recordTypes.get(0).getRecordType());
                    startActivityForResult(intent, REQUEST_CODE_ADD_NEW_RECORD_TYPE);
                } else {
                    int startLocations[] = new int[2];
                    int endLocations[] = new int[2];
                    ImageView icon = (ImageView) view.findViewById(R.id.record_icon);
                    icon.getLocationInWindow(startLocations);
                    typeIcon.getLocationInWindow(endLocations);
                    createTranslationAnimations(position, icon, startLocations[0],
                            endLocations[0], startLocations[1], endLocations[1]);
                }
            }
        });

        mRecordDr.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == recordTypes.size() - 1)
                    return false;
                if (!recordTypeAdapter.ismShake()) {
                    recordTypeAdapter.setShake(true);
                }
                return false;
            }
        });

        mRecordDr.setOnMoveListener(new DragGridView.onMoveListener() {
            @Override
            public void onBottom() {
                if (showPanel)
                    return;
                YoYo.with(Techniques.SlideInUp).duration(500).playOn(panel);
                showPanel = true;
            }

            @Override
            public void onTop() {
                if (!showPanel)
                    return;
                YoYo.with(Techniques.SlideOutDown).duration(500).playOn(panel);
                showPanel = false;
            }

            @Override
            public void onRight() {

            }

            @Override
            public void onLeft() {

            }
        });

        mDateTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(newRecord.getRecordDate());
                DatePickerDialog tpd = DatePickerDialog.newInstance(
                        CreateOrEditRecordActivity.this,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                tpd.setAccentColor(getResources().getColor(getMainTheme().getMainColorID()));
                tpd.show(getFragmentManager(), "Timepickerdialog");
                tpd.setMaxDate(Calendar.getInstance());
            }
        });
    }

    private void createTranslationAnimations(final int pos, View view, float startX,
                                             float endX, float startY, float endY) {
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(true);
        final Bitmap cache = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        mMoveImage.setImageBitmap(cache);
        ImageView icon = (ImageView) view.findViewById(R.id.record_icon);
        Animation translateAnimation = new TranslateAnimation(startX,
                endX, startY - icon.getWidth() / 2, endY - icon.getWidth() / 2);
        translateAnimation.setDuration(250);
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mMoveImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMoveImage.setImageBitmap(null);
                if (!cache.isRecycled())
                    cache.recycle();
                mMoveImage.setVisibility(View.GONE);
                setCurRecordType(pos);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mMoveImage.startAnimation(translateAnimation);
    }

    @Override
    protected void initWidget() {
        mMoveImage = (ImageView) findViewById(R.id.move_image);
        mRecordDr = (DragGridView) findViewById(R.id.record_item);
        panelBackView = findViewById(R.id.panel_color);
        panel = findViewById(R.id.panel);
        tvMoneyCount = (TextView) findViewById(R.id.money_count);
        tvTypeTitle = (TextView) findViewById(R.id.record_title);
        typeIcon = (ImageView) findViewById(R.id.record_icon);
        btZhiChu = (Button) findViewById(R.id.zhi_chu);
        btShouRu = (Button) findViewById(R.id.shou_ru);
        mAccountTv = (TextView) findViewById(R.id.account);
        mDateTv = (TextView) findViewById(R.id.date);
        circleProgressBar = (CircleProgressBar) findViewById(R.id.progressBar);
        circleProgressBar.setVisibility(View.INVISIBLE);
        remarkPanel = findViewById(R.id.remark_panel);
        remarkBt = (Button) findViewById(R.id.remark_bt);
        remarkSaveBt = (Button) findViewById(R.id.remark_save);
        etRemark = (EditText) findViewById(R.id.edit_remark);
        mRemarkTipsFlow = (FlowLayout) findViewById(R.id.remark_tips);

        mAccountTv.setTextColor(getResources().getColor(getMainTheme().getMainColorID()));
        mDateTv.setTextColor(getResources().getColor(getMainTheme().getMainColorID()));
        remarkBt.setTextColor(getResources().getColor(getMainTheme().getMainColorID()));
        remarkSaveBt.setTextColor(getResources().getColor(getMainTheme().getMainColorID()));
        ((TextView) findViewById(R.id.remark_tips2)).setTextColor(getResources().getColor(getMainTheme().getMainColorID()));
        findViewById(R.id.line3).setBackgroundColor(getResources().getColor(getMainTheme().getMainColorID()));
        initRemarkPanel();

        tvMoneyCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showPanel)
                    return;
                YoYo.with(Techniques.SlideInUp).duration(500).playOn(panel);
                showPanel = true;
            }
        });
    }

    private void initRemarkPanel() {

        recordManager.getRemarkTips(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                List<RemarkTip> remarkTips = ((List<RemarkTip>) msg.obj);
                for (final RemarkTip tip : remarkTips) {
                    final View view = View.inflate(CreateOrEditRecordActivity.this, R.layout.remark_item, null);
                    MyCircleRectangleTextView myCircleRectangleTextView = (MyCircleRectangleTextView) view.findViewById(R.id.remark);
                    myCircleRectangleTextView.setText(tip.getRemark());
                    myCircleRectangleTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            etRemark.setText(tip.getRemark());
                            newRecord.setRemark(tip.getRemark());
                            hideRemarkPanel();
                        }
                    });

                    myCircleRectangleTextView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {

                            final MaterialDialog materialDialog = new MaterialDialog(CreateOrEditRecordActivity.this);
                            materialDialog.setTitle(String.format(getString(R.string.delete_remark),tip.getRemark()))
                                    .setMessage("")
                                    .setPositiveButton(getString(R.string.ok), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            mRemarkTipsFlow.removeView(view);
                                            recordManager.deleteRemarkTip(tip.getId());
                                            materialDialog.dismiss();
                                         }
                                    })
                                    .setNegativeButton(getString(R.string.cancel), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            materialDialog.dismiss();
                                        }
                                    }).show();
                            return true;
                        }
                    });
                    view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    mRemarkTipsFlow.addView(view);
                }
            }
        });

        etRemark.setDrawingCacheBackgroundColor(getResources().getColor(getMainTheme().getMainColorID()));

        remarkBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (remarkPanel.getVisibility() == View.INVISIBLE) {
                    remarkPanel.setVisibility(View.VISIBLE);
                    YoYo.with(Techniques.SlideInDown).duration(300).playOn(remarkPanel);
                } else {
                    hideRemarkPanel();
                }
            }
        });

        remarkSaveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newRecord.setRemark(etRemark.getText().toString());
                hideRemarkPanel();
            }
        });

        //触摸事件不继续传递
        remarkPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void hideRemarkPanel() {
        KeyBoardUtils.closeKeybord(etRemark, CreateOrEditRecordActivity.this);
        YoYo.with(Techniques.SlideOutUp).duration(300).playOn(remarkPanel);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                remarkPanel.setVisibility(View.INVISIBLE);
            }
        }, 300);
    }


    private void setCurRecordType(int index) {
        setCurRecordType(index, null);
    }

    private void setCurRecordType(int index, RecordType recordType) {
        if (recordType == null) {
            mCurRecordType = recordTypes.get(index);
        } else {
            mCurRecordType = recordType;
        }
        if (recordType == null && !userSelect) {
            Account account = accountManager.getSuitAccount(mCurRecordType.getRecordTypeID());
            newRecord.setAccountID(account.getAccountID());
            mAccountTv.setText(account.getAccountName());
        }
        if (recordTypes != null) {
            tvTypeTitle.setText(mCurRecordType.getRecordDesc());
            typeIcon.setImageResource(IconTypeUtil.getTypeIcon(mCurRecordType.getRecordIcon()));
        }
    }

    private void resetRecordTypeList() {
        recordTypes.clear();
        int type = zhiChu ? Constant.RecordType.ZUICHU.getId() : Constant.RecordType.SHOURU.getId();
        recordTypes.addAll(recordManager.getRecordTypeByType(type));
        RecordType recordType = new RecordType();
        recordType.setRecordType(type);
        recordTypes.add(recordType);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mainColor = this.getResources().getColor(ThemeUtil.getTheme(this).getMainColorID());
        mainDarkColor = this.getResources().getColor(ThemeUtil.getTheme(this).getMainDarkColorID());
        panelBackView.setBackgroundDrawable(new ColorDrawable(mainColor));
        tvTypeTitle.setTextColor(mainDarkColor);
        tvMoneyCount.setTextColor(mainDarkColor);
        setBtColor();

    }

    private void setBtColor() {
        if (zhiChu) {
            btZhiChu.setTextColor(mainDarkColor);
            btShouRu.setTextColor(Color.BLACK);
        } else {
            btZhiChu.setTextColor(Color.BLACK);
            btShouRu.setTextColor(mainDarkColor);
        }
    }

    public void switchZhiChuOrShouRu(View view) {
        zhiChu = !zhiChu;
        setBtColor();
        YoYo.with(Techniques.SlideOutLeft).duration(200).playOn(mRecordDr);
        resetRecordTypeList();
        YoYo.with(Techniques.SlideInRight).delay(200).duration(200).playOn(mRecordDr);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recordTypeAdapter.notifyDataSetChanged();
                setCurRecordType(0);
            }
        }, 200);
    }

    public void selectAccount(View view) {
        Intent intent = new Intent(this, SelectAccountActivity.class);
        intent.putExtra(IntentConstant.ACCOUNT_ID, newRecord.getAccountID());
        startActivityForResult(intent, REQUEST_CODE_ACCOUNT);
        overridePendingTransition(R.anim.up_in, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_ACCOUNT:
                    userSelect = true;
                    newRecord.setAccountID(data.getLongExtra(IntentConstant.ACCOUNT_ID, 0));
                    mAccountTv.setText(accountManager.getAccountByID(newRecord.getAccountID()).getAccountName());
                    break;
                case REQUEST_CODE_ADD_NEW_RECORD_TYPE:
                    resetRecordTypeList();
                    recordTypeAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recordTypeAdapter.ismShake()) {
                recordTypeAdapter.setShake(false);
                return true;
            }
            if (remarkPanel.getVisibility() == View.VISIBLE) {
                YoYo.with(Techniques.SlideOutUp).duration(300).playOn(remarkPanel);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        remarkPanel.setVisibility(View.INVISIBLE);
                    }
                }, 300);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveAndExit() {
        saving = true;
        circleProgressBar.setVisibility(View.VISIBLE);
        circleProgressBar.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light);
        newRecord.setRecordMoney(Double.parseDouble(tvMoneyCount.getText().toString())
                * (mCurRecordType.getRecordType() / Math.abs(mCurRecordType.getRecordType())));
        newRecord.setRecordTypeID(mCurRecordType.getRecordTypeID());
        newRecord.setRecordType(mCurRecordType.getRecordType());
        if (newRecord.getAccountID() == null) {
            SnackBarUtil.showSnackInfo(panel, this, "请重新选择账户");
            return;
        }
        if (oldRecord == null) {
            newRecord.setRecordId(System.currentTimeMillis());
            recordManager.createNewRecord(newRecord, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    accountManager.updateAccountMoney(newRecord.getAccountID(), newRecord.getRecordMoney(), new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }
            });
        } else {
            //判断账户是否发生变化
            if (oldRecord.getAccountID() == newRecord.getAccountID()) {
                //账户 -oldRecord.getRecordMoney() + newRecord.getRecordMoney();
                double addMoney = -oldRecord.getRecordMoney() + newRecord.getRecordMoney();
                accountManager.updateAccountMoney(newRecord.getAccountID(), addMoney, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        newRecord.setIsDel(false);
                        recordManager.updateOldRecord(newRecord, new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    }
                });
            } else {
                //原账户 -oldRecord.getRecordMoney(), 新账户 + newRecord.getRecordMoney();
                accountManager.updateAccountMoney(oldRecord.getAccountID(), -oldRecord.getRecordMoney(), new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        accountManager.updateAccountMoney(newRecord.getAccountID(), newRecord.getRecordMoney(), new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                newRecord.setIsDel(false);
                                recordManager.updateOldRecord(newRecord, new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        super.handleMessage(msg);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DATE, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        setRecordDate(calendar.getTime());
    }

    private void setRecordDate(Date date) {
        newRecord.setRecordDate(date);
        mDateTv.setText(fmDate(date));
    }

    ////////////////////////////计算panel////////////////////////////////
    public void onClickPanel(View view) {
        if (useVibrator)
            mVibrator.vibrate(10); //震动一下
        if (recordTypeAdapter.ismShake()) {
            recordTypeAdapter.setShake(false);
        }

        Button button = (Button) view;
        String tag = button.getText().toString();
        String money = tvMoneyCount.getText().toString();
        Character s1 = money.charAt(money.length() - 1);
        Character s2 = money.charAt(money.length() - 2);
        if ("C".equals(tag)) {
            doNum = 0;
            opt = Opt.CLEAR;
            tvMoneyCount.setText("0.00");
            moneyCount = 0.00;
            tempCount = 0.00;
            isDO = false;
        } else if ("del".equals(tag)) {
            opt = Opt.DEL;
            if (doNum > 0)
                doNum--;

            if (!"0".equals(s1.toString())) {
                StringBuilder stringBuilder = new StringBuilder(money);
                stringBuilder.replace(money.length() - 1, money.length(), 0 + "");
                tvMoneyCount.setText(stringBuilder.toString());
                tempCount = Double.parseDouble(money);
                isDO = true;
                return;
            }
            if (!"0".equals(s2.toString())) {
                StringBuilder stringBuilder = new StringBuilder(money);
                stringBuilder.replace(money.length() - 2, money.length(), 0 + "");
                stringBuilder.append("0");
                tvMoneyCount.setText(stringBuilder.toString());
                tempCount = Double.parseDouble(money);
                return;
            }
            if (!"0".equals(money.substring(0, money.length() - 3))) {
                long tmp = Long.parseLong(money.substring(0, money.length() - 3));
                tmp = tmp / 10;
                //moneyCount = tmp;
                tempCount = Double.parseDouble(money);
                tvMoneyCount.setText(tmp + ".00");
            }
            isDO = false;

        } else if ("+".equals(tag)) {
            doNum = 0;
            isDO = false;
            if (tempCount == 0 && Double.parseDouble(money) > 0) {
                opt = Opt.PLUS;
                return;
            }
            tempCount = 0;
            ((Button) findViewById(R.id.ok)).setText("=");

            if (moneyCount == 0) {
                moneyCount = Double.parseDouble(money);
                opt = Opt.PLUS;
                return;
            }

            if (opt == Opt.MINUS) {
                moneyCount = moneyCount - Double.parseDouble(money);
            } else {
                moneyCount = moneyCount + Double.parseDouble(money);
            }

            opt = Opt.PLUS;
            if (moneyCount <= Constant.MAX) {
                tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, moneyCount));
            } else {
                tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, Constant.MAX));
            }
            tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, moneyCount));
        } else if ("-".equals(tag)) {
            doNum = 0;
            isDO = false;
            if (tempCount == 0 && Double.parseDouble(money) > 0) {
                opt = Opt.MINUS;
                return;
            }

            tempCount = 0;

            ((Button) findViewById(R.id.ok)).setText("=");

            if (moneyCount == 0) {
                moneyCount = Double.parseDouble(money);
                opt = Opt.MINUS;
                return;
            }

            if (opt == Opt.PLUS) {
                moneyCount = moneyCount + Double.parseDouble(money);
            } else {
                moneyCount = moneyCount - Double.parseDouble(money);
            }

            opt = Opt.MINUS;
            tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, moneyCount));
        } else if ("OK".equals(tag)) {
            isDO = false;
            doNum = 0;
            opt = Opt.OK;
            tempCount = 0;
            if (Double.parseDouble(money) <= 0) {
                SnackBarUtil.showSnackInfo(view, this, "金额不能小于或等于0");
                return;
            }

            if (saving)
                return;

            saveAndExit();

        } else if ("=".equals(tag)) {
            isDO = false;
            doNum = 0;
            if (opt == Opt.EQUAL)
                return;
            if (tempCount == 0) {
                opt = Opt.EQUAL;
                ((Button) findViewById(R.id.ok)).setText("OK");
                moneyCount = 0.00;
                tempCount = Double.parseDouble(money);
                return;
            }
            if (opt == Opt.MINUS) {
                moneyCount = moneyCount - Double.parseDouble(money);
            } else {
                moneyCount = moneyCount + Double.parseDouble(money);
            }
            if (moneyCount <= Constant.MAX) {
                tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, moneyCount));
            } else {
                tvMoneyCount.setText(MoneyUtil.getFormatNumStr(this, Constant.MAX));
            }
            opt = Opt.EQUAL;
            moneyCount = 0.00;
            tempCount = Double.parseDouble(money);
            ((Button) findViewById(R.id.ok)).setText("OK");
        } else {
            if (".".equals(tag)) {
                isDO = true;
                return;
            }

            if ((opt == Opt.PLUS || opt == Opt.MINUS || opt == Opt.EQUAL || opt == Opt.OK) && tempCount == 0) {
                money = "0.00";
            }

            if (isDO) {
                if (doNum == 2)
                    return;

                doNum++;
                money = money.substring(0, money.length() - (3 - doNum)) + tag;
                if (doNum == 1)
                    money = money + "0";

                tvMoneyCount.setText(money);
                tempCount = Double.parseDouble(money);
                return;
            }

            if ("0".equals(money.substring(0, money.length() - 3))) {
                money = tag + money.substring(money.length() - 3, money.length());
            } else {
                money = money.substring(0, money.length() - 3) + tag + money.substring(money.length() - 3, money.length());
            }
            tempCount = Double.parseDouble(money);
            if (tempCount <= Constant.MAX) {
                tvMoneyCount.setText(money);
            }

        }

    }

    private DragGridView mRecordDr;
    private View panelBackView, panel;

    RecordTypeAdapter recordTypeAdapter;
    ArrayList<RecordType> recordTypes;
    RecordManager recordManager;

    private int mainColor;
    private int mainDarkColor;

    private CircleProgressBar circleProgressBar;
    private TextView tvMoneyCount, tvTypeTitle;
    private ImageView typeIcon;
    private Button btZhiChu, btShouRu;
    private Boolean zhiChu = true;

    private RecordType mCurRecordType, mOldRecordType;
    private double moneyCount = 0.00;
    private double tempCount = 0.00;
    private int doNum = 0;

    private Opt opt = Opt.NULL;
    private boolean isDO = false;
    private boolean showPanel = true;
    private TextView mAccountTv, mDateTv;

    private Record newRecord, oldRecord;
    private AccountManager accountManager;

    private View remarkPanel;
    private Button remarkBt, remarkSaveBt;
    private EditText etRemark;

    private FlowLayout mRemarkTipsFlow;

    private Record voiceRecord;

    private boolean saving = false;

    private ImageView mMoveImage;

    private Vibrator mVibrator;
    private boolean useVibrator;

    public static final int REQUEST_CODE_ACCOUNT = 1;
    public static final int REQUEST_CODE_ADD_NEW_RECORD_TYPE = 2;

    private enum Opt {
        NULL, PLUS, MINUS, DEL, CLEAR, OK, EQUAL;
    }

    private boolean userSelect = false;

}
