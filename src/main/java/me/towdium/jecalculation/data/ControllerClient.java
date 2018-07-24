package me.towdium.jecalculation.data;

import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.data.label.ILabel;
import me.towdium.jecalculation.data.structure.Recipe;
import me.towdium.jecalculation.data.structure.User;
import me.towdium.jecalculation.network.packets.PCalculator;
import me.towdium.jecalculation.network.packets.PRecipe;
import me.towdium.jecalculation.utils.Utilities;
import me.towdium.jecalculation.utils.wrappers.Triple;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: towdium
 * Date:   17-10-15.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class ControllerClient {
    static User recordWorld;
    static User recordClient = new User();

    static User getRecord() {
        return recordWorld == null ? recordClient : recordWorld;
    }

    public static List<String> getGroups() {
        User user = getRecord();
        if (user.recipes.size() != 0) return user.recipes.stream().map(p -> p.one).collect(Collectors.toList());
        else return Collections.singletonList(Utilities.I18n.format("common.default"));
    }

    public static void addRecipe(String group, Recipe recipe) {
        getRecord().recipes.add(group, recipe);
        JustEnoughCalculation.network.sendToServer(new PRecipe(group, -1, recipe));
    }

    public static void setRecipe(String group, int index, Recipe recipe) {
        getRecord().recipes.set(group, index, recipe);
        JustEnoughCalculation.network.sendToServer(new PRecipe(group, index, recipe));
    }

    public static void removeRecipe(String group, int index) {
        getRecord().recipes.remove(group, index);
        JustEnoughCalculation.network.sendToServer(new PRecipe(group, index, null));
    }

    /**
     * This is used when syncing data from the server.
     * Client record will be initialized when client starts.
     *
     * @param nbt {@link NBTTagCompound containing the user data}
     */
    public static void init(@Nullable NBTTagCompound nbt) {
        recordWorld = nbt == null ? new User() : new User(nbt);
    }

    @SubscribeEvent
    public static void onLogOut(ClientDisconnectionFromServerEvent event) {
        recordWorld = null;
    }

    public static List<Triple<Recipe, String, Integer>> getRecipes() {
        return getRecord().recipes.getRecipes();
    }

    public static List<Triple<Recipe, String, Integer>> getRecipes(String group) {
        return getRecord().recipes.getRecipes(group);
    }

    public static List<Triple<Recipe, String, Integer>> getRecipes(ILabel label, Recipe.enumIoType type) {
        return getRecord().recipes.getRecipes(label, type);
    }

    public static List<ILabel> getRecent() {
        if (recordWorld == null) return recordClient.recent.getRecords();
        else {
            ArrayList<ILabel> ret = new ArrayList<>();
            Optional<ItemStack> ois = Utilities.getStack();
            ois.ifPresent(is -> {
                User.Recent recent = new User.Recent(Utilities.getTag(is).getTagList(User.Recent.IDENTIFIER, 10));
                ret.addAll(recent.getRecords());
            });
            return ret;
        }
    }

    public static void setRecent(ILabel label) {
        if (recordWorld == null) recordClient.recent.push(label);
        else {
            Optional<ItemStack> ois = Utilities.getStack();
            ois.ifPresent(is -> {
                User.Recent recent = new User.Recent(Utilities.getTag(is).getTagList(User.Recent.IDENTIFIER, 10));
                recent.push(label);
                Utilities.getTag(is).setTag(User.Recent.IDENTIFIER, recent.serialize());
                JustEnoughCalculation.network.sendToServer(new PCalculator(is));
            });
        }
    }

    public static void syncFromServer(User u) {
        recordWorld = u;
    }

    public static void loadFromLocal() {
        recordClient = new User(); // TODO
    }

    public static void writeToLocal() {
        // TODO
    }
}
