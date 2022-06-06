package com.example.rcbleproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

public class ProfilesActivity extends BaseAppActivity implements Removable {

    private ListView lvProfiles;
    private DatabaseAdapterProfilesControl dbAdapterProfilesControl;
    private DatabaseAdapterDisplays dbDisplays;
    private ProfilesAdapter lvAdapterProfilesControl;
    private int oldVisibleItem = 0;
    private ImageButton btAddProfile;
    private boolean isReverse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);
        setSupportActionBar(findViewById(R.id.tb_activity_profiles));
        ((TextView)findViewById(R.id.tv_label)).setText(R.string.profiles_control);
        findViewById(R.id.bt_back).setVisibility(View.GONE);
        findViewById(R.id.bt_add_device).setVisibility(View.GONE);

        /*findViewById(R.id.bt_back).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddingDevicesActivity.class);
            startActivity(intent);
            finish();
        });*/

        lvProfiles = findViewById(R.id.lv_profiles);

        dbAdapterProfilesControl = new DatabaseAdapterProfilesControl(this);
        dbAdapterProfilesControl.open();

        dbDisplays = new DatabaseAdapterDisplays(this);
        dbDisplays.open();

        lvAdapterProfilesControl = new ProfilesAdapter(this,
                R.layout.app_list_item,
                dbAdapterProfilesControl, isReverse);
        lvProfiles.setAdapter(lvAdapterProfilesControl);
        btAddProfile = (ImageButton)findViewById(R.id.bt_add_profile);
        btAddProfile.setOnClickListener(v -> {
            long id = lvAdapterProfilesControl.addProfile(getResources().getString(R.string.default_profile_name));
            dbDisplays.insert(id, 0);
            /*int pos = lvAdapterProfilesControl.getPosition(profileControl);
            View view = lvAdapterProfilesControl.getView(pos, null, lvProfiles);
            setFocusOnEditText(view, view.getId());*/
        });

        lvProfiles.setOnItemLongClickListener((parent, view, position, id) -> {
            lvAdapterProfilesControl.setFocusOnEditText(view);
            return true;
        });

        lvProfiles.setOnItemClickListener((parent, view, position, id) -> {
            if (BuildConfig.DEBUG){
                Log.v("APP_TAG", "size: " + lvAdapterProfilesControl.getCount());
                Log.v("APP_TAG", "click. id = " + id);
            }
            lvAdapterProfilesControl.cancelEdit();
            Intent intent = new Intent(this, ProfileControlActivity.class);
            intent.putExtra("profile_id", id);
            startActivity(intent);
            //finish();
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

    public void setImageButtonVisibility(int visibility){
        if (btAddProfile.getVisibility() != visibility) btAddProfile.setVisibility(visibility);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        dbAdapterProfilesControl.close();
        dbDisplays.close();
    }

    @Override
    public void remove(long id){
        lvAdapterProfilesControl.removeProfile(id);
    }

    @Override
    public void cancel(){}
}

