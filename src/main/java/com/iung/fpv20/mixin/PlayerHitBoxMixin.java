package com.iung.fpv20.mixin;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.mixin_utils.IsFlying;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerHitBoxMixin extends LivingEntity {

    @Unique
    private static final EntityDimensions DIM = new EntityDimensions(0.5f, 0.15f, false);
//    @Unique
//    private static int a = 1;

//    @Unique
//    public boolean isFlying = false;

    protected PlayerHitBoxMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
//        this.isFlying = false;
        Fpv20.LOGGER.info("new");

    }


//    @Override
//    public void tickMovement() {
//        super.tickMovement();
//    }

    @Inject(method = "getDimensions(Lnet/minecraft/entity/EntityPose;)Lnet/minecraft/entity/EntityDimensions;", at = @At("RETURN"), cancellable = true)
    private void injected(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (((IsFlying) this).get_is_flying()) {
            int frame = ((IsFlying) this).get_frame_index();
            float width = 0.5f;
            float height = 0.15f;
            switch (frame) {
                case 0: // 2" Whoop
                    width = 0.25f;
                    height = 0.08f;
                    break;
                case 1: // 3" Cinewhoop
                    width = 0.35f;
                    height = 0.12f;
                    break;
                case 2: // 5" Freestyle
                    width = 0.50f;
                    height = 0.15f;
                    break;
                case 3: // 7" LongRange
                    width = 0.68f;
                    height = 0.20f;
                    break;
            }
            cir.setReturnValue(new EntityDimensions(width, height, false));
        }
    }

    @Inject(method = "getActiveEyeHeight", at = @At("RETURN"), cancellable = true)
    private void injected(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (((IsFlying) this).get_is_flying()) {
            int frame = ((IsFlying) this).get_frame_index();
            float eyeHeight = 0.13f;
            switch (frame) {
                case 0: // 2" Whoop
                    eyeHeight = 0.06f;
                    break;
                case 1: // 3" Cinewhoop
                    eyeHeight = 0.09f;
                    break;
                case 2: // 5" Freestyle
                    eyeHeight = 0.13f;
                    break;
                case 3: // 7" LongRange
                    eyeHeight = 0.17f;
                    break;
            }
            cir.setReturnValue(eyeHeight);
        }
    }

    @Inject(method = "getVelocityMultiplier", at = @At("HEAD"), cancellable = true)
    private void injected(CallbackInfoReturnable<Float> cir) {
//        cir.getReturnValue();
        if (((IsFlying) this).get_is_flying()) {
            cir.setReturnValue(1.0f);
        }
    }


//    @Inject(method = "attack", at = @At("RETURN"))
//    private void injected(Entity target, CallbackInfo ci) {
//        Fpv20.LOGGER.info("attack");
//        this.isFlying = !this.isFlying;
//        this.setBoundingBox(this.getDimensions(null).getBoxAt(this.getPos()));
//        this.markEffectsDirty();
//    }

//    @Override
//    public boolean get_is_flying() {
//        return this.isFlying;
//    }
//
//    @Override
//    public void set_is_flying(boolean v) {
//
//        this.isFlying = v;
//        this.calculateDimensions();
//    }
}
