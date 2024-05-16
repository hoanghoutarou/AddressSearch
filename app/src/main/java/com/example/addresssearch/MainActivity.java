package com.example.addresssearch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EditText searchInput;
    private ListView resultsList;
    private AddressAdapter  adapter;
    private ArrayList<String> results;
    private RequestQueue requestQueue;
    private Handler handler = new Handler();
    private Runnable searchRunnable;
    private final long DEBOUNCE_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        searchInput = findViewById(R.id.search_input);
        resultsList = findViewById(R.id.results_list);

        results = new ArrayList<>();
        adapter = new AddressAdapter(results);
        resultsList.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable = () -> searchAddresses(s.toString()), DEBOUNCE_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            String address = results.get(position);
            openInGoogleMaps(address);
        });
    }
    private void searchAddresses(String query) {
        if (query.isEmpty()) {
            results.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        String url = "https://geocode.search.hereapi.com/v1/geocode?q=" + query + "&apiKey=asdw0UZ5iW1aFRA0cL0qmHFK-59UD5-nlCj5bkR_52M";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        results.clear();
                        try {
                            JSONArray items = response.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                String address = item.getJSONObject("address").getString("label");
                                results.add(address);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        adapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error fetching results", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
    }
    private void openInGoogleMaps(String address) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
        }
    }
    private class AddressAdapter extends ArrayAdapter<String> {
        AddressAdapter(ArrayList<String> addresses) {
            super(MainActivity.this, 0, addresses);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_result, parent, false);
            }

            String address = getItem(position);
            TextView addressText = convertView.findViewById(R.id.address_text);
            Button directionsButton = convertView.findViewById(R.id.directions_button);

            addressText.setText(address);
            directionsButton.setOnClickListener(v -> openInGoogleMaps(address));

            return convertView;
        }
    }
}