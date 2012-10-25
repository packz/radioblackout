package org.radioblackout.android;

import android.content.Context;
import android.widget.ArrayAdapter;
import java.util.List;

import org.mcsoxford.rss.*;


class RadioRSSAdapter extends ArrayAdapter {
    public RadioRSSAdapter(Context context, List<RSSItem> items) {
        super(
            context,
            android.R.layout.simple_list_item_1,
            items
            );
    }
}
