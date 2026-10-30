package com.vorinity.blueglitch;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;

/**
 * Blue glitch animation drawn over an item slot. All plain rectangles, so it needs
 * no textures and works with any resource pack.
 *
 * Layers (back to front):
 *  1. pulsing blue glow over the whole slot (20x20, a bit bigger than the item)
 *  2. flickering neon outline around the slot
 *  3. a thick scan bar with an afterimage trail that sweeps left to right; timing depends on
 *     the slot's screen x, so on the hotbar it reads as one wave running from slot to slot
 *  4. glitch tears that re-roll every few frames and spill outside the slot
 *  5. random "bursts": a slot briefly goes haywire (white flash, heavy tearing, shifted slices)
 *  6. sparks
 */
public final class GlitchEffect {
	// ---- tweak these ----
	private static final long WAVE_PERIOD_MS = 1400;   // time for the scan wave to come back around
	private static final double WAVE_SPEED_PX = 0.30;  // screen pixels the wave travels per ms
	private static final long FLICKER_STEP_MS = 55;    // how often the glitch tears re-roll
	private static final long BURST_WINDOW_MS = 1100;  // each slot rolls for a burst this often...
	private static final long BURST_LENGTH_MS = 200;   // ...and a burst lasts this long
	private static final int PAD = 2;                  // how far past the 16x16 item the slot effect reaches
	private static final int SPILL = 5;                // how far tears can stick out past the slot

	private static final int SCAN_CORE = 0xF0A8F4FF;
	private static final int SCAN_EDGE = 0xA040B0FF;
	private static final int SCAN_TRAIL = 0x502F8CFF;
	private static final int OUTLINE = 0xB040C8FF;
	private static final int[] TEAR_COLORS = {0xC039C6FF, 0xB000E5FF, 0xA0A8F0FF, 0xB05A7CFF, 0x90FFFFFF, 0xC01E3CFF};
	private static final int DROPOUT = 0xB0000A28;

	private GlitchEffect() {
	}

