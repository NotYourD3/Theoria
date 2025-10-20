package pers.notyourd3.theoria.theory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class TheoryLoader extends SimpleJsonResourceReloadListener<JsonElement> {


    public TheoryLoader() {
        super(ExtraCodecs.JSON,FileToIdConverter.json("theory"));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        map.forEach((res,jsonElement) ->{
            JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "theory");
            boolean isRoot = res.getPath().endsWith("root.json");
            if(!isRoot && jsonObject.has("category")){
                TheoryManager.getInstance().addTheory(Theory.fromJson(res,jsonObject));
            }else{
                TheoryManager.getInstance().addCategory(TheoryCategory.fromJson(res,jsonObject));
            }
        });
        TheoryManager.getInstance().postProcessing();
    }

}
