let trails = [];
let selectedPlace = null;
let autocomplete;
let sessionToken;
let date = null;
let currentUnits = "C";
let bestTrail = {
    recommendedTrail: "",
    skyCover: 0,
    highTemperature: 0,
    lowTemperature: 0,
    temperature: 0,
    feels_like: 0,
    precipitation: 0,
};
let skyCoverTerms = new Map([
    [12.5, "☀️ Sunny"],
    [37.5, "🌤️ Mostly Sunny"],
    [62.5, "⛅️️ Partly Sunny"],
    [87.5, "🌥️ Mostly Cloudy"],
    [100.00, "☁️ Cloudy"],
]);

// Load saved trails from cookies when page loads
window.onload = function () {
    loadRandomBackground();
    loadTrailsFromCookies();
    fetch('http://localhost:8080/api/Bootup', {
        method: 'GET',
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text();
        })
        .then(data => {
            console.log('Response from /Boot-up:', data);
        })
        .catch(error => {
            console.error('Error warming up Lambda:', error);
        });
    checkWeatherDataSourceStatus();
};

document.addEventListener("DOMContentLoaded", () => {
    const toggleButton = document.querySelector("#toggleButton");
    const instructions = document.querySelector("#instructions");

    toggleButton.addEventListener("click", () => {
        if (instructions.style.display === "none" || instructions.style.display === "") {
            instructions.style.display = "block";
        } else {
            instructions.style.display = "none";
        }
    });
});

const googleMapsApiKey = "API-KEY-HERE";

function loadGoogleMapsApi() {
    return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = `https://maps.googleapis.com/maps/api/js?key=${googleMapsApiKey}&libraries=places`;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Failed to load Google Maps API"));
        document.head.appendChild(script);
    });
}

function initAutocomplete() {
    const input = document.getElementById("trailName");
    sessionToken = new google.maps.places.AutocompleteSessionToken();
    autocomplete = new google.maps.places.Autocomplete(input, {
        fields: ["name", "geometry"],
        componentRestrictions: { country: "us" },
        sessionToken: sessionToken,
    });
    google.maps.event.addListener(autocomplete, "place_changed", () => {
        const place = autocomplete.getPlace();
        if (place && place.geometry && place.geometry.location) {
            selectedPlace = {
                name: place.name,
                longitude: place.geometry.location.lng(),
                latitude: place.geometry.location.lat(),
                custom: false,
            };
            sessionToken = new google.maps.places.AutocompleteSessionToken();
            autocomplete.setOptions({ sessionToken });
        } else {
            selectedPlace = null;
        }
    });
}

loadGoogleMapsApi()
    .then(() => {
        initAutocomplete();
    })
    .catch((error) => {
        console.error("Error loading Google Maps API:", error);
    });

// Ensure Google Maps API is loaded before initializing
window.addEventListener("load", () => {
    if (typeof google !== "undefined" && google.maps && google.maps.places) {
        initAutocomplete();
        autocomplete.addListener("place_changed", () => {
           addTrail();
        });
    }
});


// Add trail manually by coordinates
document.getElementById("addCoordinates").addEventListener("click", function () {
    let name = document.getElementById("trailManualName").value.trim();
    let manualLat = document.getElementById("manualLatitude").value.trim();
    let manualLong = document.getElementById("manualLongitude").value.trim();
    if (checkForMaliciousInput(name) || checkForMaliciousInput(manualLat) || checkForMaliciousInput(manualLong)) {
        alert("🚨 Whoa there, hacker! Nice try, but I'm onto you. 🕵️‍♂️ No funny business allowed!");
        document.getElementById("trailManualName").value = "Bad boy!";
        document.getElementById("manualLatitude").value = "Play nice!";
        document.getElementById("manualLongitude").value = "Don't try SQL inject or XSS";
        return;
    }
    if (name.length > 15 || manualLat.length > 10 || manualLong.length > 10) {
        name = name.substring(0, 15);
        manualLat = manualLat.substring(0, 10);
        manualLong = manualLong.substring(0, 10);
    }
    if (name === "" || manualLat === "" || manualLong === "") {
        alert("Please enter a trail name, latitude, and longitude.");
        return;
    }
    if (!isValidUSCoordinates(manualLat, manualLong)) {
        alert("Please enter  valid US coordinates.");
        return;
    }
    manualLat = limitToFourDecimals(manualLat);
    manualLong = limitToFourDecimals(manualLong);
    const customTrail = {
        name: name,
        latitude: parseFloat(manualLat),
        longitude: parseFloat(manualLong),
        custom: true
    };
    for (const trail of trails) {
        try{
            if (trail.latitude === parseFloat(manualLat) && trail.longitude === parseFloat(manualLong)) {
                alert("Trail already added!")
                return;
            }
        }
        catch(error){
            alert("Error in adding trail. Duplicate check failed. Please try again" + error.message);
            return;
        }
    }
    if (trails.length > 16){
        alert("Maximum number of trails reached!");
        return;
    }
    trails.push(customTrail);
    saveTrailsToCookies();
    document.getElementById("trailManualName").value = "";
    document.getElementById("manualLatitude").value = "";
    document.getElementById("manualLongitude").value = "";
    updateTrailList();
});

