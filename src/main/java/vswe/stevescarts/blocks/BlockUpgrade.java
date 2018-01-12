package vswe.stevescarts.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.blocks.tileentities.TileEntityUpgrade;
import vswe.stevescarts.items.ItemUpgrade;
import vswe.stevescarts.upgrades.AssemblerUpgrade;

public class BlockUpgrade extends BlockContainerBase implements ModBlocks.ICustomItemBlock, ModBlocks.ISubtypeItemBlockModelDefinition {

	public static PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 19); //Set to number of upgrades

	public BlockUpgrade() {
		super(Material.ROCK);
		setCreativeTab(StevesCarts.tabsSC2Blocks);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TYPE, 0));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, TYPE);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return getMetaFromEnum(state.getValue(FACING));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, getSideFromMeta(meta));
	}

	public EnumFacing getSideFromMeta(int i) {
		if (i == 0) {
			return EnumFacing.NORTH;
		} else if (i == 1) {
			return EnumFacing.SOUTH;
		} else if (i == 2) {
			return EnumFacing.EAST;
		} else if (i == 3) {
			return EnumFacing.WEST;
		}
		return EnumFacing.NORTH;
	}

	public int getMetaFromEnum(EnumFacing facing) {
		if (facing == EnumFacing.NORTH) {
			return 0;
		} else if (facing == EnumFacing.SOUTH) {
			return 1;
		} else if (facing == EnumFacing.EAST) {
			return 2;
		} else if (facing == EnumFacing.WEST) {
			return 3;
		}
		return 0;
	}

	@Override
	public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
	}

	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityUpgrade) {
			TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			return side != EnumFacing.UP && upgrade.getType() == 13;
		}
		return false;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		TileEntity tile = worldIn.getTileEntity(pos);
		if (tile instanceof TileEntityUpgrade) {
			TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			upgrade.setType(stack.getItemDamage());
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		super.onBlockAdded(world, pos, state);
		((BlockCartAssembler) ModBlocks.CART_ASSEMBLER.getBlock()).addUpgrade(world, pos);
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (player.capabilities.isCreativeMode) {
			TileEntity tile = worldIn.getTileEntity(pos);
			if (tile instanceof TileEntityUpgrade) {
				TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
				upgrade.setType(1);
			}
		}
		super.onBlockHarvested(worldIn, pos, state, player);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		final TileEntity tile = world.getTileEntity(pos);
		if (tile != null && tile instanceof TileEntityUpgrade) {
			final TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			if (upgrade.getUpgrade() != null)
				upgrade.getUpgrade().removed(upgrade);
			if (upgrade.getType() != 1)
				spawnAsEntity(world, pos, new ItemStack(this, 1, upgrade.getType()));
			if (upgrade.hasInventory())
				InventoryHelper.dropInventoryItems(world, pos, upgrade);
		}
		super.breakBlock(world, pos, state);
		((BlockCartAssembler) ModBlocks.CART_ASSEMBLER.getBlock()).removeUpgrade(world, pos);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return getUpgradeBounds(source, pos, state);
	}

	public final AxisAlignedBB getUpgradeBounds(IBlockAccess world, BlockPos pos, IBlockState state) {
		EnumFacing side = state.getValue(FACING).getOpposite();
		final float margin = 0.1875f;
		final float width = 0.125f;
		if (side == EnumFacing.DOWN) {
			return new AxisAlignedBB(margin, 0.0f, margin, 1.0f - margin, width, 1.0f - margin);
		} else if (side == EnumFacing.UP) {
			return new AxisAlignedBB(margin, 1.0f - width, margin, 1.0f - margin, 1.0f, 1.0f - margin);
		} else if (side == EnumFacing.WEST) {
			return new AxisAlignedBB(0.0f, margin, margin, width, 1.0f - margin, 1.0f - margin);
		} else if (side == EnumFacing.EAST) {
			return new AxisAlignedBB(1.0f - width, margin, margin, 1.0f, 1.0f - margin, 1.0f - margin);
		} else if (side == EnumFacing.NORTH) {
			return new AxisAlignedBB(margin, margin, 0.0f, 1.0f - margin, 1.0f - margin, width);
		} else if (side == EnumFacing.SOUTH) {
			return new AxisAlignedBB(margin, margin, 1.0f - width, 1.0f - margin, 1.0f - margin, 1.0f);
		}
		return FULL_BLOCK_AABB;
	}

	//TODO make idle model
	public AxisAlignedBB getIdleBlockBounds() {
		final float margin = 0.1875f;
		final float width = 0.125f;
		return new AxisAlignedBB(margin, width, margin, 1.0f - margin, 1.0f - width, 1.0f - margin);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (player.isSneaking()) {
			return false;
		}
		final TileEntity tile = world.getTileEntity(pos);
		if (tile != null && tile instanceof TileEntityUpgrade) {
			final TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			if (upgrade.getMaster() == null) {
				return false;
			}
			if (world.isRemote) {
				return true;
			}
			if (upgrade.getUpgrade().useStandardInterface()) {
				BlockPos masterPos = upgrade.getMaster().getPos();
				FMLNetworkHandler.openGui(player, StevesCarts.instance, 3, world, masterPos.getX(), masterPos.getY(), masterPos.getZ());
				return true;
			}
			FMLNetworkHandler.openGui(player, StevesCarts.instance, 7, world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(final World world, final int var2) {
		return new TileEntityUpgrade();
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityUpgrade) {
			TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			return new ItemStack(this, 1, upgrade.getType());
		}
		return super.getPickBlock(state, target, world, pos, player);
	}

	@Override
	public ItemBlock getItemBlock() {
		return new ItemUpgrade(this);
	}

	@Override
	public int getSubtypeNumber() {
		return AssemblerUpgrade.getUpgradesList().size();
	}

	@Override
	public String getSubtypeName(int meta) {
		AssemblerUpgrade data = AssemblerUpgrade.getUpgrade(meta);
		if (data != null) {
			if (data.getIcon() == null) {
				data.setIcon("upgrade_" + data.getRawName().toLowerCase());
			}
			return data.getIcon();
		}
		return "unknown_icon";
	}
}
