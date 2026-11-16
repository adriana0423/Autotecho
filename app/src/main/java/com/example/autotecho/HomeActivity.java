
package com.example.autotecho;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private static final String ESP32_URL = "http://10.157.34.175";

    private TextView btnNavControl;
    private TextView btnNavAmbiente;
    private TextView btnNavPerfil;

    private TextView tvEstadoTecho;
    private TextView tvTemperatura;
    private TextView tvHumedad;
    private TextView tvAlertas;

    private LinearLayout layoutNotificaciones;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private String ultimaAlerta = "";
    private String estadoAnterior = "";
    private boolean primeraConsulta = true;

    private final Runnable consultarEstado = new Runnable() {
        @Override
        public void run() {
            consultarEstadoEsp32();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.home);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        enlazarComponentes();
        configurarNavegacion();
    }

    private void enlazarComponentes() {

        btnNavControl = findViewById(R.id.btnNavControlHome);
        btnNavAmbiente = findViewById(R.id.btnNavAmbienteHome);
        btnNavPerfil = findViewById(R.id.btnNavPerfilHome);

        tvEstadoTecho = findViewById(R.id.tvEstadoTecho);
        tvTemperatura = findViewById(R.id.tvTemperatura);
        tvHumedad = findViewById(R.id.tvHumedad);

        tvAlertas = findViewById(R.id.tvAlertas);
        layoutNotificaciones = findViewById(R.id.layoutNotificaciones);
    }

    private void configurarNavegacion() {

        btnNavControl.setOnClickListener(v ->
                abrirPantalla(ControlActivity.class)
        );

        btnNavAmbiente.setOnClickListener(v ->
                abrirPantalla(AmbienteActivity.class)
        );

        btnNavPerfil.setOnClickListener(v ->
                abrirPantalla(PerfilActivity.class)
        );
    }

    private void abrirPantalla(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla));
    }

    private void consultarEstadoEsp32() {

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ESP32_URL + "/estado");

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(3000);
                conexion.setReadTimeout(3000);

                int codigo = conexion.getResponseCode();

                if (codigo != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Error HTTP: " + codigo);
                }

                StringBuilder respuesta = new StringBuilder();

                try (BufferedReader lector = new BufferedReader(
                        new InputStreamReader(
                                conexion.getInputStream(),
                                StandardCharsets.UTF_8
                        ))) {

                    String linea;

                    while ((linea = lector.readLine()) != null) {
                        respuesta.append(linea);
                    }
                }

                JSONObject datos = new JSONObject(
                        respuesta.toString()
                );

                String estado = datos.optString(
                        "estado",
                        "Desconocido"
                );

                String alerta = datos.optString(
                        "alerta",
                        ""
                );

                boolean lluvia = datos.optBoolean(
                        "lluvia",
                        false
                );

                double temperatura = datos.optDouble(
                        "temperatura",
                        -999
                );

                double humedad = datos.optDouble(
                        "humedad",
                        -999
                );

                runOnUiThread(() -> {

                    // Actualizar estado del techo.
                    tvEstadoTecho.setText(estado);

                    // Actualizar temperatura.
                    if (temperatura != -999) {
                        tvTemperatura.setText(
                                String.format(
                                        java.util.Locale.getDefault(),
                                        "%.1f °C",
                                        temperatura
                                )
                        );
                    }

                    // Actualizar humedad.
                    if (humedad != -999) {
                        tvHumedad.setText(
                                String.format(
                                        java.util.Locale.getDefault(),
                                        "%.0f%%",
                                        humedad
                                )
                        );
                    }

                    procesarNotificaciones(
                            lluvia,
                            estado,
                            alerta
                    );
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    tvEstadoTecho.setText("Sin conexión");
                });

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    private void procesarNotificaciones(
            boolean lluvia,
            String estado,
            String alerta
    ) {

        // Detectar cambios en el estado del techo.
        if (!primeraConsulta && !estado.equals(estadoAnterior)) {

            agregarNotificacion(
                    "Estado del techo: " + estado
            );
        }

        // Leer las alertas enviadas por Arduino.
        if (alerta != null
                && !alerta.trim().isEmpty()
                && !alerta.equals(ultimaAlerta)) {

            agregarNotificacion(alerta);
            ultimaAlerta = alerta;
        }

        estadoAnterior = estado;
        primeraConsulta = false;
    }

    private void agregarNotificacion(String mensaje) {

        tvAlertas.setVisibility(View.GONE);

        TextView notificacion = new TextView(this);

        notificacion.setText("• " + mensaje);
        notificacion.setTextSize(14);
        notificacion.setTextColor(
                Color.rgb(55, 55, 55)
        );

        notificacion.setPadding(0, 12, 0, 12);

        layoutNotificaciones.addView(notificacion, 0);

        // Mantener máximo 10 notificaciones.
        while (layoutNotificaciones.getChildCount() > 10) {

            layoutNotificaciones.removeViewAt(
                    layoutNotificaciones.getChildCount() - 1
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(consultarEstado);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(consultarEstado);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(consultarEstado);
        executor.shutdownNow();
        super.onDestroy();
    }
}