document.addEventListener("DOMContentLoaded", () => {
    const forecastDateInput = document.getElementById("forecastDate");
    const today = new Date();
    const nextWeek = new Date();
    nextWeek.setDate(today.getDate() + 7);
    const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    };
    forecastDateInput.min = formatDate(today);
    forecastDateInput.max = formatDate(nextWeek);
});

const forecastDateInput = document.getElementById("forecastDate");
forecastDateInput.addEventListener("change", (event) => {
    date = event.target.value;
});

document.addEventListener("DOMContentLoaded", () => {
    const findBestTrailButton = document.getElementById("findBestTrail");
    let isCooldown = false;
    findBestTrailButton.addEventListener("click", async () => {
        if (trails.length === 0) {
            alert("Please add at least on trail.");
            return;
        }

        if (!date) {
            const dateLabel = document.querySelector(".date-label");
             dateLabel.style.color = "red";
            setTimeout(() => {
                dateLabel.style.color = "black";
            }, 700);
            alert("Please select a date first.");
            return;
        }
        if (isCooldown) {
            alert("Please slow down your requests. 1 second before requests is enforced!")
            return;
        }
        const spinner = document.getElementById("loadingSpinner");
        const button = document.getElementById("findBestTrail");
        try {
            spinner.style.display = "block";
            button.disabled = true;
            await sendTrailsToBackend();
            await calculateConditions(date);
            await fetchRecommendation();
            await processConversions();
            await displayRecommendation();
            await reset();
            const RecommendationDisplay = document.getElementById("bestTrailContainer");
            RecommendationDisplay.scrollIntoView({ behavior: "smooth" });

        } catch (error) {
            console.error("Error processing best trail recommendation:" + error.message);
            alert("Failed to process trail recommendation. Please try again." + error.message);
        } finally {
            spinner.style.display = "none";
            document.getElementById("buttonText").textContent = "🌟 Find Best Trail 🌟";
            findBestTrailButton.disabled = false;
            setTimeout(() => {
                isCooldown = false;
            }, 1000);
        }
    });
});

document.addEventListener("DOMContentLoaded", () => {
    const unitsToggle = document.querySelector(".unitsToggle");
    if (!unitsToggle) {
        console.error("unitsToggle element not found!");
        return;
    }
    unitsToggle.textContent = "Units: °C. Click to Convert.";
    unitsToggle.addEventListener("click", () => {
        processConversions();
        displayRecommendation();
    });
});

function updateTrailList() {
    const trailList = document.getElementById("trailList");

    // Generate list items dynamically
    trailList.innerHTML = trails.map((trail, index) =>
        `<li class="trail-item">
            <span class="trail-text">
                ${trail.custom ? `📍 ${trail.name} (${trail.latitude}, ${trail.longitude}) <span class="custom-marker">(Custom)</span>` : trail.name}
            </span>
            <button class="remove-btn" data-index="${index}">❌</button>
        </li>`
    ).join("");

    // Attach event listeners to all remove buttons
    const buttons = document.querySelectorAll(".remove-btn");
    buttons.forEach(button => {
        button.addEventListener("click", (event) => {
            const index = event.target.getAttribute("data-index"); // Get the index from data attribute
            removeTrail(index); // Call the remove function
        });
    });
}

function removeTrail(index) {
    trails.splice(index, 1);
    updateTrailList();
    saveTrailsToCookies();
}

function saveTrailsToCookies() {
    const expires = new Date();
    expires.setTime(expires.getTime() + 7 * 24 * 60 * 60 * 1000);
    document.cookie = `savedTrails=${encodeURIComponent(JSON.stringify(trails))}; expires=${expires.toUTCString()}; path=/`;
}

function loadTrailsFromCookies() {
    const cookies = document.cookie.split("; ");
    for (const cookie of cookies) {
        const [name, value] = cookie.split("=");
        if (name === "savedTrails") {
            try {
                trails = JSON.parse(decodeURIComponent(value));
                updateTrailList();
            } catch (error) {
                console.error("Failed to load trails from cookies:", error);
                trails = [];
            }
        }
    }
}

