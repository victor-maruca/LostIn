package oficiogr.com.lostin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LocationChooser extends AppCompatActivity {

    TextView tv, terminal;
    EditText origem, destino;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_chooser);

        tv = (TextView) findViewById(R.id.terminal);
        terminal = (TextView) findViewById(R.id.terminal);
        origem = (EditText) findViewById(R.id.etOrigem);
        destino = (EditText) findViewById(R.id.etDestino);
        btn = (Button) findViewById(R.id.btn);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                origem.setText("-22.813992, -47.058807");
            }
        });

        terminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destino.setText("-22.909510, -47.062018");
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationChooser.this, MapsActivity.class);
                startActivity(intent);
            }
        });
    }
}
