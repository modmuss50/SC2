package vswe.stevesvehicles.modules.data;


import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import vswe.stevesvehicles.modules.ModuleBase;
import vswe.stevesvehicles.old.Helpers.ColorHelper;
import vswe.stevesvehicles.old.Helpers.Localization;
import vswe.stevesvehicles.old.Items.ModItems;
import vswe.stevesvehicles.client.rendering.models.ModelVehicle;
import vswe.stevesvehicles.old.StevesVehicles;
import vswe.stevesvehicles.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModuleData {
    private final Class<? extends ModuleBase> moduleClass;
    private final String unlocalizedName;
    private final int modularCost;
    private final ModuleType moduleType;
    private ArrayList<ModuleSide> sides;
    private boolean allowDuplicate;
    private ArrayList<ModuleData> nemesis;
    private ArrayList<ModuleDataGroup> requirement;
    private ModuleData parent;
    private boolean isValid;
    private boolean isLocked;
    private boolean defaultLock;
    private boolean hasRecipe;
    private ArrayList<Localization.MODULE_INFO> message;
    private boolean useExtraData;
    private byte extraDataDefaultValue;
    private ArrayList<IRecipe> recipes;
    private ArrayList<VehicleType> validVehicles;

    @SideOnly(Side.CLIENT)
    private HashMap<String,ModelVehicle> models;
    @SideOnly(Side.CLIENT)
    private HashMap<String,ModelVehicle> modelsPlaceholder;
    @SideOnly(Side.CLIENT)
    private ArrayList<String> removedModels;
    @SideOnly(Side.CLIENT)
    private float modelMultiplier = 0.75F;
    @SideOnly(Side.CLIENT)
    private IIcon icon;

    public ModuleData(String unlocalizedName, Class<? extends ModuleBase> moduleClass, int modularCost) {
        this.moduleClass = moduleClass;
        if (unlocalizedName.contains(":")) {
            System.err.println("The raw unlocalized name can't contain colons. Any colons have been replaced with underscores.");
        }
        this.unlocalizedName = unlocalizedName.replace(":", "_");
        this.modularCost = modularCost;

        ModuleType moduleType = ModuleType.INVALID;
        for (ModuleType type : ModuleType.values()) {
            if (moduleType.getClazz().isAssignableFrom(moduleClass)) {
                moduleType = type;
                break;
            }
        }
        this.moduleType = moduleType;
    }

    public final Class<? extends ModuleBase> getModuleClass() {
        return moduleClass;
    }

    public final String getRawUnlocalizedName() {
        return unlocalizedName;
    }

    public final int getCost() {
        return modularCost;
    }

    public final ModuleType getModuleType() {
        return moduleType;
    }

    public boolean getIsValid() {
        return isValid;
    }

    public boolean getIsLocked() {
        return isLocked;
    }

    public ModuleData lock() {
        isLocked = true;

        return this;
    }

    public boolean getEnabledByDefault() {
        return !defaultLock;
    }

    protected ModuleData lockByDefault() {
        defaultLock = true;

        return this;
    }

    protected ModuleData setAllowDuplicate() {
        allowDuplicate = true;

        return this;
    }

    public boolean getAllowDuplicate() {
        return allowDuplicate;
    }

    public ModuleData useExtraData(byte defaultValue) {
        this.extraDataDefaultValue = defaultValue;
        this.useExtraData = true;

        return this;
    }

    public boolean isUsingExtraData() {
        return useExtraData;
    }

    public byte getDefaultExtraData() {
        return extraDataDefaultValue;
    }


    public ArrayList<ModuleSide> getSides() {
        return sides;
    }

    public ModuleData addSides(ModuleSide ... sides) {
        if (this.sides == null) {
            this.sides = new ArrayList<ModuleSide>();
        }
        for (ModuleSide side : sides) {
            this.sides.add(side);

            if (side == ModuleSide.TOP) { //TODO do this in a nicer way
                removeModel("Rails");
            }
        }

        return this;
    }

    public ModuleData addParent(ModuleData parent) {
        this.parent = parent;

        return this;
    }

    public ModuleData addMessage(Localization.MODULE_INFO s) {
        if (message == null) {
            message = new ArrayList<Localization.MODULE_INFO>();
        }
        message.add(s);

        return this;
    }

    public void addNemesis(ModuleData nemesis) {
        if (this.nemesis == null) {
            this.nemesis = new ArrayList<ModuleData>();
        }
        this.nemesis.add(nemesis);
    }

    public ModuleData addRequirement(ModuleDataGroup requirement) {
        if (this.requirement == null) {
            this.requirement = new ArrayList<ModuleDataGroup>();
        }
        this.requirement.add(requirement);

        return this;
    }

    public static void addNemesis(ModuleData m1, ModuleData m2) {
        m2.addNemesis(m1);
        m1.addNemesis(m2);
    }

    public float getModelMultiplier() {
        return modelMultiplier;
    }

    public ModuleData setModelMultiplier(float val) {
        modelMultiplier = val;

        return this;
    }

    public ModuleData addModel(String tag, ModelVehicle model) {
        addModel(tag, model, false);
        addModel(tag, model, true);
        return this;
    }
    public ModuleData addModel(String tag, ModelVehicle model, boolean placeholder) {
        if (placeholder) {
            if (modelsPlaceholder == null) {
                modelsPlaceholder = new HashMap<String,ModelVehicle>();
            }

            modelsPlaceholder.put(tag, model);
        }else{
            if (models == null) {
                models = new HashMap<String,ModelVehicle>();
            }

            models.put(tag, model);
        }

        return this;
    }

    public HashMap<String,ModelVehicle> getModels(boolean placeholder) {
        if (placeholder) {
            return modelsPlaceholder;
        }else{
            return models;
        }
    }

    public boolean haveModels(boolean placeholder) {
        if (placeholder) {
            return modelsPlaceholder != null;
        }else{
            return models != null;
        }
    }

    public ModuleData removeModel(String tag) {
        if (removedModels == null) {
            removedModels = new ArrayList<String>();
        }

        if(!removedModels.contains(tag)) {
            removedModels.add(tag);
        }
        return this;
    }

    public ArrayList<String> getRemovedModels() {
        return removedModels;
    }

    public boolean haveRemovedModels() {
        return removedModels != null;
    }

    public String getName() {
        return StatCollector.translateToLocal(getUnlocalizedName());
    }

    public String getUnlocalizedName() {
        return "item." + StevesVehicles.localStart + unlocalizedName + ".name";
    }


    public ModuleData getParent() {
        return parent;
    }

    public ArrayList<ModuleData> getNemesis() {
        return nemesis;
    }

    public ArrayList<ModuleDataGroup> getRequirement() {
        return requirement;
    }

    public boolean getHasRecipe() {
        return hasRecipe;
    }

    public String getModuleInfoText(byte data) {
        return null;
    }

    public String getCartInfoText(String name, byte data) {
        return name;
    }

    public void addSpecificInformation(List<String> list) {
        list.add(ColorHelper.LIGHT_GRAY + Localization.MODULE_INFO.MODULAR_COST.translate() + ": " + modularCost);
    }

    public final void addInformation(List<String> list, NBTTagCompound compound) {
        addSpecificInformation(list);
        if (compound != null && compound.hasKey("Data")) {
            String extradatainfo = getModuleInfoText(compound.getByte("Data"));
            if (extradatainfo != null) {
                list.add(ColorHelper.WHITE + extradatainfo);
            }
        }


        if (GuiScreen.isShiftKeyDown()) {

            if (sides == null || sides.size() == 0) {
                list.add(ColorHelper.CYAN + Localization.MODULE_INFO.NO_SIDES.translate());
            }else{
                String sidesText = "";
                for (int i = 0; i < sides.size(); i++) {
                    ModuleSide side = sides.get(i);

                    if(i == 0) {
                        sidesText += side.toString();
                    }else if (i == sides.size() - 1) {
                        sidesText += " " + Localization.MODULE_INFO.AND.translate() + " " + side.toString();
                    }else{
                        sidesText += ", " + side.toString();
                    }
                }


                list.add(ColorHelper.CYAN + Localization.MODULE_INFO.OCCUPIED_SIDES.translate(sidesText, String.valueOf(sides.size())));
            }

            if (getNemesis() != null && getNemesis().size() != 0) {
                if (sides == null || sides.size() == 0) {
                    list.add(ColorHelper.RED + Localization.MODULE_INFO.CONFLICT_HOWEVER.translate() + ":");
                }else{
                    list.add(ColorHelper.RED + Localization.MODULE_INFO.CONFLICT_ALSO.translate() + ":");
                }
                for (ModuleData module : getNemesis()) {
                    list.add(ColorHelper.RED + module.getName());
                }
            }

            if (parent != null) {
                list.add(ColorHelper.YELLOW + Localization.MODULE_INFO.REQUIREMENT.translate() + " " + parent.getName());
            }

            if (getRequirement() != null && getRequirement().size() != 0) {
                for (ModuleDataGroup group : getRequirement()) {
                    list.add(ColorHelper.YELLOW + Localization.MODULE_INFO.REQUIREMENT.translate() + " " + group.getCountName() + " " + group.getName());
                }
            }

            if (getAllowDuplicate()) {
                list.add(ColorHelper.LIME + Localization.MODULE_INFO.DUPLICATES.translate());
            }

            if (validVehicles == null || validVehicles.isEmpty()) {
                list.add(ColorHelper.RED + Localization.MODULE_INFO.NO_VEHICLE_ERROR.translate());
            }else{
                String vehicleText = "";
                for (int i = 0; i < validVehicles.size(); i++) {
                    VehicleType vehicle = validVehicles.get(i);

                    if(i == 0) {
                        vehicleText += vehicle.toString();
                    }else if (i == validVehicles.size() - 1) {
                        vehicleText += " " + Localization.MODULE_INFO.AND.translate() + " " + vehicle.toString();
                    }else{
                        vehicleText += ", " + vehicle.toString();
                    }
                }


                list.add(ColorHelper.MAGENTA + Localization.MODULE_INFO.VEHICLE_TYPES.translate(vehicleText, String.valueOf(validVehicles.size())));
            }
        }

        list.add(ColorHelper.LIGHT_BLUE + Localization.MODULE_INFO.TYPE.translate() + ": " + moduleType.getName());
        addExtraMessage(list);
    }


    private static final int MAX_MESSAGE_ROW_LENGTH = 30;
    public void addExtraMessage(List<String> list) {
        if (message != null) {
            list.add("");
            for (Localization.MODULE_INFO m : message) {
                String str = m.translate();
                if (str.length() <= MAX_MESSAGE_ROW_LENGTH) {
                    addExtraMessage(list, str);
                }else{
                    String[] words = str.split(" ");
                    String row = "";
                    for (String word : words) {
                        String next = (row + " " + word).trim();
                        if (next.length() <= MAX_MESSAGE_ROW_LENGTH) {
                            row = next;
                        }else{
                            addExtraMessage(list, row);
                            row = word;
                        }
                    }
                    addExtraMessage(list, row);
                }
            }
        }
    }

    private void addExtraMessage(List<String> list, String str) {
        list.add(ColorHelper.GRAY + "\u00a7o" + str + "\u00a7r");
    }

    public ModuleData addRecipe(IRecipe recipe) {
        if(this.recipes == null) {
            this.recipes = new ArrayList<IRecipe>();
        }

        this.recipes.add(recipe);

        return this;
    }

    public ModuleData addShapedRecipeWithSize(int width, int height, Object ... recipe) {
        addRecipe(null); //TODO create a shaped recipe

        return this;
    }

    public ModuleData addShapedRecipe(Object ... recipe) {
        if (recipe.length == 9) {
            addShapedRecipeWithSize(3, 3, recipe);
        }else if(recipe.length == 4) {
            addShapedRecipeWithSize(2, 2, recipe);
        }

        return this;
    }

    public ModuleData addShapelessRecipe(Object ... recipe) {
        addRecipe(null); //TODO create a shapeless recipe

        return this;
    }

    public ModuleData addVehicles(VehicleType ... types) {
        if (validVehicles == null) {
            validVehicles = new ArrayList<VehicleType>();
        }

        for (VehicleType type : types) {
            if (validVehicles.size() > 0 && moduleType == ModuleType.HULL) {
                System.out.println("You can't add more than one vehicle type to a hull module."); //TODO localization
                break;
            }
            validVehicles.add(type);
        }

        return this;
    }

    public ArrayList<VehicleType> getValidVehicles() {
        return validVehicles;
    }

    @SideOnly(Side.CLIENT)
    public void loadModels() {}


    @SideOnly(Side.CLIENT)
    public void createIcon(IIconRegister register) {
        icon = register.registerIcon(StevesVehicles.instance.textureHeader + ":" + getRawUnlocalizedName() + "_icon");
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon() {
        return icon;
    }

    public ItemStack getItemStack(int count) {
        int id = ModuleRegistry.getIdFromModule(this);
        if (id >= 0) {
            return new ItemStack(ModItems.modules, count, id);
        }else{
            return null;
        }
    }

    public ItemStack getItemStack() {
        return getItemStack(1);
    }
}