function toggleManualEntry() {
    const manualBox = document.getElementById("manualBox");
    manualBox.style.display = (manualBox.style.display === "block") ? "none" : "block";
}

function toggleTechDetails() {
    const techBox = document.getElementById("techBox");
    techBox.style.display = (techBox.style.display === "block") ? "none" : "block";
}

function addTrail(){  if (!selectedPlace) {
    alert("Please select a valid location from the autocomplete list or use manual entry.");
    return;
}
    if (trails.some(trail => trail.latitude === selectedPlace.latitude && trail.longitude === selectedPlace.longitude)) {
        alert("Location already added!");
        return;
    }
    if (trails.length > 16){
        alert("Maximum number of trails reached!");
        return;
    }
    trails.push(selectedPlace);
    updateTrailList();
    saveTrailsToCookies();
    document.getElementById("trailName").value = "";
    selectedPlace = null;
}

function packageTrails() {
    const truncateToFourDecimals = (num) => {
        if (isNaN(num)) {
            console.error("Invalid number:", num);
            return "0";
        }
        return String(Math.trunc(num * 10000) / 10000);
    };
    const packagedTrails = [];
    for (const trail of trails) {
        packagedTrails.push({
            Name: String(trail.name),
            LatCord: truncateToFourDecimals(trail.latitude),
            LongCord: truncateToFourDecimals(trail.longitude),
            Custom: String(trail.custom),
        });
    }
    return packagedTrails;
}

async function sendTrailsToBackend() {
    const packagedTrails = packageTrails();
    try {
        const response = await fetch('http://localhost:8080/api/forecaster/addTrailhead', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(packagedTrails),
        });
        if (response.ok) {
            console.log("Trails sent to backend successfully.");
        } else {
            console.error("Failed to send trails to backend:", await response.text());
            alert("Failed to send trails to backend successfully.");
        }
    } catch (error) {
        console.error("Error sending trails to backend:", error);
        alert("Error in sending to backed" + error.message);
    }
}

async function calculateConditions(date) {
    try {
        const response = await fetch(`http://localhost:8080/api/forecaster/calculateConditions?date=${date}`, {
            method: 'GET',
        });
        if (!response.ok) {
            alert(`HTTP error! status: ${response.status} Please try again!`);
        }
    } catch (error) {
        console.error("Error calculating conditions:", error);
        alert("An error occurred while calculating conditions: " + error.message);
    }
}

async function fetchRecommendation() {
    try {
        const response = await fetch('http://localhost:8080/api/forecaster/recommendation');
        if (!response.ok) {
            alert(`HTTP error! status: ${response.status} Please try again!`);
        }
        try {
            bestTrail = await response.json();
            currentUnits = "C";
        } catch (error) {
            console.error("Failed to fetch recommendation: ", error);
            alert("Failed to fetch recommendation: " + error.message);
        }
    } catch (error) {
        alert("Error in fetching recommendation: " + error);
    }
}

function displayRecommendation() {
    document.getElementById("bestTrailContainer").style.display = "block";
    document.getElementById("bestTrailTitle").textContent = bestTrail.recommendedTrail;
    document.getElementById("bestTrailSkycover").textContent = getSkyCoverTerm(bestTrail.skyCover) + ",    "
        + bestTrail.skyCover + "% skycover.";
    document.getElementById("bestTrailTemperature").textContent = "High: " + bestTrail.highTemperature + " Low: "
        + bestTrail.lowTemperature + " Avg: " + bestTrail.temperature;
    document.getElementById("bestTrailFeelsLike").textContent = bestTrail.feels_like;
    document.getElementById("bestTrailPrecipitation").textContent = bestTrail.precipitation + "% Chance";
}

async function reset() {
    try {
        const response = await fetch('http://localhost:8080/api/forecaster/Reset', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });
        if (response.ok) {
            console.log("Reset");
        } else {
            console.error("Failed to reset:", await response.text());
            alert("Reset Failed");
        }
    } catch (error) {
        console.error("Error Resetting:", error);
        alert("Error in Resetting" + error.message);
    }
}

function celsiusToFahrenheit(celsius) {
    return (((parseFloat(celsius) * 9 / 5) + 32)).toFixed(1).toString();
}

function FahrenheitToCelsius(Fahrenheit) {
    return (((parseFloat(Fahrenheit) - 32) * 5 / 9).toFixed(1)).toString();
}

