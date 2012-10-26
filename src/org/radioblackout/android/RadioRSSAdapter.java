package org.radioblackout.android;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import org.mcsoxford.rss.*;


class RadioRSSAdapter extends ArrayAdapter {
    private Context mContext;
    public RadioRSSAdapter(Context context, List<RSSItem> items) {
        super(
            context,
            android.R.layout.simple_list_item_1,
            items
            );

        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.rss_list_item, null);

        RSSItem item = (RSSItem)getItem(position);

        TextView titleView = (TextView)view.findViewById(R.id.item_title);
        titleView.setText(item.getTitle());

        TextView dateView = (TextView)view.findViewById(R.id.item_date);
        dateView.setText(item.getPubDate().toString());

        TextView categoryView = (TextView)view.findViewById(R.id.item_category);
        List<String> categories = item.getCategories();

        String categoryText = "";
        int size = categories.size();
        for (int cycle = 0 ; cycle < size ; cycle++) {
            categoryText += categories.get(cycle);
            if ((cycle + 1) != size)
                categoryText += ", ";
        }
        categoryView.setText(categoryText);

        return view;
    }
}
