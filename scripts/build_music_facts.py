#!/usr/bin/env python3
"""Builds the bundled "this day in music" assets from the researched source file.

Input:  docs/music-facts/research.json   (one entry per MM-DD: lines, place, tie-in, sources)
Output: app/src/main/assets/music_facts.json        (unchanged format: {"MM-DD": [lines]})
        app/src/main/assets/music_fact_places.json  (footage sets + per-date place/fallback chain)

Footage sets list shipped aerial clips (Apple tvOS, Amazon Fire OS, Community manifests in
app/src/main/res/raw) whose OWN manifest entry is that place. Every token is checked against the
manifests so a set can never pick up a clip of somewhere else.

Fallback for a fact place with no footage: the nearest footage city within FALLBACK_KM, then the
general city-at-night set. Fallback clips keep their normal caption; only a direct match gets the
"<City>, <tie-in>" caption.

Usage: python3 scripts/build_music_facts.py
"""

import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "app/src/main/res/raw")
ASSETS = os.path.join(ROOT, "app/src/main/assets")
RESEARCH = os.path.join(ROOT, "docs/music-facts/research.json")

FALLBACK_KM = 650
CITY_NIGHT = "city-night"

# key: label shown in the tie-in caption, coordinates, places that count as this city,
# and clip tokens with the manifest location label each token must carry.
FOOTAGE = {
    "london": {
        "label": "London",
        "at": (51.507, -0.128),
        "aliases": ["London", "Westminster", "Camden", "Soho", "Wembley", "St John's Wood", "Marylebone"],
        "clips": {
            "comp_L004_C011": "London",
            "comp_L007_C007": "London",
            "comp_L010_C006": "London",
            "comp_L012_": "London",
            "video_inspire_uk_london_": "London",
        },
    },
    "new-york": {
        "label": "New York",
        "at": (40.758, -73.986),
        "aliases": ["New York", "New York City", "Manhattan", "Brooklyn", "Harlem", "Bronx", "The Bronx", "Queens"],
        "clips": {
            "comp_N003_C006": "New York",
            "comp_N008_C003": "New York",
            "comp_N008_C009": "New York",
            "comp_N013_C004": "New York",
            "content-0e2c8ff3-1f28-4c59-bc08-9d813e695100": "The Oculus Transportation Hub",
        },
    },
    "los-angeles": {
        "label": "Los Angeles",
        "at": (34.052, -118.244),
        "aliases": ["Los Angeles", "Hollywood", "North Hollywood", "Van Nuys", "Sherman Oaks", "Studio City",
                    "Encino", "Silver Lake", "Echo Park", "Laurel Canyon", "Westwood", "Bel Air", "Venice"],
        # The Santa Monica beach clip is left out: Santa Monica is its own city.
        "clips": {
            "comp_LA_A005_C009": "Los Angeles",
            "comp_LA_A006_C004": "Los Angeles",
            "comp_LA_A006_C008": "Los Angeles",
            "comp_LA_A009_C009": "Los Angeles",
            "comp_LA_A011_C003": "Los Angeles",
        },
    },
    "san-francisco": {
        "label": "San Francisco",
        "at": (37.775, -122.419),
        "aliases": ["San Francisco"],
        "clips": {
            "comp_A006_C003": "San Francisco",
            "comp_A007_C017": "San Francisco",
            "comp_A008_C007": "San Francisco",
            "comp_A012_C014": "San Francisco",
            "comp_A013_C012": "San Francisco",
            "comp_A015_C018": "San Francisco",
            "comp_1223LV_FLARE": "San Francisco",
            "content-dc50fb3c-701a-4c6b-b3e0-e8f82d8b5142": "Clouds Over The Golden Gate Bridge",
            "content-cef6e713-0c1b-4eaa-bb5b-21f156046711": "Bay Bridge over Treasure Island",
            "video_inspire_sanfrancisco_castro_": "San Francisco",
        },
    },
    "las-vegas": {
        "label": "Las Vegas",
        "at": (36.17, -115.14),
        "aliases": ["Las Vegas", "Paradise"],
        "clips": {"video_inspire_nevada_lasvegas_": "Las Vegas"},
    },
    "miami": {
        "label": "Miami",
        "at": (25.762, -80.192),
        "aliases": ["Miami"],
        "clips": {"video_inspire_florida_miami_": "Miami"},
    },
    "austin": {
        "label": "Austin",
        "at": (30.267, -97.743),
        "aliases": ["Austin"],
        "clips": {"video_inspire_texas_austin_": "Austin"},
    },
    "portland": {
        "label": "Portland",
        "at": (45.515, -122.679),
        "aliases": ["Portland"],
        "region": "Oregon",
        "clips": {"video_inspire_oregon_portland_": "Portland"},
    },
    "edinburgh": {
        "label": "Edinburgh",
        "at": (55.953, -3.188),
        "aliases": ["Edinburgh"],
        "clips": {"video_inspire_uk_edinburgh_": "Edinburgh"},
    },
    "hong-kong": {
        "label": "Hong Kong",
        "at": (22.319, 114.169),
        "aliases": ["Hong Kong"],
        "clips": {
            "HK_B005_C011": "Hong Kong",
            "HK_H004_C008": "Hong Kong",
            "HK_H004_C010": "Hong Kong",
            "HK_H004_C013": "Hong Kong",
        },
    },
    "dubai": {
        "label": "Dubai",
        "at": (25.204, 55.271),
        "aliases": ["Dubai"],
        "clips": {
            "comp_DB_D001_C001": "Dubai",
            "comp_DB_D001_C005": "Dubai",
            "comp_DB_D002_C003": "Dubai",
            "comp_DB_D008_C010": "Dubai",
            "comp_DB_D011_C010": "Dubai",
        },
    },
    "sydney": {
        "label": "Sydney",
        "at": (-33.857, 151.215),
        "aliases": ["Sydney"],
        "clips": {"content-0d9639cb-46b1-44b1-952b-abd80507f0b5": "Sunset at the Sydney Opera House"},
    },
    "florence": {
        "label": "Florence",
        "at": (43.769, 11.256),
        "aliases": ["Florence"],
        "clips": {"content-832de54b-ef9b-469f-bf15-459ab668d1ca": "Sunny Afternoon in Florence, Italy"},
    },
    "singapore": {
        "label": "Singapore",
        "at": (1.29, 103.85),
        "aliases": ["Singapore"],
        "clips": {"content-8cc628a1-1f11-43aa-92a4-8d3026a53272": "Neon City"},
    },
    "montgomery": {
        "label": "Montgomery, Alabama",
        "at": (32.377, -86.300),
        "aliases": ["Montgomery"],
        "region": "Alabama",
        "clips": {"video_inspire_alabama_montgomery_": "Montgomery"},
    },
    # General fallback: night-time city clips. Captions stay the clip's own place.
    CITY_NIGHT: {
        "label": "City at night",
        "clips": {
            "comp_DB_D001_C001": "Dubai",
            "comp_DB_D002_C003": "Dubai",
            "comp_DB_D011_C010": "Dubai",
            "comp_N013_C004": "New York",
            "content-8cc628a1-1f11-43aa-92a4-8d3026a53272": "Neon City",
            "video_inspire_texas_austin_": "Austin",
        },
    },
}

