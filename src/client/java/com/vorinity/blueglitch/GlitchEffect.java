package com.vorinity.blueglitch;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;

/**
 * Blue glitch animation drawn over a 16x16 item slot. All plain rectangles, so it needs
 * no textures and works with any resource pack.
 *
 * Three layers:
 *  1. a faint blue glow over the whole slot
 *  2. a bright scan bar that sweeps left to right; its timing depends on the slot's screen
 *     position, so on the hotbar it looks like one wave running from slot to slot
 *  3. random flickering "tear" bars that re-roll every few frames, like a bad signal
 */
public final class GlitchEffect {
	// ---- tweak these ----
	private static final long WAVE_PERIOD_MS = 1800;   // time for the scan wave to come back around
	private static final double WAVE_SPEED_PX = 0.22;  // screen pixels the wave travels per ms
	private static final long FLICKER_STEP_MS = 70;    // how often the glitch bars re-roll

	private static final int GLOW = 0x2E2F8CFF;
	private static final int SCAN_CORE = 0xD08FECFF;
	private static final int SCAN_EDGE = 0x803A9CFF;
	private static final int[] TEAR_COLORS = {0xA039C6FF, 0x9000E5FF, 0x80A8F0FF, 0x905A7CFF, 0x70FFFFFF};
	private static final int DROPOUT = 0x90000A28;

	private GlitchEffect() {
	}

	public static void draw(GuiGraphics g, int x, int y) {
		long now = Util.getMillis();

		// 1. glow
		g.fill(x, y, x + 16, y + 16, GLOW);

		// 2. scan wave. The phase shifts with x, so the bar appears to travel across a row of slots.
		long phase = Math.floorMod(now - (long) (x / WAVE_SPEED_PX), WAVE_PERIOD_MS);
		double sweepMs = 16 / WAVE_SPEED_PX;              // time to cross one slot
		if (phase < sweepMs + 30) {
			int barX = x + (int) (phase * WAVE_SPEED_PX) - 2;
			fillClipped(g, x, y, barX - 2, y, barX, y + 16, SCAN_EDGE);
			fillClipped(g, x, y, barX, y, barX + 2, y + 16, SCAN_CORE);
			fillClipped(g, x, y, barX + 2, y, barX + 4, y + 16, SCAN_EDGE);

			// chromatic split right behind the bar
			fillClipped(g, x, y, barX - 5, y + 3, barX - 2, y + 4, 0xB000FFFF);
			fillClipped(g, x, y, barX - 6, y + 11, barX - 3, y + 12, 0xB02050FF);
		}

		// 3. flickering tear bars. Seeded by slot position + time step so every slot glitches differently.
		long step = now / FLICKER_STEP_MS;
		long seed = hash(x * 73856093L ^ y * 19349663L ^ step * 83492791L);

		int bars = (int) (seed & 3);                       // 0-3 bars this frame
		for (int i = 0; i < bars; i++) {
			seed = hash(seed + i);
			int row = (int) ((seed >>> 8) & 15);
			int height = ((seed >>> 12) & 3) == 0 ? 2 : 1;
			int start = (int) ((seed >>> 16) & 15);
			int length = 3 + (int) ((seed >>> 20) & 9);
			int color = TEAR_COLORS[(int) ((seed >>> 28) & 0xFFFF) % TEAR_COLORS.length];
			fillClipped(g, x, y, x + start, y + row, x + start + length, y + row + height, color);
		}

		// occasional dark dropout line
		if (((seed >>> 40) & 7) == 0) {
			int row = (int) ((seed >>> 44) & 15);
			fillClipped(g, x, y, x, y + row, x + 16, y + row + 1, DROPOUT);
		}

		// rare bright pixel sparks
		if (((seed >>> 50) & 3) == 0) {
			int px = (int) ((seed >>> 52) & 15);
			int py = (int) ((seed >>> 56) & 15);
			fillClipped(g, x, y, x + px, y + py, x + px + 1, y + py + 1, 0xC0D0F8FF);
		}
	}

	/** Fills a rectangle but keeps it inside the 16x16 slot at (sx, sy). */
	private static void fillClipped(GuiGraphics g, int sx, int sy, int x1, int y1, int x2, int y2, int color) {
		int cx1 = Math.max(x1, sx);
		int cy1 = Math.max(y1, sy);
		int cx2 = Math.min(x2, sx + 16);
		int cy2 = Math.min(y2, sy + 16);
		if (cx1 < cx2 && cy1 < cy2) {
			g.fill(cx1, cy1, cx2, cy2, color);
		}
	}

	private static long hash(long v) {
		v ^= v >>> 33;
		v *= 0xff51afd7ed558ccdL;
		v ^= v >>> 33;
		v *= 0xc4ceb9fe1a85ec53L;
		v ^= v >>> 33;
		return v;
	}
}
