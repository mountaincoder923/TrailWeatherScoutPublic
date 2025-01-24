package TrailWeatherScoutBackend;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.regex.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
// Returns temp in degrees C.

public class Forecaster {
    // Variable here controls max number of trails user can add. Restricts to prevent slow calculation
    private static final int MAX_NUM = 16;

    private final ArrayList<Trailhead> trailheads = new ArrayList<>();
    private final ArrayList<WeatherCondition> WeatherConditions = new ArrayList<>();


    public Forecaster() {
    }

    public void Bootup() { // Code to get resolving DNS overhead out of the way before user input is sent
        String apiUrl = "https://api.weather.gov/alerts/active?limit=1";
        try {
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "(TrailWeatherScoutBackend, contact@example.com)");
            conn.connect();
            System.out.println("Bootup connection established!");
            if (conn.getResponseCode() != 200) {
                System.err.println("Ping failed with response code: " + conn.getResponseCode());
            }

        } catch (Exception e) {
            System.err.println("Ping failed: " + e.getMessage());
        }
    }

    public Boolean addTrailhead(String Name, String LatCord, String LongCord, boolean custom) {
        if (trailheads.size() >= MAX_NUM) {
            System.err.println("TrailWeatherScoutBackend.Trailhead limit of " + MAX_NUM + " reached. Source: addTrailhead");
            return false;
        }

        if (!isValidUSCord(LatCord, LongCord)) {
            System.err.println("Invalid Cord: " + LongCord + ", " + LatCord + " not in USA. Not added! Source: addTrailhead");
            return false;
        }

        try {
            LatCord = truncateTo4Decimals(LatCord);
            LongCord = truncateTo4Decimals(LongCord);
        }
        catch (Exception e) {
            System.err.println("Invalid Cord: " + LongCord + ", " + LatCord + " not properly formated! Source: addTrailhead (truncation)");
            return false;
        }

        for (Trailhead trailhead : trailheads) {
            if (trailhead.getLatCord().equals(LatCord) && trailhead.getLongCord().equals(LongCord)) {
                System.err.println("No Duplicate Locations Allowed!");
                return false;
            }
        }

        trailheads.add(new Trailhead(Name, LatCord, LongCord, custom));
        return true;
    }


    public Boolean addTrailhead(JSONObject json) {
        try {
            return this.addTrailhead(json.getString("Name"), json.getString("LatCord"), json.getString("LongCord"), json.getBoolean("Custom"));
        } catch (JSONException e) {
            System.err.println("JSON Error: " + e.getMessage());
            return false;
        }
    }

    public Boolean addTrailhead(JSONArray jsonArray) {
        Boolean AllDone = true;

        if (jsonArray == null || jsonArray.isEmpty()) {
            System.err.println("JSON Array is null or empty. Source: addTrailheadJSONARRAY)");
            return false;
        }

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                AllDone &= this.addTrailhead(jsonObj);
            }
        } catch (JSONException e) {
            System.err.println("JSON Error: " + e.getMessage());
            AllDone = false;
        }
        return AllDone;
    }

    public void Reset() {
        trailheads.clear();
        WeatherConditions.clear();
    }

    public boolean calculateConditions(LocalDate date) {
        if (trailheads.isEmpty()) {
            System.err.println("No trailheads found. Source: calculateConditions");
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(7);
        if (!isWithinRange(date, today, end)) {
            System.err.println("Date Requested Exceeded Forecast Range!");
            return false;
        }
        for (Trailhead trailhead : trailheads) {
            WeatherConditions.add(trailhead.getCondition(date));
        }
        return true;
    }

    public List<JSONObject> getRecommendationList() {
        List<JSONObject> sortedConditions = new ArrayList<>();

        if (WeatherConditions.isEmpty()) {
            System.err.println("No Conditions Found! Source: getRecommendationList");
            return List.of(new JSONObject().put("error", "No weather data available"));
        }
        Collections.sort(WeatherConditions);

        for (WeatherCondition condition : WeatherConditions) {
            sortedConditions.add(new JSONObject()
                    .put("recommendedTrail", condition.getTrailhead().getName())
                    .put("skyCover", condition.getSkyCover())
                    .put("temperature", condition.getTemperature())
                    .put("highTemperature", condition.getHighTemperature())
                    .put("lowTemperature", condition.getLowTemperature())
                    .put("feels_like", condition.getFeelsLike())
                    .put("precipitation", condition.getPrecipitation())
                    .put("custom", condition.getTrailhead().isCustom()));
        }

        return sortedConditions;
    }

    public Map<String, Object> getRecommendation() {
        if (WeatherConditions.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No weather data available");
            return error;
        }
        Collections.sort(WeatherConditions);
        ArrayList<WeatherCondition> idealConditions = new ArrayList<>();
        // Checks for tie
        int bestSkyCover = (int) Math.round(WeatherConditions.get(0).getSkyCover());
        for (WeatherCondition condition : WeatherConditions) {
            if ((int) Math.round(condition.getSkyCover()) == bestSkyCover) {
                idealConditions.add(condition);
            } else {
                break;
            }
        }

        WeatherCondition bestCondition = idealConditions.stream()
                .min(Comparator.comparingDouble(WeatherCondition::getPrecipitation))
                .orElse(WeatherConditions.get(0));

        Map<String, Object> response = new HashMap<>();
        response.put("recommendedTrail", bestCondition.getTrailhead().getName());
        response.put("skyCover", bestCondition.getSkyCover());
        response.put("temperature", bestCondition.getTemperature());
        response.put("highTemperature", bestCondition.getHighTemperature());
        response.put("lowTemperature", bestCondition.getLowTemperature());
        response.put("feels_like", bestCondition.getFeelsLike());
        response.put("precipitation", bestCondition.getPrecipitation());
        response.put("custom", bestCondition.getTrailhead().isCustom());
        return response;
    }


    private static boolean isWithinRange(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isEqual(start) || date.isAfter(start)) &&
                (date.isEqual(end) || date.isBefore(end));
    }

    private static boolean isValidUSCord(String LatCord, String LongCord) {
        try {
            double lat = Double.parseDouble(LatCord);
            double lon = Double.parseDouble(LongCord);
            boolean mainland = (lat >= 24.396308 && lat <= 49.384358) && (lon >= -125.0 && lon <= -66.93457);
            boolean alaska = (lat >= 51.214183 && lat <= 71.538800) && (lon >= -179.148909 && lon <= -129.993);
            boolean hawaii = (lat >= 18.9100 && lat <= 28.4021) && (lon >= -178.3347 && lon <= -154.8063);
            boolean puertoRico = (lat >= 17.5 && lat <= 18.5) && (lon >= -67.5 && lon <= -65.0);
            return mainland || alaska || hawaii || puertoRico;
        } catch (Exception e) {
            System.err.println("Invalid USCord: " + LongCord + ", " + LatCord + " not in USA. Unable to access weather data");
            System.err.println(e.getMessage());
            return false;
        }
    }

    private static String truncateTo4Decimals(String input) {
        // Regex pattern to capture up to the 4th decimal and discard extra digits
        String regex = "^(-?\\d+\\.\\d{0,4})\\d*$";  // Match up to 4 decimal digits
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            // Return the truncated version, capturing only up to the 4th decimal
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid Input (Not Decimal): " + input);
        }
    }


}
