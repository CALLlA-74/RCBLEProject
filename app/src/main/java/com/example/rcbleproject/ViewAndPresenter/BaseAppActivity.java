package com.example.rcbleproject.ViewAndPresenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rcbleproject.R;

public class BaseAppActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstance){ super.onCreate(savedInstance); }

    public void hideKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @SuppressLint("SetTextI18n")
    public void showCurrentDisplayNum(int currentDisplayNum, int countOfDisplays){
        ((TextView)findViewById(R.id.tv_num_display)).setText((currentDisplayNum + 1) + " / " + countOfDisplays);
    }

    protected void setFullscreenMode(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            v.getWindowInsetsController().hide(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    public void notifyDataSetChanged(){}
}
