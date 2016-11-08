package vswe.stevescarts.modules.engines;

import vswe.stevescarts.entitys.MinecartModular;

public class ModuleSolarStandard extends ModuleSolarTop {
	public ModuleSolarStandard(final MinecartModular cart) {
		super(cart);
	}

	@Override
	protected int getPanelCount() {
		return 4;
	}

	@Override
	protected int getMaxCapacity() {
		return 800000;
	}

	@Override
	protected int getGenSpeed() {
		return 5;
	}
}