package net.just_s.sframes;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Config {
    private final File configFile = FabricLoader.getInstance().getConfigDir().resolve("SeamlessFrames.conf").toFile();
    private final File jsonFile = FabricLoader.getInstance().getConfigDir().resolve("SeamlessFrames.json").toFile();

    private final Gson gson = new Gson();
    // https://minecraft.fandom.com/wiki/Formatting_codes#Color_codes
    // use data from "Name" column
    public String outlineColor = "white";

    // Radius of area where item frames are glowing ("-1" to glow always)
    public int radiusOfGlowing = -1;

    // If true, frames will glow only for players in radius
    public boolean clientSideGlowing = true;

    // Do shear get damaged and break
    public boolean doShearsBreak = true;

    // True if you want to reverse invisible frames back with leather
    public boolean fixWithLeather = true;

    // Dictionary of Player -> Color of frame
    public Map<String, String> playerColor = new HashMap<>();

    public void load() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFile));

            String line = br.readLine().replaceAll(" ", "");
            while (line != null) {
                if (!line.startsWith("#")) {
                    if (line.contains("=")) {
                        line = line.replaceAll(" ", "");
                        String key = line.substring(0, line.indexOf("="));
                        String value = line.substring(line.indexOf("=") + 1);
                        if (value.contains("#")) {value = value.substring(0, value.indexOf("#"));}

                        switch (key) {
                            case "outlineColor" -> outlineColor = value;
                            case "radiusOfGlowing" -> radiusOfGlowing = Integer.parseInt(value);
                            case "clientSideGlowing" -> clientSideGlowing = Boolean.parseBoolean(value);
                            case "doShearsBreak" -> doShearsBreak = Boolean.parseBoolean(value);
                            case "fixWithLeather" -> fixWithLeather = Boolean.parseBoolean(value);
                        }
                    }
                }
                line = br.readLine();
            }
            br.close();
        }  catch (IOException e) {
            SFramesMod.LOGGER.warn("Error on Config.load() .conf > " + e.getMessage());
            dump();
        }
        try {
            String json_string = Files.readString(Path.of(jsonFile.toString()), StandardCharsets.US_ASCII);
            JsonObject data = gson.fromJson(json_string, JsonObject.class);
            Set<Map.Entry<String, JsonElement>> entries = data.entrySet();
            for (Map.Entry<String, JsonElement> entry: entries) {
                playerColor.put(entry.getKey(), entry.getValue().getAsString());
            }
        }  catch (IOException e) {
            SFramesMod.LOGGER.warn("Error on Config.load() .json > " + e.getMessage());
            dumpJson();
        }
    }

    public void dump() {
        try {
            SFramesMod.LOGGER.info("Generating brand new .conf file...");
            FileWriter writer = new FileWriter(configFile);
            writer.write("# https://minecraft.fandom.com/wiki/Formatting_codes#Color_codes\n");
            writer.write("# use data from \"Name\" column\n");
            writer.write("outlineColor=" + outlineColor + "\n\n");

            writer.write("# Radius from player where item frames are glowing (\"-1\" to disable)\n");
            writer.write("radiusOfGlowing=" + radiusOfGlowing + "\n\n");

            writer.write("# If true, frames will glow for individual players based on radius\n");
            writer.write("clientSideGlowing=" + clientSideGlowing + "\n\n");

            writer.write("# Do shears get damaged and break\n");
            writer.write("doShearsBreak=" + doShearsBreak + "\n\n");

            writer.write("# True if you want to reverse invisible frames back with leather\n");
            writer.write("fixWithLeather=" + fixWithLeather);

            writer.close();
            SFramesMod.LOGGER.info("Seamless Frames Config file created with path: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            SFramesMod.LOGGER.error("Error on Config.dump() > " + e.getMessage());
        }
    }

    public void dumpJson() {
        try {
            SFramesMod.LOGGER.info("Generating brand new .json file...");
            FileWriter writer = new FileWriter(jsonFile);
            StringBuilder builder = new StringBuilder();

            builder.append("{\n");
            if (!playerColor.isEmpty()) {
                for (var entry : playerColor.entrySet()) {
                    builder.append("\t\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\",\n");
                }
                builder.replace(builder.lastIndexOf(","), builder.length(), "\n");
            }
            builder.append("}");

            writer.write(builder.toString());
            writer.close();
            SFramesMod.LOGGER.info("Seamless Frames Config file created with path: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            SFramesMod.LOGGER.error("Error on Config.dump() > " + e.getMessage());
        }
    }
}
