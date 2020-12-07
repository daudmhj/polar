package polar.com.androidblesdk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button hr = this.findViewById(R.id.hr_button);
        hr.setOnClickListener(v -> {
            Log.d("enter", "Activity PPG");
            Intent hrintent = new Intent(MainActivity.this,HrActivity.class);
            startActivity(hrintent);
        });

        final Button ppg = this.findViewById(R.id.ppg_button);
        ppg.setOnClickListener(v -> {
            Log.d("enter", "Activity PPG");
            Intent ppgintent = new Intent(MainActivity.this,PpgActivity.class);
            startActivity(ppgintent);
        });

        final Button ppi = this.findViewById(R.id.ppi_button);
        ppi.setOnClickListener(v -> {
            Log.d("enter", "Activity PPI");
            Intent ppiintent = new Intent(MainActivity.this,PpiActivity.class);
            startActivity(ppiintent);
        });

        final Button acc = this.findViewById(R.id.acc_button);
        acc.setOnClickListener(v -> {
            Log.d("enter", "Activity PPI");
            Intent accintent = new Intent(MainActivity.this,AccActivity.class);
            startActivity(accintent);
        });

    }

}
