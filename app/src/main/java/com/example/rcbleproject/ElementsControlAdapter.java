package com.example.rcbleproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ElementsControlAdapter extends ArrayAdapter<BaseControlElement> {
    private final LayoutInflater inflater;
    private final int layout;
    private final List<BaseControlElement> elements;
    private final AddingElementControlActivity activity;

    public ElementsControlAdapter(AddingElementControlActivity context,
                                  int resource, List<BaseControlElement> elements){
        super(context, resource, elements);
        inflater = LayoutInflater.from(context);
        activity = context;
        this.elements = elements;
        layout = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final ViewHolder viewHolder;
        if (convertView == null){
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.position = position;
        BaseControlElement element = elements.get(position);
        viewHolder.tv_element_name.setText(element.getName());
        viewHolder.iv_element_image.setImageResource(element.getIcon());

        return convertView;
    }

    public int getElementType(int position){
        return elements.get(position).getType().ordinal();
    }

    private class ViewHolder{
        final TextView tv_element_name;
        final ImageView iv_element_image;
        int position;
        ViewHolder(View view){
            tv_element_name = view.findViewById(R.id.tv_element_name);
            iv_element_image = view.findViewById(R.id.iv_element_image);
        }
    }
}
