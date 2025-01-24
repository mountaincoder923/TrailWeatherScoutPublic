package TrailWeatherScoutBackend;


public class WeatherCondition implements Comparable<WeatherCondition> {
    private final double temperature;
    private final double highTemperature;
    private final double lowTemperature;
    private final double skyCover;
    private final double precipitation;
    private final double feelsLike;
    private final Trailhead Trailhead;


    public WeatherCondition(Trailhead trailhead, double temperature, double highTemperature, double lowTemperature, double skyCover,
                            double precipitation,  double feelsLike) {
        this.Trailhead = trailhead;
        this.temperature = Math.round(temperature * 100.00) / 100.00;
        this.highTemperature = Math.round(highTemperature * 100.00) / 100.00;
        this.lowTemperature = Math.round(lowTemperature * 100.00) / 100.00;
        this.skyCover = Math.round(skyCover * 100.00) / 100.00;
        this.precipitation = Math.round(precipitation * 100.00) / 100.00;
        this.feelsLike = Math.round(feelsLike * 100.00) / 100.00; // Feels like temp (include wind chill)

    }

    public double getTemperature() {
        return temperature;
    }

    public double getHighTemperature() {
        return highTemperature;
    }
    public double getLowTemperature() {
        return lowTemperature;
    }

    public double getSkyCover() {
        return skyCover;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public double getFeelsLike() {
        return feelsLike;
    }


    public Trailhead getTrailhead() {
        return Trailhead;
    }

    @Override
    public int compareTo(WeatherCondition other) {
        // Default: Sort by skyCover in ascending order
        return Double.compare(this.skyCover, other.skyCover);
    }

    @Override
    public String toString() {
        return "Condition{" +
                "Trailhead=" + this.Trailhead.getName() +
                ", Temperature=" + temperature + "°F" +
                ", HighTemperature=" + highTemperature +
                ", LowTemperature=" + lowTemperature +
                ", Sky Cover=" + skyCover + "%" +
                "Precipitation=" + precipitation + "%" +
                ", Feels Like=" + feelsLike + "°F" +
                '}';
    }
}
