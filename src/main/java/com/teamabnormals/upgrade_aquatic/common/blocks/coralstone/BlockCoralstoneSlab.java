package com.teamabnormals.upgrade_aquatic.common.blocks.coralstone;

import java.util.Random;

import javax.annotation.Nullable;

import com.teamabnormals.upgrade_aquatic.core.registry.UABlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CoralWallFanBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public class BlockCoralstoneSlab extends SlabBlock {
	@Nullable
	private Block[] growableCoralBlocks;
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");
	
	public BlockCoralstoneSlab(Properties properties, @Nullable Block[] growableCoralBlocks) {
		super(properties);
		this.growableCoralBlocks = growableCoralBlocks;
		this.setDefaultState(this.getDefaultState()
			.with(TYPE, SlabType.BOTTOM)
			.with(WATERLOGGED, false)
			.with(POWERED, false)
		);
	}
	
	@Override
	public void tick(BlockState state, World worldIn, BlockPos pos, Random random) {
		if(!worldIn.isAreaLoaded(pos, 3)) return;
		Block block = state.getBlock();
		
		if(this.growableCoralBlocks == null && block != UABlocks.DEAD_CORALSTONE_SLAB.get()) {
			for(int i = 0; i < 4; i++) {
				BlockPos blockpos = pos.add(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
				if(UABlocks.CORALSTONE_SLAB_CONVERSION_MAP.containsKey(worldIn.getBlockState(blockpos).getBlock())) {
					BlockState newState = UABlocks.CORALSTONE_SLAB_CONVERSION_MAP.get(worldIn.getBlockState(blockpos).getBlock()).getDefaultState().with(TYPE, state.get(TYPE)).with(WATERLOGGED, state.get(WATERLOGGED));
					worldIn.setBlockState(pos, newState);
				}
			}
		}
		
		if(this.growableCoralBlocks != null && random.nextFloat() < 0.12F && state.get(POWERED)) {
			Direction randDirection = this.growableCoralBlocks.length > 3 ? Direction.random(random) : Direction.byIndex(random.nextInt(5) + 1);
			BlockPos growPos = pos.offset(randDirection);
			IFluidState fluidState = worldIn.getBlockState(growPos).getFluidState();
			
			if(state.get(TYPE) == SlabType.BOTTOM) {
				if(this.isValidPosToGrow(worldIn, pos.offset(Direction.DOWN), fluidState) && growableCoralBlocks.length > 3) {
					worldIn.setBlockState(pos.offset(Direction.DOWN), growableCoralBlocks[3].getDefaultState(), 2);
				}
			} else if(state.get(TYPE) == SlabType.TOP) {
				if(this.isValidPosToGrow(worldIn, pos.offset(Direction.UP), fluidState)) {
					if(random.nextBoolean()) {
						worldIn.setBlockState(pos.offset(Direction.UP), growableCoralBlocks[0].getDefaultState(), 2);
					} else {
						worldIn.setBlockState(pos.offset(Direction.UP), growableCoralBlocks[1].getDefaultState(), 2);
					}
				}
			} else if(state.get(TYPE) == SlabType.DOUBLE && this.isValidPosToGrow(worldIn, growPos, fluidState)) {
				if(randDirection.getIndex() > 1) {
					worldIn.setBlockState(growPos, growableCoralBlocks[2].getDefaultState().with(CoralWallFanBlock.FACING, randDirection), 2);
				} else if(randDirection.getIndex() == 1) {
					if(random.nextBoolean()) {
						worldIn.setBlockState(growPos, growableCoralBlocks[1].getDefaultState(), 2);
					} else {
						worldIn.setBlockState(growPos, growableCoralBlocks[0].getDefaultState(), 2);
					}
				} else {
					worldIn.setBlockState(growPos, growableCoralBlocks[3].getDefaultState(), 2);
				}
			}
		}
	}
	
	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		if(!worldIn.isRemote) {
			boolean flag = state.get(POWERED);
			if(flag != worldIn.isBlockPowered(pos)) {
				worldIn.setBlockState(pos, state.cycle(POWERED), 2);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		ItemStack stack = player.getHeldItem(hand);
		if(stack.getItem() == Items.SHEARS && state.getBlock() != UABlocks.CORALSTONE_SLAB.get()) {
			BlockState newState = UABlocks.CORALSTONE_SLAB.get().getDefaultState();
			world.playSound(null, pos, SoundEvents.ENTITY_MOOSHROOM_SHEAR, SoundCategory.PLAYERS, 1.0F, 0.8F);
			stack.damageItem(1, player, (entity) -> entity.sendBreakAnimation(hand));
			world.setBlockState(pos, newState.with(TYPE, state.get(TYPE)).with(WATERLOGGED, state.get(WATERLOGGED)), 2);
			return true;
		}
		return super.onBlockActivated(state, world, pos, player, hand, hit);
	}
	
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return super.getStateForPlacement(context).with(POWERED, context.getWorld().isBlockPowered(context.getPos()));
	}
	
	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(TYPE, WATERLOGGED, POWERED);
	}
	
	private boolean isValidPosToGrow(World world, BlockPos pos, IFluidState fluidState) {
		return world.getBlockState(pos).getMaterial().isReplaceable() && fluidState.getLevel() >= 8 && fluidState.isTagged(FluidTags.WATER);
	}
}