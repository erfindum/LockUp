package com.smartfoxitsolutions.lockup;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockRecyclerViewHeader extends RecyclerView.ViewHolder {
    private View headerView;
    private TextView headerText;

    public AppLockRecyclerViewHeader(View itemView) {
        super(itemView);
        headerView = itemView;
        headerText = (TextView) headerView.findViewById(R.id.recycler_header_textView);
    }

    TextView getHeaderText(){
      return this.headerText;
    }

    View getHeaderView(){
        return this.headerView;
    }
}
