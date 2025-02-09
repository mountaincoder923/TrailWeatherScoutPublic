## 🏔️ TrailWeatherScout ##


As an avid hiker, I wanted a way to avoid having to compare weather across multiple spots to find the spot with best weather, so I created TrailWeatherScout.

TrailWeatherScout is a web app that helps you find the best  location based on live weather data. Instead of manually checking forecasts for multiple locations, let TrailWeatherScout automatically compare them and pick the best spot for your next outdoor adventure!


🌐 Try It Here **[TrailWeatherScout.com](https://trailweatherscout.com)

---

**✨ Features**

Automatically fetches & compares weather conditions for multiple locations  

Uses National Weather Service (NWS) API for real-time accuracy up to 3 square km.

Supports up to 15 locations at once (adjustable for local versions) 

Google Maps integration for easy trailhead selection

Insert manually by coordinates for any point within the United States down to 10m.

---

**🚀 How It Works**

TrailWeatherScout consists of two main parts:

1️⃣ Backend (Java + Spring Boot)

Fetches weather conditions using NWS API

Parses and compares weather data for multiple locations

Returns the best location as JSON

**Note**: While the backend in this repository is implemented using Spring Boot for local use, the actual deployed version runs as an AWS Lambda function for scalability.


2️⃣ Frontend (JavaScript + Google Maps API)

Lets users input trailheads 

Converts locations into coordinates

Performs validation 

Calls the backend API and displays the results

---

**🛠️ Installation & Setup**

1️⃣ Clone the Repository

``` bash
git clone https://github.com/mountaincoder923/TrailWeatherScoutPublic.git
cd TrailWeatherScoutPublic
```

2️⃣ Backend Setup (Spring Boot)

Update CORS settings in WebConfig.java if needed.

Build and run the backend:

cd TrailWeatherScoutBackend
./mvnw spring-boot:run

By default, the backend runs on localhost:8080.

3️⃣ Frontend Setup

Update the API URL in script.js if needed.

Insert your own Google Maps API Key in script.js.

Open index.html in a browser to start using TrailWeatherScout!

---

**📂 Project Structure**

📦 TrailWeatherScoutPublic  
├──  README.md | Project documentation  
├── 📂 TrailWeatherScoutBackend   | Java Spring Boot Backend  
├── 📂 TrailWeatherScoutFrontend  | HTML, JS, and UI components  
└──  LICENSE.md         |  Licensing details

---


📋 Requirements

💡 Spring Boot (Java Backend)

💡Google Maps API Key (for trailhead selection)

💡 Maven (for backend dependency management)

---

📚 License

This project is licensed under a custom license (see LICENSE.md).Summary:

You may use, modify, and learn from the code for personal or educational purposes.

Redistribution, commercialization, or public deployment is not allowed.

---

**⚠️ Disclaimer**

TrailWeatherScout provides weather forecasts, but accuracy is never guaranteed. Always double check conditions and exercise due caution before heading out!

