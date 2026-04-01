package com.group2A.solardesignbuddy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // 1. Declare the variables
    EditText etPvPower, etPvVoltage, etIsc, etBattVoltage;
    EditText etDistPvToFuse, etDistFuseToInv, etDistInvToBatt;
    Button btnCalculate;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. Connect variables to the XML IDs
        etPvPower = findViewById(R.id.etPvPower);
        etPvVoltage = findViewById(R.id.etPvVoltage);
        etIsc = findViewById(R.id.etIsc);
        etBattVoltage = findViewById(R.id.etBattVoltage);
        etDistPvToFuse = findViewById(R.id.etDistPvToFuse);
        etDistFuseToInv = findViewById(R.id.etDistFuseToInv);
        etDistInvToBatt = findViewById(R.id.etDistInvToBatt);
        btnCalculate = findViewById(R.id.btnCalculate);
        tvResult = findViewById(R.id.tvResult);

        // 3. Set up the button click listener
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSolarCalculations();
            }
        });
    }

    // 4. The Calculation Logic
    private void performSolarCalculations() {
        // Get numbers from inputs
        Double power = getNumericValue(etPvPower, getString(R.string.pv_power_w));
        Double pvVolts = getNumericValue(etPvVoltage, getString(R.string.pv_voltage_v));
        Double isc = getNumericValue(etIsc, getString(R.string.current_isc_a));
        Double battVolts = getNumericValue(etBattVoltage, getString(R.string.battery_voltage_v));
        Double dist1 = getNumericValue(etDistPvToFuse, getString(R.string.panels_to_fuse_box_m));
        Double dist2 = getNumericValue(etDistFuseToInv, getString(R.string.fuse_box_to_inverter_m));
        Double dist3 = getNumericValue(etDistInvToBatt, getString(R.string.inverter_to_battery_bank_m));

        if (power == null || pvVolts == null || isc == null || battVolts == null ||
                dist1 == null || dist2 == null || dist3 == null) {
            return; // Error message already shown
        }

        if (pvVolts == 0) {
            Toast.makeText(this, R.string.pv_voltage_cannot_be_zero, Toast.LENGTH_SHORT).show();
            return;
        }

        if (battVolts == 0) {
            Toast.makeText(this, R.string.battery_voltage_cannot_be_zero, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double rho = 0.0172; // Copper resistivity

            // CALC 1: PV to Fuse
            double allowedDropPv = pvVolts * 0.03;
            double area1 = (2 * dist1 * isc * rho) / allowedDropPv;

            // CALC 2: Fuse to Inverter
            double area2 = (2 * dist2 * isc * rho) / allowedDropPv;

            // CALC 3: Inverter to Battery (High Current!)
            double battCurrent = power / battVolts;
            double allowedDropBatt = battVolts * 0.03;
            double area3 = (2 * dist3 * battCurrent * rho) / allowedDropBatt;

            // PROTECTION
            double fuseRating = isc * 1.56;
            double breakerRating = battCurrent * 1.25;

            // DISPLAY RESULT
            String report = getString(R.string.solar_sizing_report) + "\n\n" +
                    getString(R.string.pv_to_fuse_box_report, String.format("%.2f", area1), getStandardCable(area1)) + "\n\n" +
                    getString(R.string.fuse_box_to_inverter_report, String.format("%.2f", area2), getStandardCable(area2)) + "\n\n" +
                    getString(R.string.inverter_to_battery_report, String.format("%.1f", battCurrent), String.format("%.2f", area3), getStandardCable(area3)) + "\n\n" +
                    getString(R.string.protection_report, Math.ceil(fuseRating), Math.ceil(breakerRating));

            tvResult.setText(report);

        } catch (Exception e) {
            tvResult.setText(getString(R.string.error_message, e.getMessage()));
        }
    }

    // Helper to get a numeric value from an EditText and validate it.
    private Double getNumericValue(EditText editText, String fieldName) {
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, getString(R.string.please_fill_in_the_field, fieldName), Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_number_format_in_field, fieldName), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Helper: Pick standard cable
    private double getStandardCable(double calculated) {
        double[] sizes = {1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0};
        for (double size : sizes) {
            if (size >= calculated) return size;
        }
        return 95.0;
    }
}