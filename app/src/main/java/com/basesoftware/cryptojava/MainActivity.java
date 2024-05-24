package com.basesoftware.cryptojava;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import com.basesoftware.cryptojava.architecture.mvc.view.MVCActivity;
import com.basesoftware.cryptojava.architecture.mvp.view.MVPActivity;
import com.basesoftware.cryptojava.architecture.mvvm.view.MvvmActivity;
import com.basesoftware.cryptojava.architecture.nonArchitecture.NonArchitectureActivity;
import com.basesoftware.cryptojava.databinding.ActivityMainBinding;
import com.basesoftware.cryptojava.databinding.SelectArchitectureBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectArchitecture(); // Hangi mimariyi seçeceği kullanıcıya soruluyor

    }

    private void selectArchitecture() {

        AlertDialog.Builder architectureDialog = new AlertDialog.Builder(this);

        SelectArchitectureBinding architectureBinding = SelectArchitectureBinding.inflate(getLayoutInflater());

        architectureDialog.setCancelable(false);

        architectureDialog.setView(architectureBinding.getRoot());

        architectureDialog.setPositiveButton("TAMAM", (dialog, which) -> {

            RadioButton selectedArchitectureView = architectureBinding.getRoot().findViewById(architectureBinding.rdGroupArchitecture.getCheckedRadioButtonId());
            
            String selectedArchitecture = selectedArchitectureView.getText().toString();

            Class target = switch(selectedArchitecture) {
                case "MVC" -> MVCActivity.class;
                case "MVP" -> MVPActivity.class;
                case "MVVM" -> MvvmActivity.class;
                default -> NonArchitectureActivity.class;
            };

            Intent changeActivity = new Intent(this, target);
            startActivity(changeActivity);
            finish();

        });

        architectureDialog.show();

    }

}