# Approximate coordinates (lat, lon) of every fact place without its own footage, used only to
# pick the nearest footage city. A place missing here falls straight back to city-at-night.
PLACE_COORDS = {
    "Atlanta": (33.749, -84.388),
    "Berlin": (52.52, 13.405),
    "Bethel": (41.698, -74.875),
    "Blauvelt": (41.063, -73.958),
    "Boulogne-Billancourt": (48.836, 2.240),
    "Brighton": (50.823, -0.137),
    "Bristol": (51.454, -2.588),
    "Burbank": (34.181, -118.309),
    "Champaign": (40.116, -88.243),
    "Chicago": (41.878, -87.630),
    "Clear Lake": (43.138, -93.379),
    "Cleveland": (41.499, -81.694),
    "Clovis": (34.405, -103.205),
    "Cupertino": (37.323, -122.032),
    "Daly City": (37.688, -122.470),
    "Detroit": (42.331, -83.046),
    "Doraville": (33.898, -84.283),
    "Dublin": (53.350, -6.260),
    "Englewood Cliffs": (40.885, -73.952),
    "Folsom": (38.678, -121.176),
    "Gjerlev": (56.580, 10.130),
    "Gladsaxe": (55.733, 12.483),
    "Glendale": (33.539, -112.186),
    "Headley": (51.119, -0.826),
    "Honolulu": (21.307, -157.858),
    "Houston": (29.760, -95.370),
    "Indianapolis": (39.768, -86.158),
    "Inglewood": (33.962, -118.353),
    "Kingston": (17.971, -76.793),
    "Knebworth": (51.866, -0.187),
    "Lagos": (6.524, 3.379),
    "Liverpool": (53.408, -2.991),
    "Lugano": (46.004, 8.951),
    "Memphis": (35.150, -90.049),
    "Miami Beach": (25.790, -80.130),
    "Miami Gardens": (25.942, -80.246),
    "Moncton": (46.088, -64.778),
    "Monterey": (36.600, -121.894),
    "Munich": (48.137, 11.575),
    "Muscle Shoals": (34.745, -87.667),
    "Nashville": (36.163, -86.781),
    "Nassau": (25.044, -77.350),
    "New Orleans": (29.951, -90.072),
    "Newport": (41.490, -71.313),
    "Odense": (55.403, 10.402),
    "Paris": (48.857, 2.352),
    "Pasadena": (34.148, -118.144),
    "Philadelphia": (39.953, -75.165),
    "Randers": (56.461, 10.036),
    "Rockfield": (51.830, -2.750),
    "San Bernardino": (34.108, -117.290),
    "San Diego": (32.716, -117.161),
    "Santa Monica": (34.019, -118.491),
    "Sausalito": (37.859, -122.485),
    "Seattle": (47.606, -122.332),
    "Sheffield": (34.765, -87.699),
    "Shipton-on-Cherwell": (51.850, -1.310),
    "Stockholm": (59.329, 18.069),
    "Tokyo": (35.676, 139.650),
    "Vienna": (48.208, 16.373),
    "Washington": (38.907, -77.037),
    "Watkins Glen": (42.381, -76.873),
    "West Hollywood": (34.090, -118.362),
    "Windsor": (51.483, -0.604),
}


