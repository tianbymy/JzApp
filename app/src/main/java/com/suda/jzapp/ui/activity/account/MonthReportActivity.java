package com.suda.jzapp.ui.activity.account;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.suda.jzapp.BaseActivity;
import com.suda.jzapp.R;
import com.suda.jzapp.manager.AccountManager;
import com.suda.jzapp.manager.RecordManager;
import com.suda.jzapp.manager.domain.MonthReport;
import com.suda.jzapp.misc.Constant;
import com.suda.jzapp.util.MoneyUtil;
import com.suda.jzapp.util.SPUtils;

import java.util.Calendar;
import java.util.Locale;

import me.drakeet.materialdialog.MaterialDialog;

public class MonthReportActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMyContentView(R.layout.activity_month_report_acyivity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recordManager = new RecordManager(this);
        accountManager = new AccountManager(this);
        initWidget();
    }

    @Override
    protected void initWidget() {
        mTvBudget = (TextView) findViewById(R.id.budget);
        mTvBudgetTip = (TextView) findViewById(R.id.budget_tip);
        mTvInMoney = (TextView) findViewById(R.id.in_money_tv);
        mTvInMoneyTip = (TextView) findViewById(R.id.in_money_tip);
        mTvOutMoney = (TextView) findViewById(R.id.out_money_tv);
        mTvOutMoneyTip = (TextView) findViewById(R.id.out_money_tip);
        mTvAllMoney = (TextView) findViewById(R.id.all_money);
        mTipEditBudget = findViewById(R.id.tip_edit_budget);

        refresh();
        resetTipRound();

        mTvBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SPUtils.put(MonthReportActivity.this, Constant.SP_TIP_ROUND_EDIT_BUDGET, false);
                resetTipRound();
                final MaterialDialog materialDialog = new MaterialDialog(MonthReportActivity.this);
                materialDialog.setTitle("预算修改");
                final EditText editText = new EditText(MonthReportActivity.this);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setText(mTvBudget.getText().toString());
                editText.setFocusable(true);
                editText.requestFocus();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager inputManager =
                                (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(editText, 0);
                    }
                }, 200);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String tmp = s.toString();
                        if (tmp.contains(".")) {
                            String[] strs = tmp.split("\\.");
                            if (strs.length > 1 && strs[1].length() > 2) {
                                editText.setText(tmp.substring(0, tmp.length() - 1));
                                editText.setSelection(tmp.length() - 1);
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                materialDialog.setContentView(editText);
                materialDialog.setNegativeButton(getResources().getString(R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String money = editText.getText().toString();
                        if (TextUtils.isEmpty(money)) {
                            money = "0.00";
                        }
                        accountManager.updateBudget(Double.parseDouble(money));
                        refresh();
                        materialDialog.dismiss();
                    }
                });
                materialDialog.setCanceledOnTouchOutside(true);
                materialDialog.show();
            }
        });

    }

    private void refresh() {
        recordManager.getThisMonthReport(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                MonthReport monthReport = (MonthReport) msg.obj;
                mTvAllMoney.setText(MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, monthReport.getAllMoney()));
                //目前还剩20\n看来得收紧口袋喽
                String budgetTip = "目前还剩" + (MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, monthReport.getBudgetMoney() + monthReport.getOutMoney()));
                if (Math.abs(monthReport.getOutMoney()) / monthReport.getBudgetMoney() > 0.8 && Math.abs(monthReport.getOutMoney()) / monthReport.getBudgetMoney() < 1) {
                    budgetTip += "\n看来得勒紧裤腰带喽";
                    mTvBudget.setTextColor(Color.RED);
                    mTvBudgetTip.setTextColor(Color.RED);
                } else if (Math.abs(monthReport.getOutMoney()) / monthReport.getBudgetMoney() <= 0.8) {
                    budgetTip += "\n预算还很充足嘛";
                    mTvBudget.setTextColor(Color.BLACK);
                    mTvBudgetTip.setTextColor(getResources().getColor(R.color.gray_light));
                } else {
                    mTvBudget.setTextColor(Color.RED);
                    mTvBudgetTip.setTextColor(Color.RED);
                    budgetTip = "本月已超出预算";
                }
                //其中'吃饭'消费最多，共消费40。\n看来你是一个吃货呀
                String outMoneyTip = "平均每天消费" + MoneyUtil.getFormatMoneyStr(MonthReportActivity.this,
                        Math.abs(monthReport.getOutMoney() / getDayOfMonth()));
                if (!TextUtils.isEmpty(monthReport.getOutMaxType())) {
                    outMoneyTip += "\n其中'" + monthReport.getOutMaxType() + "'消费最多，共消费"
                            + MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, monthReport.getOutMaxMoney());
                }
                //加油，骚年
                String inMoneyTip = "";
                if (monthReport.getBudgetMoney() > monthReport.getInMoney()) {
                    inMoneyTip = "奋斗吧，骚年";
                } else {
                    inMoneyTip = "很不错嘛，继续加油";
                }
                mTvBudget.setText(MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, monthReport.getBudgetMoney()));
                mTvInMoney.setText(MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, monthReport.getInMoney()));
                mTvOutMoney.setText(MoneyUtil.getFormatMoneyStr(MonthReportActivity.this, Math.abs(monthReport.getOutMoney())));
                mTvBudgetTip.setText(budgetTip);
                mTvInMoneyTip.setText(inMoneyTip);
                mTvOutMoneyTip.setText(outMoneyTip);
            }
        });
    }

    private void resetTipRound() {
        boolean showEditBudgetTip = (boolean) SPUtils.get(this, Constant.SP_TIP_ROUND_EDIT_BUDGET, true);
        mTipEditBudget.setVisibility(showEditBudgetTip ? View.VISIBLE : View.GONE);
        if (showEditBudgetTip) {
            AnimatorSet mAnimatorSet = new AnimatorSet();
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mTipEditBudget, "alpha", 1, 0);
            objectAnimator.setRepeatMode(Animation.RESTART);
            objectAnimator.setRepeatCount(Integer.MAX_VALUE);
            objectAnimator.setDuration(1000);
            mAnimatorSet.playTogether(objectAnimator);
            mAnimatorSet.start();
        }
    }

    public static int getDayOfMonth() {
        Calendar aCalendar = Calendar.getInstance(Locale.CHINA);
        int day = aCalendar.get(Calendar.DATE);
        return day;
    }

    private TextView mTvBudget, mTvBudgetTip;
    private TextView mTvInMoney, mTvInMoneyTip;
    private TextView mTvOutMoney, mTvOutMoneyTip;
    private TextView mTvAllMoney;
    private View mTipEditBudget;

    private RecordManager recordManager;
    private AccountManager accountManager;

}