function processConversions() {
    const unitsToggle = document.querySelector(".unitsToggle");
    if (currentUnits === "C") {
        bestTrail.highTemperature = celsiusToFahrenheit(bestTrail.highTemperature) + "°F";
        bestTrail.lowTemperature = celsiusToFahrenheit(bestTrail.lowTemperature) + "°F";
        bestTrail.temperature = celsiusToFahrenheit(bestTrail.temperature) + "°F";
        bestTrail.feels_like = celsiusToFahrenheit(bestTrail.feels_like) + "°F";
        unitsToggle.textContent = "Units: °F. Click to Convert.";
        currentUnits = "F";
    } else if (currentUnits === "F") {
        bestTrail.highTemperature = FahrenheitToCelsius(bestTrail.highTemperature) + "°C";
        bestTrail.lowTemperature = FahrenheitToCelsius(bestTrail.lowTemperature)  + "°C";
        bestTrail.temperature = FahrenheitToCelsius(bestTrail.temperature)  + "°C";
        bestTrail.feels_like = FahrenheitToCelsius(bestTrail.feels_like)  + "°C";
        unitsToggle.textContent = "Units: °C. Click to Convert.";
        currentUnits = "C";
    }
}

function getSkyCoverTerm(skycoverPercentage) {
    for (let [threshold, term] of skyCoverTerms) {
        if (Number(skycoverPercentage) <= threshold) {
            return term;
        }
    }
    return "Unknown";
}

function isValidUSCoordinates(latStr, lngStr) {
    const US_BOUNDS = {
        minLat: 14.0,
        maxLat: 71.5,
        minLng: -179.15,
        maxLng: -64.57
    };
    return !isNaN(parseFloat(latStr)) &&
        !isNaN(parseFloat(lngStr)) &&
        parseFloat(latStr) >= US_BOUNDS.minLat &&
        parseFloat(latStr) <= US_BOUNDS.maxLat &&
        parseFloat(lngStr) >= US_BOUNDS.minLng &&
        parseFloat(lngStr) <= US_BOUNDS.maxLng;
}

function limitToFourDecimals(input) {
    let value = parseFloat(input);
    if (isNaN(value)) {
        throw new Error("Invalid input: not a valid number.");
    }
    value = Math.round(value * 10000) / 10000;
    return value.toFixed(4);
}

function loadRandomBackground() {
    const index = Math.floor(Math.random() * 5) + 1;
    const imageUrl = `images/background${index}.WebP`;
    document.body.style.background = 'linear-gradient(to bottom, darkblue, lightblue, pink, orange, darkorange)';
    document.body.style.backgroundSize = 'cover';
    document.body.style.backgroundPosition = 'center';
    document.body.style.backgroundRepeat = 'no-repeat';
    document.body.style.backgroundAttachment = 'fixed';
    const tempImage = new Image();
    tempImage.onload = function () {
        document.body.style.backgroundImage = `url("${imageUrl}")`;
    };
    tempImage.onerror = function () {
        console.warn(`Failed to load image: ${imageUrl}. Retaining gradient background.`);
    };
    tempImage.src = imageUrl;
}

function checkForMaliciousInput(inputText) {
    // Allow null or empty input
    if (!inputText) return false;

    // Block potentially harmful HTML/JS injection patterns
    const xssRegex = /<script|<\/script>|javascript:|on\w+=/i;

    // Block dangerous SQL keywords only if surrounded by SQL-like syntax
    const sqlRegex = /\b(SELECT|DROP|INSERT|DELETE|UPDATE|UNION|ALTER|TRUNCATE)\b.*;/i;

    // Look for suspicious SQL injection patterns (quotes and semicolons together)
    const suspiciousOpsRegex = /['"]{2,}|['"];|--/;

    return (
        xssRegex.test(inputText) ||
        sqlRegex.test(inputText) ||
        suspiciousOpsRegex.test(inputText)
    );
}

async function checkWeatherDataSourceStatus() {
    const statusDiv = document.getElementById("weatherDataStatus");
    try {
        const response = await fetch("https://api.weather.gov/alerts");
        if (response.ok) {
            statusDiv.style.display = "none";
        } else {
            statusDiv.textContent = "Weather Data Source: OFFLINE. Forecasts may be temporarily unavailable.";
            statusDiv.style.backgroundColor = "red";
            statusDiv.style.color = "white";
            statusDiv.style.border = "1px solid darkred";
            statusDiv.style.fontSize = "16px";
            statusDiv.style.display = "block";
            alert("Weather Data Source might be offline. Check box in bottom right for status.");
        }
    } catch (error) {
        statusDiv.textContent = "Weather Data Source: OFFLINE. Forecasts may be temporarily unavailable.";
        statusDiv.style.backgroundColor = "red";
        statusDiv.style.color = "white";
        statusDiv.style.border = "1px solid darkred";
        statusDiv.style.fontSize = "16px";
        statusDiv.style.display = "block";
        alert("Weather Data Source might be offline.");
    }
}
