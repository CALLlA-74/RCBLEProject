package com.example.rcbleproject;

import static com.example.rcbleproject.Container.chosenProfControlPrefKey;
import static com.example.rcbleproject.Container.appPrefKey;
import static com.example.rcbleproject.Container.currDisIdPrefKey;
import static com.example.rcbleproject.Container.currDisIdxPrefKey;
import static com.example.rcbleproject.Container.numOfDisplaysPrefKey;
import static com.example.rcbleproject.Container.numOfElementsPrefKey;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

public class ProfilesActivity extends BaseAppActivity implements IRemovable {

    private ListView lvProfiles;
    private DatabaseAdapterProfilesControl dbAdapterProfilesControl;
    private DatabaseAdapterDisplays dbDisplays;
    private ProfilesAdapter lvAdapterProfilesControl;
    private int oldVisibleItem = 0;
    private ImageButton btAddProfile;
    private Button btAddFirstProfile;
    private boolean isReverse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);
        setSupportActionBar(findViewById(R.id.tb_activity_profiles));
        ((TextView)findViewById(R.id.tv_label)).setText(R.string.profiles_control);
        findViewById(R.id.bt_back).setVisibility(View.GONE);
        findViewById(R.id.bt_add_device).setVisibility(View.GONE);

        lvProfiles = findViewById(R.id.lv_profiles);

        dbAdapterProfilesControl = Container.getDbProfilesControl(this);
        dbDisplays = Container.getDbDisplays(this);
        Container.getDbForHubs(this);

        lvAdapterProfilesControl = new ProfilesAdapter(this,
                R.layout.app_list_item,
                dbAdapterProfilesControl, isReverse);
        lvProfiles.setAdapter(lvAdapterProfilesControl);
        btAddProfile = (ImageButton)findViewById(R.id.bt_add_profile);
        btAddProfile.setOnClickListener(v -> addProfile());

        ((TextView)findViewById(R.id.tv_msg_empty_list)).setText(R.string.empty_profiles_list);
        btAddFirstProfile = findViewById(R.id.bt_empty_list);
        btAddFirstProfile.setText(R.string.add_first_profile_control);
        btAddFirstProfile.setOnClickListener(v -> {
            findViewById(R.id.inc_empty_list_label).setVisibility(View.GONE);
            addProfile();
        });

        lvProfiles.setOnItemLongClickListener((parent, view, position, id) -> {
            lvAdapterProfilesControl.setFocusOnEditText(view);
            return true;
        });

        lvProfiles.setOnItemClickListener((parent, view, position, id) -> {
            if (lvAdapterProfilesControl.cancelEdit()) return;
            intoProfileControl(id);
        });

        lvProfiles.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (oldVisibleItem != firstVisibleItem) lvAdapterProfilesControl.cancelEdit();
                if (oldVisibleItem < firstVisibleItem)
                    btAddProfile.setVisibility(View.GONE);
                if (oldVisibleItem > firstVisibleItem)
                    btAddProfile.setVisibility(View.VISIBLE);
                oldVisibleItem = firstVisibleItem;
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (lvAdapterProfilesControl.getCount() <= 0)
            findViewById(R.id.inc_empty_list_label).setVisibility(View.VISIBLE);
        else findViewById(R.id.inc_empty_list_label).setVisibility(View.GONE);
    }

    private void addProfile(){
        lvAdapterProfilesControl.cancelEdit();
        long id = lvAdapterProfilesControl.addProfile(getResources().getString(R.string.default_profile_name));
        dbDisplays.insert(id, 0);
        /*int pos = lvAdapterProfilesControl.getPosition(profileControl);
        View view = lvAdapterProfilesControl.getView(pos, null, lvProfiles); */
    }

    @SuppressLint("ApplySharedPref")
    public void intoProfileControl(long profileId){
        if (BuildConfig.DEBUG){
            Log.v("APP_TAG", "size: " + lvAdapterProfilesControl.getCount());
            Log.v("APP_TAG", "click. id = " + profileId);
        }
        Intent intent = new Intent(getBaseContext(), ProfileControlActivity.class);
        SharedPreferences preferences = getSharedPreferences(appPrefKey, Context.MODE_PRIVATE);
        preferences.edit().putLong(chosenProfControlPrefKey, profileId).commit();
        startActivity(intent);
    }

    public void setImageButtonVisibility(int visibility){
        if (btAddProfile.getVisibility() != visibility) btAddProfile.setVisibility(visibility);
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    public void remove(long id){
        if (lvAdapterProfilesControl.getCount() <= 1)
            findViewById(R.id.inc_empty_list_label).setVisibility(View.VISIBLE);
        lvAdapterProfilesControl.removeProfile(id);
        SharedPreferences preferences = getSharedPreferences(appPrefKey, Context.MODE_PRIVATE);
        preferences.edit().remove(currDisIdxPrefKey +id)
                .remove(currDisIdPrefKey +id)
                .remove(numOfElementsPrefKey +id)
                .remove(numOfDisplaysPrefKey +id).commit();
    }

    @Override
    public void cancel(){}
}

