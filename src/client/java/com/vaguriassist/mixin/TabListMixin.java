package com.vaguriassist.mixin;

import com.vaguriassist.VaguriAssistClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class TabListMixin {

	@Inject(method = "handleTabListCustomisation", at = @At("TAIL"))
	private void vaguriassist$captureTabList(ClientboundTabListPacket packet, CallbackInfo ci) {
		VaguriAssistClient.onTabListUpdate(packet.header(), packet.footer());
	}
}
