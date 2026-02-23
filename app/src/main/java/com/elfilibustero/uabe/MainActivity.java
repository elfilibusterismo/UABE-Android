package com.elfilibustero.uabe;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.elfilibustero.uabe.databinding.ActivityMainBinding;
import com.elfilibustero.uabe.ui.BundleViewerFragment;
import com.elfilibustero.uabe.ui.BundleViewerViewModel;
import com.elfilibustero.uabe.util.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        BundleViewerViewModel viewModel = new ViewModelProvider(this).get(
                BundleViewerViewModel.class);

        ActivityMainBinding b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        Utils.addSystemWindowInsetToPadding(b.appbar, true, true, true, false);

        setSupportActionBar(b.toolbar);

        viewModel.getDisplayName().observe(this, b.toolbar::setSubtitle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new BundleViewerFragment())
                    .commit();
        }
    }
}
