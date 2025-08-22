package gimpanel.tracker.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Singleton
public class DataSerializer
{
    private final Gson gson;

    public DataSerializer()
    {
        this.gson = new GsonBuilder()
            .registerTypeAdapter(WorldPoint.class, new WorldPointSerializer())
            .registerTypeAdapter(WorldPoint.class, new WorldPointDeserializer())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .setPrettyPrinting()
            .create();
    }

    public String toJson(Object object)
    {
        return gson.toJson(object);
    }

    public <T> T fromJson(String json, Class<T> classOfT)
    {
        return gson.fromJson(json, classOfT);
    }

    public <T> T fromJson(String json, Type typeOfT)
    {
        return gson.fromJson(json, typeOfT);
    }

    public <T> List<T> fromJsonList(String json, Class<T> classOfT)
    {
        Type listType = TypeToken.getParameterized(List.class, classOfT).getType();
        return gson.fromJson(json, listType);
    }

    public Map<String, Object> toMap(Object object)
    {
        String json = toJson(object);
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(json, mapType);
    }

    private static class WorldPointSerializer implements JsonSerializer<WorldPoint>
    {
        @Override
        public JsonElement serialize(WorldPoint src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("plane", src.getPlane());
            return obj;
        }
    }

    private static class WorldPointDeserializer implements JsonDeserializer<WorldPoint>
    {
        @Override
        public WorldPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            JsonObject obj = json.getAsJsonObject();
            int x = obj.get("x").getAsInt();
            int y = obj.get("y").getAsInt();
            int plane = obj.get("plane").getAsInt();
            return new WorldPoint(x, y, plane);
        }
    }
}