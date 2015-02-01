package me.ahirani.sunshineudacitytutorial;

/**
 * Created by Ali on 25-Jan-2015.
 */

import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    // Declare ArrayAdapter
    public ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    // Fragment lifecycle method where the fragment is created
    // Called before onCreateView and does not rely on content view
    // hierarchy being initialized at this point
    @Override

    // Parameter: savedInstanceState means the fragment is being
    // recreated from a previous saved state
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Needed in order for this fragment to handle menu events
        // and therefore for the onCreateOptionsMenu to be called
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

    // Parameters:
    // inflater: The LayoutInflater object that can be used to inflate any views in the fragment
    // container: non-null, parent view that the fragment's UI should be attached to
    // savedInstanceState: non-null, fragment is being reconstructed from a previous saved state

    //Returns: Return the View for the fragment's UI
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Sample data
        final String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/40",
                "Weds - Cloudy - 72/63",
                "Thurs -Asteroids - 75/65",
                "Fri - Foggy - 70/46",
                "Sat - SNOW STORM WARNING - 60/51",
                "Sun - Sunny - 80/68"
        };

        // Instantiate an ArrayList
        List<String> weekForecast = new ArrayList<String>(

                // Arrays.asList() converts an array to a List object
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

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to ListView and attach adapter
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        // This method is used to wire up behaviour on a list item when it is clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // Get the position from the Adapter
                String forecast = mForecastAdapter.getItem(position);

                /*

                // Instantiate toast
                Toast forecastEntryToast;

                // Set up the toast
                forecastEntryToast = Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT);

                // Display the toast
                forecastEntryToast.show();

                */

                // Launch an explicit intent to the detail activity class
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)

                        //EXTRA_TEXT is a constant char sequence associated with the Intent
                        .putExtra(Intent.EXTRA_TEXT, forecast);

                // Execute detailIntent
                startActivity(detailIntent);



            }
        });

        return rootView;
    }

    // Accepts a String parameter (postal code) and returns an array of forecasts
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.*/

         private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }

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
                // If the code didn't successfully get the weather data, there's no point in attempting
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
                try {
                    return getWeatherDataFromJson(forecastJsonStr, numDays);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }

            // If an error getting or parsing the forecast
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {

                // Clear old entries
                mForecastAdapter.clear();

                // Loop through and append entries to list
                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}