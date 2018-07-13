package com.micronet_inc.smarthubsampleapp.fragments;

import static java.lang.Thread.sleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.micronet_inc.smarthubsampleapp.CanTest;
import com.micronet_inc.smarthubsampleapp.R;
import java.util.Calendar;
import java.util.Date;

public class Can1OverviewFragment extends Fragment {

    private final String TAG = "Can1OverviewFragment";
    private Date LastCreated;
    private Date LastClosed;

    private int BITRATE_250K = 250000;
    private int BITRATE_500K = 500000;
    private boolean silentMode = false;
    private boolean termination = false;
	private boolean filtersEnabled = false;
    private boolean flowControlEnabled = false;
    private int baudRateSelected = BITRATE_250K;

    private Thread updateUIThread;

    private CanTest canTest;
    private TextView txtInterfaceClsTimeCan1;
    private TextView txtInterfaceOpenTimeCan1;
    private TextView txtCanTxSpeedCan1;
    private TextView txtCanBaudRateCan1;

    private TextView textViewFramesRx;
    private TextView textViewFramesTx;

    // Socket dependent UI
    private Button btnTransmitCAN1;
    private ToggleButton swCycleTransmitJ1939Can1;
    private SeekBar seekBarJ1939SendCan1;

    //Interface dependent UI
    private ToggleButton toggleButtonTermCan1;
    private ToggleButton toggleButtonListenCan1;
    private RadioGroup baudRateCan1;
    private ToggleButton toggleButtonFilterSetCan1;
    private ToggleButton toggleButtonFlowControlCan1;

    private Button openCan1;
    private Button closeCan1;

    private ChangeBaudRateTask changeBaudRateTask;

    private int mDockState = -1;
    private boolean reopenCANOnTtyAttachEvent = false;

    public Can1OverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canTest = CanTest.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        IntentFilter filters = new IntentFilter();
        filters.addAction("com.micronet.smarthubsampleapp.dockevent");
        filters.addAction("com.micronet.smarthubsampleapp.portsattached");
        filters.addAction("com.micronet.smarthubsampleapp.portsdetached");

