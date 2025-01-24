TrailWeatherScout / TrailWeatherScout.com

## Description
As an avid hiker, I wanted a way to avoid having to compare weather across multiple spots to find the one with the best weather on the day I wanted to go hiking, so I created TrailWeatherScout and TrailWeatherScout.com 

TrailWeatherScout (TWS from hereon) is a tool that given a list of locations, can automatically fetch, parse and compare to return the location best weather conditons from the options you provided. 

A high level overview of how it works: Forecaster contains a list of trailheads and weather conditions. Each trailhead contains its coordinates and a method to fetch and calculate conditions from the NWS.
Forecaster can have each trailhead return the conditions of a provided day as a WeatherConditions object. Forecaster then compares the WeatherConditions and returns it as a JSON to the frontend. 

TWSFrontend simply takes in your input, translates it into a set of coordinates and passes it to TWSBackend. 

The TWSFrontend then renders the JSON from backend 


## Features
-  Compares and produces local weather results. 
- Typical 3 square Km locality. 
- Can automatically compare up to 15 locations. (Local versions can be adjusted for more). 

## How to Use
1. Clone the repository:
2. Change CORS in WebConfig.java to allow whatever you're running your frontend on to connect to the backend if needed. Currently, WebcConfig.java allows Webstorm preview to connect. 
3. Adjust API in script.js if needed. By default, its set the localhost:8080. 
4. In script.js, insert your own API key for Google Maps Autocomplete. 

## Requirements
- Springboot 
- JSON
- Maven
- Google Maps API Key

## License
This project is licensed under a custom license (see `LICENSE.txt`).  
Summary:
- You can use, modify, and learn from the code for personal or educational purposes.
- **You cannot redistribute, commercialize, or deploy it on a public server**.


## Disclaimer
Trailweatherscout.com and TrailWeatherScout provides weather forecasts, but makes no promises of accuracy and takes no responsbility for decisions you make based off the data we provide. Always double check conditions and exercise caution. 
