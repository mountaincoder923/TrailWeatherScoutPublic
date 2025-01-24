package TrailWeatherScoutBackend;

import java.time.LocalDate;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class Trailhead {
    private final boolean custom;
    private final String name;
    private final String LatCord;
    private final String LongCord;
    private static final LocalTime START_TIME = LocalTime.of(7, 0);
    private static final LocalTime END_TIME = LocalTime.of(19, 0);
    private JSONObject forecast;

    public Trailhead(String name, String LatCord, String LongCord, boolean custom) {
        this.name = name;
        this.LatCord = LatCord;
        this.LongCord = LongCord;
        this.custom = custom;
        try {
            fetchWeatherData();
        } catch (Exception e) {
            System.err.println("Unable to fetch forecast at: " + LatCord + " " + LongCord + " " + e.getMessage());
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean isCustom() {
        return this.custom;
    }

    public String getLatCord() {
        return this.LatCord;
    }

    public String getLongCord() {
        return this.LongCord;
    }

    public WeatherCondition getCondition(LocalDate date) {
        double[] Temps = calculateTemp(date);
        if (Temps.length != 3){
            System.err.println("Failed to get Conditions. Temperatures not properly calculated. Source: getCondition");
            return new WeatherCondition(this,-9999,-9999,-9999,-9999,-9999,-9999);
        }
        try {
            return new WeatherCondition(
                    this,
                    Temps[0],
                    Temps[1],
                    Temps[2],
                    calculateSkyCover(date),
                    calculatePrecipitation(date),
                    calculateFeelsLike(date)
            );
        }
        catch (Exception e) {
            System.err.println("Failed to get Conditions. Source: getCondition" + e.getMessage());
            return new WeatherCondition(this,-9999,-9999,-9999,-9999,-9999,-9999);
        }
    }

    @Override
    public String toString() {
        return "TrailWeatherScoutBackend.Trailhead{" +
                "name='" + name + '\'' +
                ", LatCord='" + LatCord + '\'' +
                ", LongCord='" + LongCord + '\'' +
                ", forecast=" + (forecast != null ? "Loaded" : "Not Available") +
                '}';
    }

    private double calculateSkyCover(LocalDate date) {
        try {
            double skyCover = extractData("skyCover", date)[0];
            return Math.max(skyCover, 0);
        } catch (Exception e) {
            System.err.println("Unable to calculate sky cover");
            return -9999;
        }
    }

    private double[] calculateTemp(LocalDate date) {
        try {
            return extractData("temperature", date);
        } catch (Exception e) {
            System.err.println("Unable to calculate temperature");
            double[] temp = new double[1];
            temp[0] = -9999;
            return temp;
        }
    }

    private double calculatePrecipitation(LocalDate date) {
        try {
            double probabilityOfPrecipitation = extractData("probabilityOfPrecipitation", date)[0];
            return Math.max(0, probabilityOfPrecipitation);
        } catch (Exception e) {
            System.err.println("Unable to calculate Precipitation");
            return -9999;
        }
    }

    private double calculateFeelsLike(LocalDate date) {
        try {
            return extractData("apparentTemperature", date)[0];
        } catch (Exception e) {
            System.err.println("Unable to calculate feels like");
            return -9999;
        }
    }

    private void fetchWeatherData() {
        String pointsUrl = "https://api.weather.gov/points/" + this.LatCord + "," + this.LongCord;
        try {
            JSONObject pointsResponse = fetchJson(pointsUrl);
            String gridForecastUrl = pointsResponse
                    .getJSONObject("properties")
                    .getString("forecastGridData");
            this.forecast = fetchJson(gridForecastUrl);
        } catch (Exception e) {
            System.err.println("Error in fetching data: " + e.getMessage());
            this.forecast = null;
        }
    }

    private double[] extractData(String condition, LocalDate date) {
        double[] results = new double[3];
        if (forecast == null
                || !forecast.has("properties")
                || !forecast.getJSONObject("properties").has(condition)) {
            System.err.println("Forecast Data does not exist or key '" + condition + "' is missing! Source: extractData");
            results[0] = -9999;
            return results;
        }
        JSONArray conditionsValues = forecast
                .getJSONObject("properties")
                .getJSONObject(condition)
                .getJSONArray("values");
        String rawUom = forecast.getJSONObject("properties").getJSONObject(condition).optString("uom", "");
        System.err.println("Units for " + condition + " is " + rawUom);

        double totalWeightedValue = 0.0;
        double totalHoursInRange = 0.0;
        ZoneId localZone = ZoneId.systemDefault();
        ZonedDateTime dailyStart = ZonedDateTime.of(LocalDateTime.of(date, START_TIME), localZone);
        ZonedDateTime dailyEnd = ZonedDateTime.of(LocalDateTime.of(date, END_TIME), localZone);
        DateTimeFormatter offsetFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        double low = Double.POSITIVE_INFINITY; // Used for calculating high and low
        double high = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < conditionsValues.length(); i++) {
            JSONObject entry = conditionsValues.getJSONObject(i);
            String validTimeRaw = entry.getString("validTime");
            String[] parts = validTimeRaw.split("/");
            if (parts.length != 2) {
                System.err.println("Invalid validTime format: " + validTimeRaw);
                continue;
            }
            String startStr = parts[0];
            String durationStr = parts[1];
            ZonedDateTime startZoned;
            try {
                startZoned = ZonedDateTime.parse(startStr, offsetFormatter);
            } catch (Exception e) {
                System.err.println("Error parsing start time: " + startStr + " - " + e.getMessage());
                continue;
            }
            ZonedDateTime endZoned;
            try {
                endZoned = startZoned.plus(parseDuration(durationStr));
            } catch (Exception e) {
                System.err.println("Error parsing duration: " + durationStr + " - " + e.getMessage());
                continue;
            }
            ZonedDateTime localStart = startZoned.withZoneSameInstant(localZone);
            ZonedDateTime localEnd = endZoned.withZoneSameInstant(localZone);
            double value;
            try {
                value = entry.getDouble("value");
            } catch (Exception e) {
                try {
                    value = entry.getDouble("value");
                } catch (Exception ex) {
                    System.err.println("Error parsing 'value': " + ex.getMessage());
                    continue;
                }
            }
            if (rawUom.toLowerCase().contains("degf")) {
                double original = value;
                value = (value - 32.0) * 5.0 / 9.0;
                System.err.println("Converting from F to C for " + condition + ": " + original + " -> " + value);
            }
            ZonedDateTime overlapStart = localStart.isBefore(dailyStart) ? dailyStart : localStart;
            ZonedDateTime overlapEnd = localEnd.isAfter(dailyEnd) ? dailyEnd : localEnd;
            if (overlapStart.isBefore(overlapEnd)) {
                if (value > high) {
                    high = value;
                }
                if (value < low) {
                    low = value;
                }
                double overlapHours = Duration.between(overlapStart, overlapEnd).toMinutes() / 60.0;
                totalWeightedValue += value * overlapHours;
                totalHoursInRange += overlapHours;
            }
        }
        if (totalHoursInRange == 0) {
            System.err.println("No valid data found for condition '" + condition + "' on date " + date);
            results[0] = -9999;
            return results;
        }
        double average = totalWeightedValue / totalHoursInRange;
        if (high == Double.NEGATIVE_INFINITY) {
            high = average;
        }
        if (low == Double.POSITIVE_INFINITY) {
            low = average;
        }
        System.err.println("Successfully calculated average for condition '" + condition + "': " + average);
        results[0] = average;
        results[1] = high;
        results[2] = low;
        return results;
    }

    private Duration parseDuration(String durationStr) {
        try {
            return Duration.parse(durationStr);
        } catch (Exception ex) {
            int dayIndex = durationStr.indexOf('D');
            if (dayIndex != -1) {
                String daySubstring = durationStr.substring(1, dayIndex);
                int days = Integer.parseInt(daySubstring);
                String remainder = durationStr.substring(dayIndex + 1);
                Duration dayDur = Duration.ofDays(days);
                if (remainder.isEmpty()) {
                    return dayDur;
                } else {
                    Duration timeDur = Duration.parse("P" + remainder);
                    return dayDur.plus(timeDur);
                }
            }
            throw new IllegalArgumentException("Cannot parse duration: " + durationStr, ex);
        }
    }

    private static JSONObject fetchJson(String urlString) throws Exception {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return new JSONObject(content.toString());
    }
}