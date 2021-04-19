package com.bridgefy.samples.alerts.ux.main;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bridgefy.samples.alerts.R;
import com.bridgefy.samples.alerts.databinding.MainFragmentBinding;
import com.bridgefy.samples.alerts.model.Alert;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    private MainViewModel viewModel;
    private MainFragmentBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false);

        binding.deviceText.setText(Build.MANUFACTURER + " " + Build.MODEL);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
        binding.setViewModel(viewModel);
        // Required to update UI with LiveData
        binding.setLifecycleOwner(getViewLifecycleOwner());

        viewModel.getMessageMutableLiveData().observe(getViewLifecycleOwner(), message -> {
            viewModel.incomingMessageBroadcast(message);
        });

        viewModel.getBridgefyClientMutableLiveData().observe(getViewLifecycleOwner(), client -> {
            binding.deviceId.setText(client.getUserUuid());
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_alerts) {
            List<Alert> alertList = viewModel.getAlertsMutableLiveData().getValue();
            if (alertList == null)
                getNavigationController().navigate(MainFragmentDirections.actionMainToAlerts(new ArrayList()));
            else
                getNavigationController().navigate(MainFragmentDirections.actionMainToAlerts(new ArrayList(alertList)));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private NavController getNavigationController() {
        return Navigation.findNavController(binding.getRoot());
    }
}