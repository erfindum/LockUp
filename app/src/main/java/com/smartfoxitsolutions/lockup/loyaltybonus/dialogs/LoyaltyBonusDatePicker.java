package com.smartfoxitsolutions.lockup.loyaltybonus.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusMain;

import java.util.Calendar;

/**
 * Created by RAAJA on 17-01-2017.
 */

public class LoyaltyBonusDatePicker extends DialogFragment  implements DatePickerDialog.OnDateSetListener{

    LoyaltyBonusMain activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Bundle argBundle = getArguments();
        int year,month,day;
        if(argBundle==null) {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
        }else{
            year = argBundle.getInt("year");
            month = argBundle.getInt("month");
            day = argBundle.getInt("date");
        }

        return new DatePickerDialog(getContext(),this,year,month,day);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyBonusMain) getActivity();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        setDate(year, monthOfYear+1, dayOfMonth);
    }

    void setDate(int year, int monthOfYear, int dayOfMonth){
        if(activity!=null){
            activity.setDate(year,monthOfYear,dayOfMonth);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}