def km(a, b):
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(h))


def load_manifest_labels():
    """file name -> manifest location label, across all shipped manifests."""
    labels = {}
    for name, label_key in (("tvos26", "accessibilityLabel"), ("fireos8", "accessibilityLabel"),
                            ("comm1", "title"), ("comm2", "title")):
        with open(os.path.join(RAW, name + ".json"), encoding="utf-8") as f:
            for asset in json.load(f)["assets"]:
                for key, url in asset.items():
                    if key.startswith("url") and url:
                        labels[os.path.basename(url)] = asset.get(label_key) or ""
    return labels


def check_tokens(labels):
    for key, spec in FOOTAGE.items():
        for token, expected in spec["clips"].items():
            hits = {name: label for name, label in labels.items() if token.lower() in name.lower()}
            if not hits:
                sys.exit(f"{key}: token {token} matches no shipped clip")
            wrong = {n: l for n, l in hits.items() if l != expected}
            if wrong:
                sys.exit(f"{key}: token {token} also matches other places: {wrong}")


def footage_chain(place):
    name = place["place"]
    for key, spec in FOOTAGE.items():
        if key != CITY_NIGHT and name in spec["aliases"]:
            return key, [key, CITY_NIGHT], "direct"
    at = PLACE_COORDS.get(name)
    if at is not None:
        nearest = min(
            ((km(at, spec["at"]), key) for key, spec in FOOTAGE.items() if key != CITY_NIGHT),
        )
        if nearest[0] <= FALLBACK_KM:
            return None, [nearest[1], CITY_NIGHT], "nearest"
    else:
        print(f"note: no coordinates for {name}; using city-at-night", file=sys.stderr)
    return None, [CITY_NIGHT], "city-night"


def main():
    labels = load_manifest_labels()
    check_tokens(labels)
    with open(RESEARCH, encoding="utf-8") as f:
        research = json.load(f)

    facts = {}
    dates = {}
    stats = {"direct": 0, "nearest": 0, "city-night": 0, "none": 0}
    for key in sorted(research):
        entry = research[key]
        facts[key] = entry["lines"]
        if not entry.get("place"):
            stats["none"] += 1
            continue
        direct, chain, kind = footage_chain(entry)
        stats[kind] += 1
        record = {"place": entry["place"]}
        if entry.get("placeRegion"):
            record["region"] = entry["placeRegion"]
        if direct and entry.get("tieIn"):
            record["tieIn"] = entry["tieIn"]
        if direct:
            record["direct"] = direct
        record["footage"] = chain
        dates[key] = record

    footage = {
        key: {"label": spec["label"], "tokens": sorted(spec["clips"])} for key, spec in FOOTAGE.items()
    }
    with open(os.path.join(ASSETS, "music_facts.json"), "w", encoding="utf-8") as f:
        json.dump(facts, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(os.path.join(ASSETS, "music_fact_places.json"), "w", encoding="utf-8") as f:
        json.dump({"footage": footage, "dates": dates}, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(json.dumps(stats))


if __name__ == "__main__":
    main()
