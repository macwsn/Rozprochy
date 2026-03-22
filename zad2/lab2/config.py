import os
from typing import Any

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = "gemini-2.5-flash"
#GEMINI_MODEL = "gemini-2.5-flash-lite"
GEMINI_API_URL = (
    f"https://generativelanguage.googleapis.com/v1beta/models/"
    f"{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"
)

REQUEST_TIMEOUT = 20 

PUBLIC_APIS: dict[str, dict[str, Any]] = {
    "open_meteo": {
        "name": "Open-Meteo Weather",
        "description": "Current and forecast weather data worldwide.",
        "base_url": "https://api.open-meteo.com/v1/forecast",
        "example_topics": ["weather", "temperature", "rain", "wind", "forecast"],
    },
    "open_library": {
        "name": "Open Library Books",
        "description": "Search millions of books and authors.",
        "base_url": "https://openlibrary.org/search.json",
        "example_topics": ["books", "literature", "authors", "reading", "novels"],
    },
    "restcountries": {
        "name": "REST Countries",
        "description": "Data about countries: population, area, languages, currencies.",
        "base_url": "https://restcountries.com/v3.1",
        "example_topics": ["countries", "geography", "population", "flags", "nations"],
    },
    "dog_ceo": {
        "name": "Dog CEO / Dog API",
        "description": "Random dog images by breed.",
        "base_url": "https://dog.ceo/api",
        "example_topics": ["dogs", "pets", "animals", "breeds", "puppy"],
    },
    "catfact": {
        "name": "Cat Facts",
        "description": "Random interesting facts about cats.",
        "base_url": "https://catfact.ninja",
        "example_topics": ["cats", "pets", "animals", "feline", "kitten"],
    },
    "uselessfacts": {
        "name": "Useless Facts",
        "description": "Returns a random interesting fact.",
        "base_url": "https://uselessfacts.jsph.pl/api/v2/facts/random",
        "example_topics": ["trivia", "facts", "random", "fun"],
    },
    "numbersapi": {
        "name": "Numbers API",
        "description": "Mathematical and historical facts about numbers.",
        "base_url": "http://numbersapi.com",
        "example_topics": ["numbers", "math", "mathematics", "date", "trivia"],
    },
    "bored": {
        "name": "Bored API",
        "description": "Suggests random activities when bored.",
        "base_url": "https://bored-api.appbrewery.com/random",
        "example_topics": ["bored", "activities", "hobby", "free time", "fun"],
    },
    "ipapi": {
        "name": "IP-API Geolocation",
        "description": "Geolocation data for any public IP address.",
        "base_url": "http://ip-api.com/json",
        "example_topics": ["ip", "geolocation", "location", "internet", "network"],
    },
    "icanhazdadjoke": {
        "name": "icanhazdadjoke",
        "description": "Random or searched dad jokes.",
        "base_url": "https://icanhazdadjoke.com",
        "example_topics": ["jokes", "humor", "dad jokes", "funny", "comedy"],
    },
    "open_trivia": {
        "name": "Open Trivia Database",
        "description": "Trivia questions across many categories.",
        "base_url": "https://opentdb.com/api.php",
        "example_topics": ["trivia", "quiz", "questions", "knowledge", "game"],
    },
    "cocktaildb": {
        "name": "TheCocktailDB",
        "description": "Cocktail recipes: ingredients and instructions.",
        "base_url": "https://www.thecocktaildb.com/api/json/v1/1",
        "example_topics": ["cocktails", "drinks", "alcohol", "recipes", "mixology"],
    },
    "themealdb": {
        "name": "TheMealDB",
        "description": "Global meal and food recipes.",
        "base_url": "https://www.themealdb.com/api/json/v1/1",
        "example_topics": ["food", "recipes", "cooking", "meals", "cuisine"],
    },
}
