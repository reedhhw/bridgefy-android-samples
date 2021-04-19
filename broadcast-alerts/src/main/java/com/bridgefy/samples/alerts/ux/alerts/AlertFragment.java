package com.bridgefy.samples.alerts.ux.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bridgefy.samples.alerts.R;
import com.bridgefy.samples.alerts.databinding.FragmentDeviceListBinding;
import com.bridgefy.samples.alerts.model.Alert;
import com.bridgefy.samples.alerts.ux.main.MainViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class AlertFragment extends Fragment {

    private AlertViewModel alertViewModel;
    private FragmentDeviceListBinding binding;
    private AlertFragmentArgs args;

    private static final String ARG_ALERTS = "alerts";
    private AlertAdapter alertsAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlertFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = AlertFragmentArgs.fromBundle(getArguments());
        alertsAdapter = new AlertAdapter(args.getAlertsArrayList());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        alertViewModel = new ViewModelProvider(this).get(AlertViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding =  DataBindingUtil.inflate(inflater, R.layout.fragment_device_list, container, false);
        binding.list.setAdapter(alertsAdapter);
        return binding.getRoot();
    }
}
