package polar.com.androidblesdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarAccelerometerData;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarExerciseEntry;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarSensorSetting;

public class AccActivity extends AppCompatActivity {
    private final static String TAG = AccActivity.class.getSimpleName();
    PolarBleApi api;
    Disposable broadcastDisposable;
    Disposable ecgDisposable;
    Disposable accDisposable;
    Disposable ppgDisposable;
    Disposable ppiDisposable;
    Disposable scanDisposable;
    String DEVICE_ID = "6682EE20"; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id
    Disposable autoConnectDisposable;
    PolarExerciseEntry exerciseEntry;

    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;
    private StringBuilder datacsv;
    final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc);
        // Notice PolarBleApi.ALL_FEATURES are enabled
        api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.ALL_FEATURES);
        api.setPolarFilter(false);

        final Button autoConnect = this.findViewById(R.id.autoconnect_button_acc);
        final Button disconnect = this.findViewById(R.id.disconnect_button_acc);
        final Button acc = this.findViewById(R.id.button_acc);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        datacsv = new StringBuilder();
        datacsv.append("x,y,z");
        graph.addSeries(series);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);

        api.setApiLogger(s -> Log.d(TAG,s));

        Log.d(TAG,"version: " + PolarBleApiDefaultImpl.versionInfo());

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG,"BLE power: " + powered);
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTED: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTING: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"DISCONNECTED: " + polarDeviceInfo.deviceId);
                ecgDisposable = null;
                accDisposable = null;
                ppgDisposable = null;
                ppiDisposable = null;
                acc.setText("Not Ready");
            }

            @Override
            public void ecgFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"ECG READY: " + identifier);
                // ecg streaming can be started now if needed
            }

            @Override
            public void accelerometerFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }

            @Override
            public void ppgFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"PPG READY: " + identifier);
                // ohr ppg can be started
            }

            @Override
            public void ppiFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"PPI READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void biozFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"BIOZ READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void hrFeatureReady(@NonNull String identifier) {
                Log.d(TAG,"HR READY: " + identifier);
                // hr notifications are about to start
            }

            @Override
            public void disInformationReceived(@NonNull String identifier,@NonNull UUID uuid,@NonNull String value) {
                Log.d(TAG,"uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(@NonNull String identifier, int level) {
                Log.d(TAG,"BATTERY LEVEL: " + level);

            }

            @Override
            public void hrNotificationReceived(@NonNull String identifier,@NonNull PolarHrData data) {
                Log.d(TAG,"testing : halo" + "HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);
                acc.setText("Ready");
            }

            @Override
            public void polarFtpFeatureReady(@NonNull String s) {
                Log.d(TAG,"FTP ready");
            }
        });

        disconnect.setOnClickListener(view -> {
            try {
                api.disconnectFromDevice(DEVICE_ID);
            } catch (PolarInvalidArgument polarInvalidArgument) {
                polarInvalidArgument.printStackTrace();
            }
        });

        autoConnect.setOnClickListener(view -> {
            if(autoConnectDisposable != null) {
                autoConnectDisposable.dispose();
                autoConnectDisposable = null;
            }
            autoConnectDisposable = api.autoConnectToDevice(-50, "180D", null).subscribe(
                    () -> Log.d(TAG,"auto connect search complete"),
                    throwable -> Log.e(TAG,"" + throwable.toString())
            );
        });

        acc.setOnClickListener(v -> {
            if(accDisposable == null) {
                accDisposable = api.requestAccSettings(DEVICE_ID).toFlowable().flatMap((Function<PolarSensorSetting, Publisher<PolarAccelerometerData>>) settings -> {
                    PolarSensorSetting sensorSetting = settings.maxSettings();
                    return api.startAccStreaming(DEVICE_ID,sensorSetting);
                }).observeOn(AndroidSchedulers.mainThread()).subscribe(
                        polarAccelerometerData -> {
                            for( PolarAccelerometerData.PolarAccelerometerSample data : polarAccelerometerData.samples ){
                                Log.d(TAG,"    x: " + data.x + " y: " + data.y + " z: "+ data.z);
                                series.appendData(new DataPoint(lastX++, data.x), true, 100);
                                datacsv.append("\n"+data.x+","+data.y+","+data.z);
                            }
                        },
                        throwable -> Log.e(TAG,""+throwable.getLocalizedMessage()),
                        () -> Log.d(TAG,"complete")
                );
            } else {
                // NOTE dispose will stop streaming if it is "running"
                accDisposable.dispose();
                accDisposable = null;
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == 1) {
            Log.d(TAG,"bt ready");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        api.backgroundEntered();
    }

    @Override
    public void onResume() {
        super.onResume();
        api.foregroundEntered();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
    }

    public void exportacc(View view) {
        try{
            //saving the file into device
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault());
            String formattedDate = df.format(c);
            FileOutputStream out = openFileOutput("dataacc_"+formattedDate+".csv", Context.MODE_PRIVATE);
            out.write((datacsv.toString()).getBytes());
            out.close();

            //exporting
            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "dataacc_"+formattedDate+".csv");
            Uri path = FileProvider.getUriForFile(context, "polar.com.androidblesdk.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Dataacc_"+formattedDate);
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
