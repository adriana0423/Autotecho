package com.example.autotecho;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControlActivity extends AppCompatActivity {

    // Cambiar por la IP real del ESP32.
    private static final String ESP32_URL = "http://10.157.34.175";

    private TextView btnNavInicio;
    private TextView btnNavAmbiente;
    private TextView btnNavPerfil;

    private TextView tvEstadoTecho;
    private TextView tvDescripcionEstado;
    private TextView tvConexionEsp32;
    private TextView tvPorcentajeApertura;

    private Button btnAbrirTecho;
    private Button btnCerrarTecho;
    private Button btnAplicarApertura;

    private SeekBar sbApertura;
    private Switch switchModoManual;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean modoManual = false;
    private boolean actualizandoInterfaz = false;
    private boolean arrastrando = false;
    private int porcentajeSeleccionado = 0;

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
        setContentView(R.layout.control);

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
        configurarControles();

        actualizarControles();

        // Consultar el estado del ESP32 cada tres segundos.
        handler.post(consultarEstado);
    }

    private void enlazarComponentes() {
        btnNavInicio = findViewById(R.id.btnNavInicioControl);
        btnNavAmbiente = findViewById(R.id.btnNavAmbienteControl);
        btnNavPerfil = findViewById(R.id.btnNavPerfilControl);

        tvEstadoTecho = findViewById(R.id.tvEstadoTecho);
        tvDescripcionEstado = findViewById(R.id.tvDescripcionEstado);
        tvConexionEsp32 = findViewById(R.id.tvConexionEsp32);
        tvPorcentajeApertura = findViewById(R.id.tvPorcentajeApertura);

        btnAbrirTecho = findViewById(R.id.btnAbrirTecho);
        btnCerrarTecho = findViewById(R.id.btnCerrarTecho);
        btnAplicarApertura = findViewById(R.id.btnAplicarApertura);

        sbApertura = findViewById(R.id.sbApertura);
        switchModoManual = findViewById(R.id.switchModoManual);
    }

    private void configurarNavegacion() {
        btnNavInicio.setOnClickListener(v ->
                abrirPantalla(HomeActivity.class)
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

    private void configurarControles() {
        sbApertura.setMax(100);
        sbApertura.setProgress(0);

        tvPorcentajeApertura.setText("0%");

        sbApertura.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        porcentajeSeleccionado = progress;
                        tvPorcentajeApertura.setText(progress + "%");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        arrastrando = true;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        arrastrando = false;
                    }
                }
        );

        switchModoManual.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (actualizandoInterfaz) {
                        return;
                    }

                    modoManual = isChecked;
                    actualizarControles();

                    String valor = isChecked ? "true" : "false";

                    enviarOrden("/modo?manual=" + valor, respuesta -> {
                        Toast.makeText(
                                this,
                                respuesta
                                        ? "Modo manual actualizado"
                                        : "No se pudo confirmar el modo manual",
                                Toast.LENGTH_SHORT
                        ).show();

                        if (!respuesta) {
                            actualizandoInterfaz = true;
                            switchModoManual.setChecked(!isChecked);
                            modoManual = !isChecked;
                            actualizandoInterfaz = false;
                            actualizarControles();
                        }
                    });
                }
        );

        btnAbrirTecho.setOnClickListener(v ->
                ejecutarOrdenManual("/abrir")
        );

        btnCerrarTecho.setOnClickListener(v ->
                ejecutarOrdenManual("/cerrar")
        );

        btnAplicarApertura.setOnClickListener(v -> {
            if (!modoManual) {
                mostrarMensaje("Activa primero el modo manual");
                return;
            }

            String ruta = "/apertura?porcentaje="
                    + porcentajeSeleccionado;

            ejecutarOrdenManual(ruta);
        });
    }

    private void ejecutarOrdenManual(String ruta) {
        if (!modoManual) {
            mostrarMensaje("Activa primero el modo manual");
            return;
        }

        enviarOrden(ruta, respuesta -> {
            if (respuesta) {
                mostrarMensaje("Orden enviada al ESP32");
                consultarEstadoEsp32();
            } else {
                mostrarMensaje("No se pudo enviar la orden");
            }
        });
    }

    private void actualizarControles() {
        sbApertura.setEnabled(modoManual);
        btnAplicarApertura.setEnabled(modoManual);
        btnAbrirTecho.setEnabled(modoManual);
        btnCerrarTecho.setEnabled(modoManual);

        float opacidad = modoManual ? 1.0f : 0.55f;

        sbApertura.setAlpha(opacidad);
        btnAplicarApertura.setAlpha(opacidad);
        btnAbrirTecho.setAlpha(opacidad);
        btnCerrarTecho.setAlpha(opacidad);
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
                    throw new Exception("Respuesta HTTP " + codigo);
                }

                String respuesta = leerRespuesta(
                        conexion.getInputStream()
                );

                JSONObject datos = new JSONObject(respuesta);

                String estado = datos.optString(
                        "estado",
                        "Desconocido"
                );

                int porcentaje = datos.optInt(
                        "porcentaje",
                        -1
                );

                boolean manual = datos.optBoolean(
                        "modoManual",
                        modoManual
                );

                runOnUiThread(() -> {
                    tvConexionEsp32.setText("Conectado");
                    tvConexionEsp32.setTextColor(0xFF2E7D32);
                    tvConexionEsp32.setBackgroundColor(0xFFE8F5E9);

                    tvEstadoTecho.setText(estado);

                    tvDescripcionEstado.setText(
                            porcentaje >= 0
                                    ? "Apertura real: " + porcentaje + "%"
                                    : "Estado recibido del dispositivo"
                    );

                    if (porcentaje >= 0 && !arrastrando) {
                        porcentajeSeleccionado = porcentaje;
                        sbApertura.setProgress(porcentaje);
                        tvPorcentajeApertura.setText(
                                porcentaje + "%"
                        );
                    }

                    if (switchModoManual.isChecked() != manual) {
                        actualizandoInterfaz = true;
                        switchModoManual.setChecked(manual);
                        actualizandoInterfaz = false;
                    }

                    modoManual = manual;
                    actualizarControles();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvConexionEsp32.setText("Desconectado");
                    tvConexionEsp32.setTextColor(0xFFC62828);
                    tvConexionEsp32.setBackgroundColor(0xFFFFF0F0);

                    tvEstadoTecho.setText("Sin conexión");
                    tvDescripcionEstado.setText(
                            "No se pudo obtener el estado del dispositivo."
                    );
                });

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    private void enviarOrden(
            String ruta,
            ResultadoOrden callback
    ) {
        executor.execute(() -> {
            HttpURLConnection conexion = null;
            boolean exito = false;

            try {
                URL url = new URL(ESP32_URL + ruta);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(3000);
                conexion.setReadTimeout(3000);

                int codigo = conexion.getResponseCode();

                exito = codigo >= 200 && codigo < 300;

            } catch (Exception e) {
                exito = false;

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }

            boolean resultado = exito;

            runOnUiThread(() -> callback.terminado(resultado));
        });
    }

    private String leerRespuesta(InputStream entrada)
            throws Exception {

        StringBuilder resultado = new StringBuilder();

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(
                        entrada,
                        StandardCharsets.UTF_8
                )
        )) {
            String linea;

            while ((linea = lector.readLine()) != null) {
                resultado.append(linea);
            }
        }

        return resultado.toString();
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(consultarEstado);
        executor.shutdownNow();
        super.onDestroy();
    }

    private interface ResultadoOrden {
        void terminado(boolean respuesta);
    }
}