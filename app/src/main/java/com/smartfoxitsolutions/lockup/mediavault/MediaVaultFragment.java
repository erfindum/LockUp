package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 01-10-2016.
 */

public class MediaVaultFragment extends Fragment {

    int mediaType;
    RecyclerView mediaVaultRecycler;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.vault_fragment,container,false);
        mediaVaultRecycler = (RecyclerView) parent.findViewById(R.id.vault_fragment_recycler);
        return parent;
    }

}
