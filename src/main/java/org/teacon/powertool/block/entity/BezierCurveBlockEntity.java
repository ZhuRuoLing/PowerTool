package org.teacon.powertool.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.teacon.powertool.block.PowerToolBlocks;

import java.util.List;

public class BezierCurveBlockEntity extends BlockEntity {
    
    public BezierCurveBlockEntity(BlockPos pos, BlockState blockState) {
        super(PowerToolBlocks.BEZIER_CURVE_BLOCK_ENTITY.get(), pos, blockState);
    }
    
    public List<Vector3f> getControlPoints() {
        return List.of(new Vector3f(0,0,0),new Vector3f(20,0.5f,0),new Vector3f(2,1,5),new Vector3f(-1,-1,-5));
    }
}
