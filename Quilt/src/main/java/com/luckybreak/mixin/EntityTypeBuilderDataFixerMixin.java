package com.luckybreak.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityType.Builder.class)
public abstract class EntityTypeBuilderDataFixerMixin {

    @Redirect(
        method = "build",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Util;fetchChoiceType(Lcom/mojang/datafixers/DSL$TypeReference;Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;"
        )
    )
    private Type<?> luckybreak$suppressCustomEntityDataFixerError(DSL.TypeReference typeReference, String choiceName) {
        if (choiceName != null && choiceName.startsWith("luckybreak:")) {
            return null;
        }
        return Util.fetchChoiceType(typeReference, choiceName);
    }
}
