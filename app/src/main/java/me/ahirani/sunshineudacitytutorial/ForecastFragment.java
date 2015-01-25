package me.ahirani.sunshineudacitytutorial;

/**
 * Created by Ali on 25-Jan-2015.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    // Declare ArrayAdapter
    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    // Fragment lifecycle method where the fragment is created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Needed in order from this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Inflate menu layout defined earlier
        inflater.inflate(R.menu.forecastfragment, menu);
    }



    // Get notified when a menu item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click here
        // When menu item with id below is selected return true
        int id = item.getItemId();
        if (id == R.id.action_refresh) {

            // Create new fetchweathertask and call execute on it
            // App will crash due to SecurityException (missing internet permission)
            FetchWeatherTask weatherTask = new FetchWeatherTask();

            // Takes in first 3 digits of postal code
            weatherTask.execute("L4T");

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This is where the UI gets initialized
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Sample data
        String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/40",
                "Weds - Cloudy - 72/63",
                "Thurs -Asteroids - 75/65",
                "Fri - Foggy - 70/46",
                "Sat - SNOW STORM WARNING - 60/51",
                "Sun - Sunny - 80/68"
        };

        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray));


        // ArrayAdapter will take data from a source and populate the ListView it's attached to
        mForecastAdapter = new ArrayAdapter<String>(

                // Context contains global about app environment, allows access to system services etc
                // The current context (parent activity of the this fragment)
                getActivity(),

                // ID of list item layout (layout file)
                R.layout.list_item_forecast,

                // ID of text view we wish to populate (specific XML element)
                R.id.list_item_forecast_textview,

                // list of data
                weekForecast);

        // Get a reference to ListView and attach adapter
        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_forecast);

        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    // Accepts a String parameter (postal code) and returns an array of forecasts
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();



        @Override
        // Pass in String... params, postal code is stored in 0th param
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            // Various PARAMS
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                // FORMAT: PARAM=INPUT
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                    //ex: q=L4T,CA
                    final String QUERY_PARAM = "q";

                    //ex: mode=json
                    final String FORMAT_PARAM = "mode";

                    //ex: units=metric
                    final String UNITS_PARAM = "units";

                    //ex: cnt=2
                    final String DAYS_PARAM = "cnt";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()

                        // Postal code stored in 0th cell of params array
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays)).build();

                URL url = new URL(builtUri.toString());

                // Verbose LOG Command to print built URI
                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=L4T,CA&mode=json&units=metric&cnt=7");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                // Verify data is returned by adding a verbose log statement that prints the JSON string
                Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }
    }
}