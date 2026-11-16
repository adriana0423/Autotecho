package com.example.autotecho;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PerfilActivity extends AppCompatActivity {

    // Declaración de variables para los botones de la barra inferior
    private TextView btnNavInicio, btnNavControl, btnNavAmbiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.perfil); // Vincula con perfil.xml

        // Ajuste de bordes de pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Vincular TextViews con los IDs de perfil.xml
        btnNavInicio = findViewById(R.id.btnNavInicioPerfil);
        btnNavControl = findViewById(R.id.btnNavControlPerfil);
        btnNavAmbiente = findViewById(R.id.btnNavAmbientePerfil);

        // 2. Navegación a Inicio (Home)
        btnNavInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerfilActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        // 3. Navegación a Control
        btnNavControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerfilActivity.this, ControlActivity.class);
                startActivity(intent);
            }
        });

        // 4. Navegación a Ambiente
        btnNavAmbiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerfilActivity.this, AmbienteActivity.class);
                startActivity(intent);
            }
        });
    }
}