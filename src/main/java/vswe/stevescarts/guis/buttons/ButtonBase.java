package vswe.stevescarts.guis.buttons;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

public abstract class ButtonBase {
	protected final LOCATION loc;
	protected final ModuleBase module;
	private boolean lastVisibility;
	private int currentID;
	private int moduleID;
	@SideOnly(Side.CLIENT)
	private static ResourceLocation texture;

	public ButtonBase(final ModuleBase module, final LOCATION loc) {
		(this.module = module).addButton(this);
		this.loc = loc;
	}

	public void setCurrentID(final int id) {
		currentID = id;
	}

	public void setIdInModule(final int id) {
		moduleID = id;
	}

	public int getIdInModule() {
		return moduleID;
	}

	@Override
	public String toString() {
		return "";
	}

	public boolean isEnabled() {
		return false;
	}

	public boolean hasText() {
		return false;
	}

	public boolean isVisible() {
		return false;
	}

	public final void computeOnClick(final GuiMinecart gui, final int mousebutton) {
		if (isVisible() && isEnabled()) {
			onClientClick(mousebutton, GuiScreen.isCtrlKeyDown(), GuiScreen.isShiftKeyDown());
			if (handleClickOnServer()) {
				byte clickinformation = (byte) (mousebutton & 0x3F);
				clickinformation |= (byte) ((GuiScreen.isCtrlKeyDown() ? 1 : 0) << 6);
				clickinformation |= (byte) ((GuiScreen.isShiftKeyDown() ? 1 : 0) << 7);
				module.sendButtonPacket(this, clickinformation);
			}
		}
	}

	public void onClientClick(final int mousebutton, final boolean ctrlKey, final boolean shiftKey) {
	}

	public void onServerClick(final EntityPlayer player, final int mousebutton, final boolean ctrlKey, final boolean shiftKey) {
	}

	public boolean handleClickOnServer() {
		return true;
	}

	private boolean useTexture() {
		return texture() != -1;
	}

	public int ColorCode() {
		return 0;
	}

	private boolean hasBorder() {
		return borderID() != -1;
	}

	public int borderID() {
		return -1;
	}

	public int texture() {
		return -1;
	}

	public int textureX() {
		return texture() % 21 * 12;
	}

	public int textureY() {
		return 60 + texture() / 21 * 12;
	}

	public void drawButtonText(final GuiMinecart gui, final ModuleBase module) {
		if (isVisible() && hasText()) {
			module.drawString(gui, toString(), X() + 8, Y() + 7, 16777215);
		}
	}

	public void drawButton(final GuiMinecart gui, final ModuleBase module, final int x, final int y) {
		final boolean visibility = isVisible();
		if (visibility != lastVisibility) {
			module.buttonVisibilityChanged();
		}
		lastVisibility = visibility;
		ResourceHelper.bindResource(ButtonBase.texture);
		if (!visibility) {
			return;
		}
		int sourceX = 0;
		int sourceY = 20;
		if (isEnabled()) {
			sourceX = 20 * (ColorCode() + 1);
		}
		if (module.inRect(x, y, getBounds())) {
			sourceY += 20;
		}
		module.drawImage(gui, getBounds(), sourceX, sourceY);
		if (useTexture()) {
			module.drawImage(gui, X() + 4, Y() + 4, textureX(), textureY(), 12, 12);
		}
		if (hasBorder()) {
			module.drawImage(gui, getBounds(), borderID() * 20, 0);
		}
	}

	public int[] getBounds() {
		return new int[] { X(), Y(), 20, 20 };
	}

	public int X() {
		switch (loc) {
			case OVERVIEW: {
				return 15 + currentID * 25;
			}
			case PROGRAM: {
				return 125 + currentID % 6 * 25;
			}
			case TASK: {
				return 306 + currentID % 5 * 25;
			}
			case DEFINED: {
				return 0;
			}
			case FLOATING: {
				return 115 + currentID % 7 * 25;
			}
			case VARIABLE: {
				return 400 + currentID % 3 * 25;
			}
			case BUILD: {
				return 366 + currentID % 5 * 25;
			}
			case MODEL: {
				return 111 + currentID % 6 * 22;
			}
			default: {
				return -1;
			}
		}
	}

	public int Y() {
		switch (loc) {
			case OVERVIEW: {
				return 143;
			}
			case PROGRAM: {
				return 118 + currentID / 6 * 25;
			}
			case TASK: {
				return 32 + currentID / 5 * 25;
			}
			case DEFINED: {
				return 0;
			}
			case FLOATING: {
				return 32 + currentID / 7 * 25;
			}
			case VARIABLE: {
				return 32 + currentID / 3 * 25;
			}
			case BUILD: {
				return 118 + currentID / 5 * 25;
			}
			case MODEL: {
				return 19 + currentID / 6 * 22;
			}
			default: {
				return -1;
			}
		}
	}

	public LOCATION getLocation() {
		return loc;
	}

	public int getLocationID() {
		for (int i = 0; i < LOCATION.values().length; ++i) {
			if (LOCATION.values()[i] == loc) {
				return i;
			}
		}
		return 0;
	}

	static {
		ButtonBase.texture = ResourceHelper.getResource("/gui/buttons.png");
	}

	public enum LOCATION {
		OVERVIEW,
		PROGRAM,
		TASK,
		DEFINED,
		FLOATING,
		VARIABLE,
		BUILD,
		MODEL
	}
}
