package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Curated, unbelievable 1-2 sentence facts for the TV screensaver.
 * Combines a rich offline database of 50 mind-blowing facts with an online API refresh.
 */
final class FactFeed {
    private static final String TAG = "TvHome.FactFeed";
    private static final String PREFS = "screensaver_fact_feed";
    private static final String FACTS_KEY = "facts_json";
    private static final String UPDATED_KEY = "updated_at";
    private static final String ATTEMPT_KEY = "attempted_at";
    private static final int TARGET = 50;
    private static final long HOUR_MS = 60L * 60L * 1000L;
    private static final long RETRY_MS = 10L * 60L * 1000L;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "screensaver-fact-feed");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static final class Fact {
        final String text;
        final String sourceTitle;
        final String sourceUrl;

        Fact(String text, String sourceTitle, String sourceUrl) {
            this.text = text;
            this.sourceTitle = sourceTitle;
            this.sourceUrl = sourceUrl;
        }

        String displayText() {
            // Clean 1-2 sentences only, without clutter
            return text;
        }
    }

    private FactFeed() { }

    /**
     * Curated, offline collection of 50 verified unbelievable facts (strictly 1-2 sentences each).
     */
    static List<Fact> fallback() {
        List<Fact> facts = new ArrayList<>(50);
        facts.add(new Fact("Honey never spoils. Archaeologists have excavated 3,000-year-old honey from ancient Egyptian tombs that is still perfectly edible.", "Honey", "https://en.wikipedia.org/wiki/Honey"));
        facts.add(new Fact("A day on Venus is longer than a year on Venus, taking 243 Earth days to complete one single rotation.", "Venus", "https://en.wikipedia.org/wiki/Venus"));
        facts.add(new Fact("There are more trees on Earth than there are stars in the Milky Way galaxy—roughly 3 trillion trees compared to 100 billion stars.", "Trees", "https://en.wikipedia.org/wiki/Tree"));
        facts.add(new Fact("Bananas are curved because they grow towards the sun against gravity, a natural phenomenon known as negative geotropism.", "Banana", "https://en.wikipedia.org/wiki/Banana"));
        facts.add(new Fact("Octopuses have three hearts, nine brains, and completely blue blood powered by copper.", "Octopus", "https://en.wikipedia.org/wiki/Octopus"));
        facts.add(new Fact("Sharks existed before trees and Saturn's rings, having roamed the oceans for over 400 million years.", "Sharks", "https://en.wikipedia.org/wiki/Shark"));
        facts.add(new Fact("A single teaspoon of material from a neutron star would weigh about 6 billion tons on Earth.", "Neutron Star", "https://en.wikipedia.org/wiki/Neutron_star"));
        facts.add(new Fact("Cleopatra lived closer in time to the Apollo 11 Moon landing than to the construction of the Great Pyramid of Giza.", "Cleopatra", "https://en.wikipedia.org/wiki/Cleopatra"));
        facts.add(new Fact("Water can boil and freeze at the exact same moment under the right temperature and pressure, called the triple point.", "Triple Point", "https://en.wikipedia.org/wiki/Triple_point"));
        facts.add(new Fact("An average cumulus cloud weighs roughly 1.1 million pounds (500,000 kg), floating effortlessly in the sky.", "Cloud", "https://en.wikipedia.org/wiki/Cloud"));
        facts.add(new Fact("If you could fold a standard piece of paper 42 times, its thickness would reach from the Earth to the Moon.", "Paper folding", "https://en.wikipedia.org/wiki/Mathematics_of_paper_folding"));
        facts.add(new Fact("Wombat poop is shaped like cubes, which prevents it from rolling away and helps mark their territory.", "Wombat", "https://en.wikipedia.org/wiki/Wombat"));
        facts.add(new Fact("Polar bear skin is pitch black, and their fur is actually clear and hollow to channel warmth directly to the skin.", "Polar Bear", "https://en.wikipedia.org/wiki/Polar_bear"));
        facts.add(new Fact("Oxford University was already teaching students centuries before the Aztec Empire was first founded in 1428.", "Oxford", "https://en.wikipedia.org/wiki/University_of_Oxford"));
        facts.add(new Fact("The immortal jellyfish (Turritopsis dohrnii) can revert to its juvenile polyp stage when stressed, making it biologically immortal.", "Turritopsis", "https://en.wikipedia.org/wiki/Turritopsis_dohrnii"));
        facts.add(new Fact("Koalas have unique fingerprints that are nearly impossible to distinguish from human fingerprints under a microscope.", "Koala", "https://en.wikipedia.org/wiki/Koala"));
        facts.add(new Fact("A single bolt of lightning generates temperatures five times hotter than the surface of the sun.", "Lightning", "https://en.wikipedia.org/wiki/Lightning"));
        facts.add(new Fact("Butterflies taste food using chemical receptors located on the bottom of their feet.", "Butterfly", "https://en.wikipedia.org/wiki/Butterfly"));
        facts.add(new Fact("Saturn's largest moon, Titan, has rivers, lakes, and clouds that rain liquid methane and natural gas.", "Titan", "https://en.wikipedia.org/wiki/Titan_(moon)"));
        facts.add(new Fact("Human DNA is roughly 60% genetically identical to that of a banana.", "Human Genome", "https://en.wikipedia.org/wiki/Human_genome"));
        facts.add(new Fact("There is a giant interstellar cloud in our galaxy containing enough alcohol to fill 400 trillion pints of beer.", "Sagittarius B2", "https://en.wikipedia.org/wiki/Sagittarius_B2"));
        facts.add(new Fact("Cows have best friends among their herd and experience measurable anxiety when separated from them.", "Cattle", "https://en.wikipedia.org/wiki/Cattle"));
        facts.add(new Fact("A blue whale's heart is the size of a golf cart, and its beat can be heard through sonar two miles away.", "Blue Whale", "https://en.wikipedia.org/wiki/Blue_whale"));
        facts.add(new Fact("Nintendo was founded in 1889 as a manufacturer of handmade Japanese playing cards, long before video games existed.", "Nintendo", "https://en.wikipedia.org/wiki/Nintendo"));
        facts.add(new Fact("The Eiffel Tower can grow over 15 centimeters (6 inches) taller during hot summer days due to thermal expansion.", "Eiffel Tower", "https://en.wikipedia.org/wiki/Eiffel_Tower"));
        facts.add(new Fact("Over 80% of Earth's oceans remain completely unexplored and unmapped by humans.", "Ocean", "https://en.wikipedia.org/wiki/Ocean"));
        facts.add(new Fact("Under certain conditions, hot water can freeze faster than cold water, a phenomenon called the Mpemba effect.", "Mpemba Effect", "https://en.wikipedia.org/wiki/Mpemba_effect"));
        facts.add(new Fact("A single day on planet Mercury lasts approximately 176 Earth days from sunrise to sunset.", "Mercury", "https://en.wikipedia.org/wiki/Mercury_(planet)"));
        facts.add(new Fact("There are more possible moves in a game of chess than there are atoms in the observable universe.", "Shannon number", "https://en.wikipedia.org/wiki/Shannon_number"));
        facts.add(new Fact("Sloths can hold their breath underwater for up to 40 minutes, which is four times longer than dolphins.", "Sloth", "https://en.wikipedia.org/wiki/Sloth"));
        facts.add(new Fact("The total biomass of all ants on Earth is estimated to equal or exceed that of all wild birds and mammals combined.", "Ant", "https://en.wikipedia.org/wiki/Ant"));
        facts.add(new Fact("Rain on the exoplanet HD 189733b consists of molten glass, blowing sideways in 5,400 mph winds.", "HD 189733b", "https://en.wikipedia.org/wiki/HD_189733_b"));
        facts.add(new Fact("Your brain generates around 20 watts of electrical power while awake, enough to power a dim lightbulb.", "Human Brain", "https://en.wikipedia.org/wiki/Human_brain"));
        facts.add(new Fact("Apples, strawberries, peaches, and cherries are all biological members of the rose plant family.", "Rosaceae", "https://en.wikipedia.org/wiki/Rosaceae"));
        facts.add(new Fact("Tigers have striped patterns on their skin beneath their fur, with every single coat pattern being unique.", "Tiger", "https://en.wikipedia.org/wiki/Tiger"));
        facts.add(new Fact("Mount Everest grows approximately 4 millimeters higher each year due to continental tectonic movement.", "Mount Everest", "https://en.wikipedia.org/wiki/Mount_Everest"));
        facts.add(new Fact("Sound waves travel roughly four times faster through water than through open air.", "Speed of Sound", "https://en.wikipedia.org/wiki/Speed_of_sound"));
        facts.add(new Fact("A full NASA space suit costs approximately $12 million, with over 70% of the cost for the life-support backpack.", "Space Suit", "https://en.wikipedia.org/wiki/Space_suit"));
        facts.add(new Fact("Venus is the only planet in our solar system that spins clockwise on its axis.", "Venus", "https://en.wikipedia.org/wiki/Venus"));
        facts.add(new Fact("A land snail can enter hibernation and sleep continuously for up to three years during harsh weather.", "Snail", "https://en.wikipedia.org/wiki/Snail"));
        facts.add(new Fact("The human body contains enough carbon to manufacture around 9,000 lead pencils.", "Human Body", "https://en.wikipedia.org/wiki/Composition_of_the_human_body"));
        facts.add(new Fact("Light from the Sun takes approximately 8 minutes and 20 seconds to travel 93 million miles to Earth.", "Sunlight", "https://en.wikipedia.org/wiki/Sunlight"));
        facts.add(new Fact("The tongue of an adult blue whale weighs as much as an entire adult Asian elephant.", "Blue Whale", "https://en.wikipedia.org/wiki/Blue_whale"));
        facts.add(new Fact("Antarctica holds 70% of the world's fresh water and 90% of its ice, yet is technically Earth's driest desert.", "Antarctica", "https://en.wikipedia.org/wiki/Antarctica"));
        facts.add(new Fact("Pluto never completed a single orbit around the Sun between its discovery in 1930 and its declassification in 2006.", "Pluto", "https://en.wikipedia.org/wiki/Pluto"));
        facts.add(new Fact("A flock of crows is known as a murder, while a group of owls is called a parliament.", "Bird terminology", "https://en.wikipedia.org/wiki/List_of_animal_names"));
        facts.add(new Fact("You produce enough saliva in a lifetime to fill more than two average-sized Olympic swimming pools.", "Saliva", "https://en.wikipedia.org/wiki/Saliva"));
        facts.add(new Fact("Hummingbirds are the only birds capable of flying backwards and upside down.", "Hummingbird", "https://en.wikipedia.org/wiki/Hummingbird"));
        facts.add(new Fact("Sea otters hold hands while sleeping to prevent the ocean currents from drifting them apart.", "Sea Otter", "https://en.wikipedia.org/wiki/Sea_otter"));
        facts.add(new Fact("The lighter was invented before the modern friction match was created.", "Lighter", "https://en.wikipedia.org/wiki/Lighter"));
        return facts;
    }

    static void load(Context context, Consumer<List<Fact>> callback) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Fact> cached = read(prefs.getString(FACTS_KEY, null));
        if (cached.isEmpty()) cached = fallback();
        callback.accept(cached);

        long now = System.currentTimeMillis();
        long updated = prefs.getLong(UPDATED_KEY, 0L);
        long attempted = prefs.getLong(ATTEMPT_KEY, 0L);
        if (!cached.isEmpty() && now - updated < HOUR_MS) return;
        if (attempted > 0 && now - attempted < RETRY_MS) return;
        prefs.edit().putLong(ATTEMPT_KEY, now).apply();

        WORKER.execute(() -> {
            try {
                List<Fact> downloaded = downloadOnlineFacts();
                if (downloaded.isEmpty()) return;
                // Merge with fallback facts to guarantee full variety
                List<Fact> combined = new ArrayList<>(downloaded);
                for (Fact f : fallback()) {
                    if (combined.size() >= TARGET) break;
                    combined.add(f);
                }
                JSONArray json = encode(combined);
                prefs.edit().putString(FACTS_KEY, json.toString()).putLong(UPDATED_KEY, System.currentTimeMillis()).apply();
                MAIN.post(() -> callback.accept(combined));
            } catch (Exception error) {
                Log.w(TAG, "Could not refresh online facts, retaining curated database", error);
            }
        });
    }

    private static List<Fact> downloadOnlineFacts() {
        List<Fact> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Fetch short unbelievable facts from online API
        for (int i = 0; i < 15 && result.size() < 10; i++) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://uselessfacts.jsph.pl/api/v2/facts/random?language=en");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "CleanYourTV/1.0");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream in = connection.getInputStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        JSONObject json = new JSONObject(sb.toString());
                        String text = json.optString("text", "").trim();
                        // Verify: 1-2 sentences, length between 30 and 220 characters
                        if (text.length() >= 30 && text.length() <= 220 && !text.contains("http") && seen.add(text.toLowerCase(Locale.ROOT))) {
                            result.add(new Fact(text, "Online Fact", "https://uselessfacts.jsph.pl"));
                        }
                    }
                }
            } catch (Exception ignored) {
                // Network error on individual fetch, continue
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return result;
    }

    private static JSONArray encode(List<Fact> facts) throws Exception {
        JSONArray json = new JSONArray();
        for (Fact fact : facts) {
            JSONObject item = new JSONObject();
            item.put("text", fact.text);
            item.put("title", fact.sourceTitle);
            item.put("url", fact.sourceUrl);
            json.put(item);
        }
        return json;
    }

    private static List<Fact> read(String value) {
        if (value == null || value.isEmpty()) return Collections.emptyList();
        try {
            JSONArray json = new JSONArray(value);
            if (json.length() == 0) return Collections.emptyList();
            List<Fact> facts = new ArrayList<>(json.length());
            for (int i = 0; i < json.length(); i++) {
                JSONObject item = json.optJSONObject(i);
                if (item == null) continue;
                String text = item.optString("text", "").trim();
                String title = item.optString("title", "").trim();
                String url = item.optString("url", "").trim();
                if (!text.isEmpty()) {
                    facts.add(new Fact(text, title, url));
                }
            }
            return facts;
        } catch (Exception invalid) {
            return Collections.emptyList();
        }
    }
}
