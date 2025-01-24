package TrailWeatherScoutBackend;

import org.json.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/forecaster")
public class ForecasterController {

    private final Forecaster forecaster = new Forecaster();


    // Add a new trailhead
    @PostMapping("/addTrailhead")
    public String addTrailhead(@RequestBody List<Map<String, Object>> trailheadJson) {
        System.out.println("Received payload: " + trailheadJson); // Debugging
        try {
            // Convert the List<Map<String, Object>> into JSONArray if necessary
            JSONArray jsonArray = new JSONArray(trailheadJson);
            boolean result = forecaster.addTrailhead(jsonArray);
            return result ? "Trailhead added successfully" : "Failed to add trailhead";
        } catch (Exception e) {
            System.err.println("Error processing trailhead JSON: " + e.getMessage());
            return "Error processing trailhead JSON: " + e.getMessage();
        }
    }

    // Calculate weather conditions for a specific date
    @GetMapping("/calculateConditions")
    public String calculateConditions(@RequestParam String date) {
        System.out.println("done");
        try {
            LocalDate requestedDate = LocalDate.parse(date);
            boolean result = forecaster.calculateConditions(requestedDate);
            return result ? "Weather conditions calculated successfully for " + date
                    : "Failed to calculate weather conditions or date out of range.";
        } catch (DateTimeParseException e) {
            return "Invalid date format. Please use YYYY-MM-DD.";
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

    // Get the top recommendation
    @GetMapping("/recommendation")
    public Map<String, Object> getRecommendation() {
        return forecaster.getRecommendation();
    }

    // Get the full list of recommendations
    @GetMapping("/recommendationList")
    public List<JSONObject> getRecommendationList() {
        return forecaster.getRecommendationList();
    }

    @GetMapping("/Reset")
    public void Reset() {
        forecaster.Reset();
    }

    @GetMapping("/Bootup")
    public String Bootup() {
       forecaster.Bootup();
        return "Booting!";
    }
}

