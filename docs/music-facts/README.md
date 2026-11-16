# This Day in Music: data and footage

The top-left "This Day in Music" overlay shows one fact per calendar day (366 entries,
including February 29). This folder holds the source data behind it.

## Files

| File | What it is |
|---|---|
| `docs/music-facts/research.json` | Source of truth. One entry per `MM-DD`: the overlay `lines`, the `place` where the record was made or the event happened, a short `tieIn`, and the `sources` used to verify the date and every detail. |
| `scripts/build_music_facts.py` | Generates the two app assets below from `research.json`, and checks every footage token against the shipped manifests. |
| `app/src/main/assets/music_facts.json` | Overlay text, `{"MM-DD": ["line 1", "optional line 2"]}`. Same format as before. |
| `app/src/main/assets/music_fact_places.json` | Footage sets plus the per-date place and footage fallback chain. |

Re-generate after editing `research.json`:

```sh
python3 scripts/build_music_facts.py
```

## Accuracy rules

- Every date was checked against reliable sources (Wikipedia statements with citations, RIAA,
  Billboard, the Recording Academy, Official Charts, major press), then re-checked in a second,
  independent pass. Source URLs are kept per date in `research.json`.
- Credits (producer, label, session players, chart and sales figures) appear only where a
  checked source states them. Details that could not be confirmed were dropped.
- When release dates differ by country, the note says which one is used. Where sources disagreed
  on the day, a different event was chosen.
- A place is tagged only when a source documents it.

## Matching videos to the fact's city

When the overlay is on and "Match videos to the fact's city" is enabled (the default), the
playlist opens each day with a clip of the fact's city and then slots one in every few items.
The clips come only from the aerial manifests the app already ships (Apple, Amazon, and the
Jetson Creative and Robin Fourcade community sets). Each provider's own enabled, quality, scene, and time-of-day
settings still apply.

A clip joins a city's footage set only when its **own manifest entry** is that place, for
example Apple's "London" clips or the Jetson Creative "Austin" clip. Space shots that pass over a
city are not used.

| Footage set | Clips |
|---|---|
| London | Apple (4), Jetson Creative: Hampstead Heath |
| New York | Apple (4), Amazon: The Oculus |
| Los Angeles | Apple (5). The Santa Monica beach clip is excluded because Santa Monica is a separate city. |
| San Francisco | Apple (7), Amazon: Golden Gate, Bay Bridge; Jetson Creative: Castro |
| Las Vegas, Miami, Austin, Portland, Edinburgh | Jetson Creative (1 each) |
| Hong Kong, Dubai | Apple (4 and 5) |
| Sydney, Florence, Singapore ("Neon City") | Amazon (1 each) |
| Montgomery, Alabama | Jetson Creative countryside clip |
| City at night (general fallback) | Night clips of Dubai, New York, Singapore, Austin |

Fallback order for each date:

1. **Direct**: the fact's place has its own footage. The bottom-right caption reads
   "<City>, <tie-in>", for example "London, where Abbey Road was recorded".
2. **Nearest**: the nearest footage city within 650 km. The clip keeps its normal caption,
   which names the place actually shown.
3. **City at night**: the general night set, also with normal captions.

Dates with no documented place leave the playlist unchanged.

## Outside clips

No outside clips were added. Every clip used comes from the providers the app already ships.
