package oficiogr.com.lostin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public GoogleMap mMap;
    static boolean firstLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        new Monitor(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    if(!firstLocation) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 14));

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                                .zoom(18)
                                .bearing(90)
                                .tilt(40)
                                .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        firstLocation = true;
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 50, 5, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 50, 5, locationListener);
    }

    public void newTick(String lat, String lon){
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(Double.valueOf(lat), Double.valueOf(lon)))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_black_24dp)));
    }

    public void clearMap(){
        mMap.clear();
    }

    public void createRoute(String s){
        try {
            JSONArray a = new JSONArray(s);
            double totalDistance = 0;
            double totalTime = 0;

            for(int j=0 ; j<a.length() ; j++) {
                JSONArray json = a.getJSONObject(j).getJSONArray("steps");
                for (int i = 0; i < json.length(); i++) {
                    double startLat, startLong, endLat, endLong;
                    JSONObject start = json.getJSONObject(i).getJSONObject("start_location");
                    JSONObject end = json.getJSONObject(i).getJSONObject("end_location");
                    JSONObject dist = json.getJSONObject(i).getJSONObject("distance");
                    JSONObject time = json.getJSONObject(i).getJSONObject("duration");
                    totalDistance += dist.getDouble("value");
                    totalTime += time.getDouble("value");
                    startLat = start.getDouble("lat");
                    startLong = start.getDouble("lng");
                    endLat = end.getDouble("lat");
                    endLong = end.getDouble("lng");
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(startLat, startLong),
                                    new LatLng(endLat, endLong)));
                }
            }

            Toast.makeText(this, "Distância: "+totalDistance+"m. Tempo estimado: "+totalTime+"min", Toast.LENGTH_LONG).show();
        }catch(JSONException e){
            Log.e("",e.toString());
        }
    }
}

class Monitor extends AsyncTask<Void, Void, Void>{

    Context ctx;

    public Monitor(Context ctx){
        this.ctx = ctx;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Constants.HOST);
            factory.setUsername(Constants.USER);
            factory.setPassword(Constants.PASS);
            factory.setVirtualHost(Constants.VHOST);
            Connection con = factory.newConnection();
            Channel channel = con.createChannel();
            channel.queueDeclare(Constants.QUEUE, true, false, false, null);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body){


                    final byte[] received = body;
                    String type = properties.getHeaders().get("TYPE")+"";

                    switch(type){
                        case "MARKER":
                            final String lat = properties.getHeaders().get("lat")+"";
                            final String lng = properties.getHeaders().get("lon")+"";
                            ((MapsActivity)ctx).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MapsActivity)ctx).newTick(lat,lng);
                                }
                            });
                            break;
                        case "ROUTE":
                            ((MapsActivity)ctx).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MapsActivity)ctx).createRoute(new String(received));
                                }
                            });
                            break;
                        case "CLEAR":
                            ((MapsActivity)ctx).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MapsActivity)ctx).clearMap();
                                }
                            });
                            break;
                    }
                }
            };

            channel.basicConsume(Constants.QUEUE, true, consumer);
        }catch(Exception e){
            Log.e("CONSUMER", e.toString());
        }
        return null;
    }
}

