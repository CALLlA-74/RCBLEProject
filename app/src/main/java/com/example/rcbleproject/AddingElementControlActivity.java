package com.example.rcbleproject;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.databinding.ActivityAddingElementControlBinding;

import java.util.ArrayList;
import java.util.List;

public class AddingElementControlActivity extends BaseAppActivity {
    private ActivityAddingElementControlBinding binding;
    private long displayID;
    private DisplayMetrics displayMetrics;
    private DatabaseAdapterElementsControl dbAdapterElementsControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayID = getIntent().getLongExtra("display_id", -1);
        int elementNumber = getIntent().getIntExtra("count_of_elements", -1);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        binding = ActivityAddingElementControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tbActivityAddElement.tvLabel.setText(getString(R.string.elements_control));
        binding.tbActivityAddElement.btAddDevice.setVisibility(View.GONE);
        List<BaseControlElement> list = initElementsControlList();
        dbAdapterElementsControl = Container.getDbElementsControl(this);
        ElementsControlAdapter adapter = new ElementsControlAdapter(this,
                                                                        R.layout.item_element_control,
                                                                        list);
        binding.lvElementsControl.setAdapter(adapter);
        binding.tbActivityAddElement.btBack.setOnClickListener(v -> finish());
        binding.lvElementsControl.setOnItemClickListener((parent, view, position, id) -> {
            dbAdapterElementsControl.insert(elementNumber, displayID,
                                             adapter.getElementType(position),
                                            2, 2,
                                            (float) displayMetrics.widthPixels/2,
                                            (float) displayMetrics.heightPixels/2);
            finish();
        });
    }

    protected List<BaseControlElement> initElementsControlList(){
        return BaseControlElement.getAllDefaultElementControlTypes(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        setFullscreenMode(binding.llContentAddElement);
    }
}