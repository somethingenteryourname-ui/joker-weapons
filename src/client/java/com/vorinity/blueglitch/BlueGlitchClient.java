package com.vorinity.blueglitch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.DrawItemStackOverlayCallback;

public class BlueGlitchClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Fires right after an item (and its count/durability bar) is drawn in any GUI slot:
		// hotbar, inventory, chests, creative tabs, etc.
		DrawItemStackOverlayCallback.EVENT.register((graphics, font, stack, x, y) -> {
			if (!stack.isEmpty() && !stack.hasFoil()) {
				GlitchEffect.draw(graphics, x, y);
			}
		});
	}
}
