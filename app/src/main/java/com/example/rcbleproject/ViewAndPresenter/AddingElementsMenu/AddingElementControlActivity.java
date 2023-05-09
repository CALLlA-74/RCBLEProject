package com.example.rcbleproject.ViewAndPresenter.AddingElementsMenu;

import static com.example.rcbleproject.Container.chosenProfControlPrefKey;
import static com.example.rcbleproject.Container.currDisIdPrefKey;
import static com.example.rcbleproject.Container.appPrefKey;
import static com.example.rcbleproject.Container.numOfElementsPrefKey;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import com.example.rcbleproject.Model.BaseControlElement;
import com.example.rcbleproject.R;
import com.example.rcbleproject.ViewAndPresenter.BaseAppActivity;
import com.example.rcbleproject.databinding.ActivityAddingElementControlBinding;

import java.util.List;

public class AddingElementControlActivity extends BaseAppActivity {
    private ActivityAddingElementControlBinding binding;
    private DisplayMetrics displayMetrics;
    private int elementNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(appPrefKey, MODE_PRIVATE);
        long profileId = preferences.getLong(chosenProfControlPrefKey, 0);
        long displayID = preferences.getLong(currDisIdPrefKey + profileId, -1);
        elementNumber = preferences.getInt(numOfElementsPrefKey+profileId, -1);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        binding = ActivityAddingElementControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tbActivityAddElement.tvLabel.setText(getString(R.string.elements_control));
        binding.tbActivityAddElement.btAddDevice.setVisibility(View.GONE);
        List<BaseControlElement> list = initElementsControlList(displayID);
        ElementsControlAdapter adapter = new ElementsControlAdapter(this,
                                                                        R.layout.item_element_control,
                                                                        list);
        binding.lvElementsControl.setAdapter(adapter);
        binding.tbActivityAddElement.btBack.setOnClickListener(v -> finish());
        binding.lvElementsControl.setOnItemClickListener((parent, view, position, id) -> {

        });
    }

    protected List<BaseControlElement> initElementsControlList(long displayID){
        return BaseControlElement.getAllDefaultElementControlTypes(this, displayID,
                elementNumber, (float) displayMetrics.widthPixels/2,
                (float) displayMetrics.heightPixels/2);
    }

    @Override
    protected void onResume(){
        super.onResume();
        setFullscreenMode(binding.llContentAddElement);
    }
}