        Context context = getContext();
        if (context != null){
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, filters);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }
    }

    private void setStateSocketDependentUI() {
        boolean open = canTest.isPort1SocketOpen();
        btnTransmitCAN1.setEnabled(open);
        swCycleTransmitJ1939Can1.setEnabled(open);
        seekBarJ1939SendCan1.setEnabled(open);
    }

    private void setStateInterfaceDependentUI() {
        boolean open = canTest.isCan1InterfaceOpen();
        //btnGetBaudrateCam.setEnabled(open);

    }

    private void setDockStateDependentUI(){
        boolean uiElementEnabled = true;
        if (mDockState == Intent.EXTRA_DOCK_STATE_UNDOCKED){
            uiElementEnabled = false;
        }
        toggleButtonTermCan1.setEnabled(uiElementEnabled);
        toggleButtonListenCan1.setEnabled(uiElementEnabled);
        baudRateCan1.setEnabled(uiElementEnabled);
        toggleButtonFilterSetCan1.setEnabled(uiElementEnabled);
        toggleButtonFlowControlCan1.setEnabled(uiElementEnabled);
        openCan1.setEnabled(uiElementEnabled);
        closeCan1.setEnabled(uiElementEnabled);
    }

    private void updateInterfaceStatusUI(String status) {
        final TextView txtInterfaceStatus = getView().findViewById(R.id.textCan1InterfaceStatus);
        if(status != null) {
            txtInterfaceStatus.setText(status);
            txtInterfaceStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isCan1InterfaceOpen()) {
            txtInterfaceStatus.setText(getString(R.string.open));
            txtInterfaceStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtInterfaceStatus.setText(getString(R.string.closed));
            txtInterfaceStatus.setBackgroundColor(Color.RED);
        }

        final TextView txtSocketStatus = getView().findViewById(R.id.textCan1SocketStatus);
        if(status != null) {
            txtSocketStatus.setText(status);
            txtSocketStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isPort1SocketOpen()) {
            txtSocketStatus.setText(getString(R.string.open));
            txtSocketStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtSocketStatus.setText(getString(R.string.closed));
            txtSocketStatus.setBackgroundColor(Color.RED);
        }
    }

    private void updateInterfaceStatusUI() {
        updateInterfaceStatusUI(null);
    }

    @Override
    public void onStart() {
        super.onStart();

        final View rootView = getView();

        textViewFramesRx = rootView.findViewById(R.id.textViewCan1FramesRx);
        textViewFramesTx = rootView.findViewById(R.id.textViewCan1FramesTx);

        baudRateCan1 = rootView.findViewById(R.id.radioGrCan1BaudRates);
        toggleButtonListenCan1 = rootView.findViewById(R.id.toggleButtonCan1Listen);
        toggleButtonTermCan1 = rootView.findViewById(R.id.toggleButtonCan1Term);
		toggleButtonFilterSetCan1 = rootView.findViewById(R.id.toggleButtonCan1Filters);
        toggleButtonFlowControlCan1 = rootView.findViewById(R.id.toggleButtonCan1FlowControl);

        openCan1 = rootView.findViewById(R.id.buttonOpenCan1);
        closeCan1 = rootView.findViewById(R.id.buttonCloseCan1);
        txtInterfaceClsTimeCan1 = rootView.findViewById(R.id.textViewCan1ClosedTime);
        txtInterfaceOpenTimeCan1 = rootView.findViewById(R.id.textViewCan1CreatedTime);
        txtCanTxSpeedCan1 = rootView.findViewById(R.id.textViewCan1CurrTransmitInterval);
        txtCanBaudRateCan1 = rootView.findViewById(R.id.textViewCan1CurrBaudRate);

        btnTransmitCAN1 = rootView.findViewById(R.id.btnCan1SendJ1939);
        seekBarJ1939SendCan1 = rootView.findViewById(R.id.seekBarCan1SendSpeed);
        swCycleTransmitJ1939Can1 = rootView.findViewById(R.id.swCan1CycleTransmitJ1939);

        seekBarJ1939SendCan1.setProgress(canTest.getJ1939IntervalDelay());
        btnTransmitCAN1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canTest.sendJ1939Port1();
            }
        });

        baudRateCan1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radio250K:
                        baudRateSelected = BITRATE_250K;
                        break;
                    case R.id.radio500K:
                        baudRateSelected = BITRATE_500K;
                        break;
                }
            }
        });

        toggleButtonTermCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                termination = isChecked;
            }
        });


        toggleButtonListenCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                silentMode = isChecked;
            }
        });

		toggleButtonFilterSetCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filtersEnabled = isChecked;
            }
        });

        toggleButtonFlowControlCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flowControlEnabled = isChecked;
            }
        });

        openCan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCan1Interface();
            }
        });

        closeCan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCan1Interface();
            }
        });

        swCycleTransmitJ1939Can1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canTest.setAutoSendJ1939Port1(swCycleTransmitJ1939Can1.isChecked());
                canTest.sendJ1939Port1();
            }
        });

        seekBarJ1939SendCan1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    canTest.setJ1939IntervalDelay(progress);
                    txtCanTxSpeedCan1.setText(progress + "ms");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //txtCanTxSpeedCan1.setText(canTest.getJ1939IntervalDelay() + "ms");
        updateBaudRateUI();
        updateInterfaceTime();
        updateInterfaceStatusUI();
        setStateInterfaceDependentUI();
        setStateSocketDependentUI();
        setDockStateDependentUI();
    }

    private void openCan1Interface(){
        canTest.setRemoveCan1InterfaceState(false);
        canTest.setBaudrate(baudRateSelected);
        canTest.setPortNumber(2);
        canTest.setSilentMode(silentMode);
        canTest.setTermination(termination);
        canTest.setRemoveCan1InterfaceState(false);
        canTest.setFiltersEnabled(filtersEnabled);
        canTest.setFlowControlEnabled(flowControlEnabled);
        executeChangeBaudrate();
    }

    private void closeCan1Interface(){
        canTest.setRemoveCan1InterfaceState(true);
        executeChangeBaudrate();
    }

    private void
    executeChangeBaudrate() {
        if (changeBaudRateTask == null || changeBaudRateTask.getStatus() != AsyncTask.Status.RUNNING) {
            changeBaudRateTask = new ChangeBaudRateTask( silentMode , baudRateSelected, termination, canTest.getPortNumber(), filtersEnabled, flowControlEnabled);
            changeBaudRateTask.execute();
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void updateCountUI() {
        if (canTest != null){
            String s1 = canTest.getPort1CanbusRxFrameCount() + " Frames / " + canTest.getPort1CanbusRxByteCount() + " Bytes";
            swCycleTransmitJ1939Can1.setChecked(canTest.isAutoSendJ1939Port1());
            textViewFramesRx.setText(s1);
            if (canTest.getPort1CanbusRxFrameCount() == 0){
                textViewFramesRx.setBackgroundColor(Color.WHITE);
            }
            else{
                textViewFramesRx.setBackgroundColor(Color.GREEN);
            }

            String s2 = "Tx: " + canTest.getPort1CanbusTxFrameCount() + " Frames / " + canTest.getPort1CanbusTxByteCount() + " Bytes";
            textViewFramesTx.setText(s2);
            if (canTest.getPort1CanbusTxFrameCount() == 0) {
                textViewFramesTx.setBackgroundColor(Color.WHITE);
            }
            else{
                textViewFramesTx.setBackgroundColor(Color.GREEN);
            }
        }
    }

    private void updateBaudRateUI() {
        String baudrateDesc = getString(R.string._000k_desc);
        if (canTest.getBaudrate() == BITRATE_250K) {
            baudrateDesc = getString(R.string._250k_desc);
        } else if (canTest.getBaudrate() == BITRATE_500K) {
            baudrateDesc = getString(R.string._500k_desc);
        }
        txtCanBaudRateCan1.setText(baudrateDesc);
    }

    private void updateInterfaceTime() {
        String closedDate = " None ";
        String createdDate = " None ";
        if(LastClosed != null){
            closedDate = LastClosed.toString();
        }
        if(LastCreated != null){
            createdDate = LastCreated.toString();
        }

        txtInterfaceOpenTimeCan1.setText(createdDate);
        txtInterfaceClsTimeCan1.setText(closedDate);
    }


    private void startUpdateUIThread() {
        if (updateUIThread == null) {
            updateUIThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCountUI();
                            }
                        });
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (updateUIThread != null && !updateUIThread.isAlive()) {
            updateUIThread.start();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_can1_overview, container, false);
    }

    private class ChangeBaudRateTask extends AsyncTask<Void, String, Void> {

        int baudrate;
        boolean silent;
        boolean termination;
        int port;
        boolean removeInterface;
		boolean filtersEnabled;
        boolean flowControlEnabled;

        public ChangeBaudRateTask(boolean silent,int baudrate,boolean termination, int port, boolean filtersEnabled, boolean flowControlEnabled ) {
            this.baudrate = baudrate;
            this.silent = silent;
            this.termination=termination;
            this.port=port;
            this.removeInterface=canTest.getRemoveCan1InterfaceState();
            this.filtersEnabled = filtersEnabled;
            this.flowControlEnabled = flowControlEnabled;
        }

        @Override
        protected Void doInBackground(Void... params) {
            LastClosed = Calendar.getInstance().getTime();
            if(canTest.isCan1InterfaceOpen() || canTest.isPort1SocketOpen()) {
                publishProgress("Closing interface, please wait...");
                canTest.closeCan1Interface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCan1Socket();
            }
            if(removeInterface==true){
                return null;
            }

            publishProgress("Opening, please wait...");
            int ret = canTest.CreateCanInterface1(silent, baudrate, termination, port, filtersEnabled, flowControlEnabled);
            if (ret == 0) {
                LastCreated = Calendar.getInstance().getTime();
            }
            else{
                publishProgress("Closing interface, please wait...");
                canTest.closeCan1Interface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCan1Socket();
                publishProgress("failed");
            }
            return null;
        }

        protected void onProgressUpdate(String... params) {
            updateInterfaceStatusUI(params[0]);
            setStateSocketDependentUI();
            setStateInterfaceDependentUI();
            setDockStateDependentUI();
        }

        protected void onPostExecute(Void result) {
            updateBaudRateUI();
            updateInterfaceTime();
            startUpdateUIThread();
            updateInterfaceStatusUI();
            setStateInterfaceDependentUI();
            setStateSocketDependentUI();
            setDockStateDependentUI();
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "com.micronet.smarthubsampleapp.dockevent":
                        mDockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + mDockState);
                        break;
                    case "com.micronet.smarthubsampleapp.portsattached":
                        if (reopenCANOnTtyAttachEvent){
                            Log.d(TAG, "Reopening CAN1 port since the tty port attach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "Reopening CAN1 port since the tty port attach event was received",
                                    Toast.LENGTH_SHORT).show();
                            openCan1Interface();
                            reopenCANOnTtyAttachEvent = false;
                        }
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case "com.micronet.smarthubsampleapp.portsdetached":
                        if (canTest.isCan1InterfaceOpen()){
                            Log.d(TAG, "closing CAN1 port since the tty port detach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "closing CAN1 port since the tty port detach event was received",
                                    Toast.LENGTH_SHORT).show();
                            closeCan1Interface();
                            reopenCANOnTtyAttachEvent = true;
                        }
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    private void updateCradleIgnState(){
        String cradleStateMsg, ignitionStateMsg;
        Log.d(TAG, "updateCradleIgnState() mDockState:" + mDockState);
        switch (mDockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                //ignitionStateMsg = getString(R.string.ignition_off_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
        }

        TextView cradleStateTextview = (TextView) getView().findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextview = (TextView) getView().findViewById(R.id.textViewIgnitionState);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
    }
}