	public static void draw(GuiGraphics g, int x, int y) {
		long now = Util.getMillis();

		// slot box and the wider "spill" box tears may reach into
		int bx1 = x - PAD, by1 = y - PAD, bx2 = x + 16 + PAD, by2 = y + 16 + PAD;
		Box slot = new Box(bx1, by1, bx2, by2);
		Box spill = new Box(bx1 - SPILL, by1, bx2 + SPILL, by2);
		int w = bx2 - bx1, h = by2 - by1;

		long burstRoll = hash(x * 92821L ^ y * 68917L ^ (now / BURST_WINDOW_MS) * 31337L);
		boolean burst = (burstRoll & 3) == 0 && (now % BURST_WINDOW_MS) < BURST_LENGTH_MS;
		long burstAge = now % BURST_WINDOW_MS;

		// 1. pulsing glow
		double pulse = 0.5 + 0.5 * Math.sin(now / 260.0 + x * 0.07);
		int glowAlpha = burst ? 0x70 : 0x24 + (int) (pulse * 0x2C);
		fill(g, slot, bx1, by1, bx2, by2, (glowAlpha << 24) | 0x2F8CFF);

		// 2. flickering outline
		long step = now / FLICKER_STEP_MS;
		long seed = hash(x * 73856093L ^ y * 19349663L ^ step * 83492791L);
		if (burst || ((seed >>> 60) & 7) != 0) {
			int col = burst ? 0xFFB4F8FF : OUTLINE;
			fill(g, slot, bx1, by1, bx2, by1 + 1, col);
			fill(g, slot, bx1, by2 - 1, bx2, by2, col);
			fill(g, slot, bx1, by1, bx1 + 1, by2, col);
			fill(g, slot, bx2 - 1, by1, bx2, by2, col);
			// glitched gap in the outline
			int gap = (int) ((seed >>> 4) % w);
			fill(g, slot, bx1 + gap, by1, bx1 + gap + 4, by1 + 1, 0xFF00102E);
		}

		// 3. scan wave with afterimage trail
		long phase = Math.floorMod(now - (long) (x / WAVE_SPEED_PX), WAVE_PERIOD_MS);
		double crossMs = (w + 16) / WAVE_SPEED_PX;
		if (phase < crossMs) {
			int barX = bx1 + (int) (phase * WAVE_SPEED_PX) - 6;
			fill(g, slot, barX - 12, by1, barX - 3, by2, SCAN_TRAIL);
			fill(g, slot, barX - 3, by1, barX, by2, SCAN_EDGE);
			fill(g, slot, barX, by1, barX + 4, by2, SCAN_CORE);
			fill(g, slot, barX + 4, by1, barX + 7, by2, SCAN_EDGE);
			// chromatic split lines that stick out of the slot
			fill(g, spill, barX - 9, by1 + 4, barX - 1, by1 + 5, 0xE000FFFF);
			fill(g, spill, barX - 11, by1 + 13, barX - 2, by1 + 14, 0xE02050FF);
			fill(g, spill, barX + 2, by1 + 9, barX + 11, by1 + 10, 0xC0FFFFFF);
		}

		// 4. tears (more of them during a burst)
		int bars = burst ? 6 + (int) (seed & 3) : 1 + (int) (seed & 3);
		for (int i = 0; i < bars; i++) {
			seed = hash(seed + i);
			int row = (int) ((seed >>> 8) % h);
			int height = 1 + (int) ((seed >>> 12) & (burst ? 3 : 1));
			int start = (int) ((seed >>> 16) % (w + SPILL * 2)) - SPILL;
			int length = 4 + (int) ((seed >>> 22) % (burst ? 18 : 11));
			int color = TEAR_COLORS[(int) ((seed >>> 28) & 0xFFFF) % TEAR_COLORS.length];
			fill(g, spill, bx1 + start, by1 + row, bx1 + start + length, by1 + row + height, color);
		}

		// dark dropout lines
		if (burst || ((seed >>> 40) & 3) == 0) {
			int row = (int) ((seed >>> 44) % h);
			fill(g, slot, bx1, by1 + row, bx2, by1 + row + 1, DROPOUT);
		}

		// 5. burst extras
		if (burst) {
			if (burstAge < 45) {
				fill(g, slot, bx1, by1, bx2, by2, 0x70E6F8FF);  // white-blue flash
			}
			// shifted "slices": thick bright bands knocked sideways out of the slot
			for (int i = 0; i < 3; i++) {
				seed = hash(seed ^ (0x9E37L + i));
				int row = (int) ((seed >>> 8) % (h - 3));
				int shift = (int) ((seed >>> 16) % (SPILL * 2 + 1)) - SPILL;
				fill(g, spill, bx1 + shift, by1 + row, bx2 + shift, by1 + row + 3, 0x803CC8FF);
			}
		}

		// 6. sparks
		int sparks = burst ? 6 : 2;
		for (int i = 0; i < sparks; i++) {
			seed = hash(seed + 0x51L * (i + 1));
			if (!burst && ((seed >>> 3) & 1) == 0) {
				continue;
			}
			int px = (int) ((seed >>> 8) % (w + SPILL * 2)) - SPILL;
			int py = (int) ((seed >>> 20) % h);
			int size = ((seed >>> 32) & 3) == 0 ? 2 : 1;
			fill(g, spill, bx1 + px, by1 + py, bx1 + px + size, by1 + py + size, 0xF0D0F8FF);
		}
	}

	private record Box(int x1, int y1, int x2, int y2) {
	}

	/** Fills a rectangle clipped to the given box. */
	private static void fill(GuiGraphics g, Box clip, int x1, int y1, int x2, int y2, int color) {
		int cx1 = Math.max(x1, clip.x1);
		int cy1 = Math.max(y1, clip.y1);
		int cx2 = Math.min(x2, clip.x2);
		int cy2 = Math.min(y2, clip.y2);
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
