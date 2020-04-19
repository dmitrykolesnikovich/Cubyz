package io.cubyz.world.cubyzgenerators;

import java.util.Random;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.Ore;

public class OreGenerator implements Generator {
	
	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "lifeland_ore");
	}
	
	@Override
	public int getPriority() {
		return 32768; // Somewhere before cave generation.
	}
	
	private static Block stone = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:stone");

	public static Ore[] ores;
	public OreGenerator() {}


	// Works basically similar to cave generation, but considers a lot less chunks and has a few other differences.
	@Override
	public void generate(long seed, int cx, int cz, Block[][][] chunk, boolean[][] vegetationIgnoreMap) {
		Random rand = new Random(seed);
		long rand1 = rand.nextLong();
		long rand2 = rand.nextLong();
		// Generate caves from all close by chunks(±1):
		for(int x = cx - 1; x <= cx + 1; ++x) {
			for(int z = cz - 1; z <= cz + 1; ++z) {
				long randX = (long)x*rand1;
				long randY = (long)z*rand2;
				considerCoordinates(x, z, cx, cz, chunk, randX ^ randY ^ seed);
			}
		}
	}
	private void considerCoordinates(int x, int z, int cx, int cz, Block[][][] chunk, long seed) {
		Random rand = new Random();
		for(int i = 0; i < ores.length; i++) {
			// Compose the seeds from some random stats of the ore. They generally shouldn't be the same for two different ores.
			rand.setSeed(seed^(ores[i].getHeight())^(Float.floatToIntBits(ores[i].getMaxSize()))^ores[i].getRegistryID().getID().charAt(0)^Float.floatToIntBits(ores[i].getHardness()));
			// Determine how many veins of this type start in this chunk. The number depends on parameters set for the specific ore:
			int oreSpawns = (int)Math.round((rand.nextFloat()-0.5F)*(rand.nextFloat()-0.5F)*4*ores[i].getSpawns());
			for(int j = 0; j < oreSpawns; ++j) {
				// Choose some in world coordinates to start generating:
				double worldX = (double)((x << 4) + rand.nextInt(16));
				double worldH = (double)rand.nextInt(ores[i].getHeight());
				double worldY = (double)((z << 4) + rand.nextInt(16));
				float direction = rand.nextFloat()*(float)Math.PI*2.0F;
				float slope = (rand.nextFloat() - 0.5F)/4.0F;
				float size = rand.nextFloat()*ores[i].getMaxSize()/2; // Half it to get the radius!
				int length = (int)Math.round(rand.nextFloat()*(ores[i].getMaxLength()));
				if(length == 0)
					continue;
				size = size*length/ores[i].getMaxLength(); // Scale it so that shorter veins don't end up being balls.
				generateVein(rand.nextLong(), cx, cz, chunk, worldX, worldH, worldY, size, direction, slope, length, ores[i]);
			}
		}
	}
	private void generateVein(long random, int cx, int cz, Block[][][] chunk, double worldX, double worldY, double worldZ, float size, float direction, float slope, int veinLength, Block ore) {
		double cwx = (double) (cx*16 + 8);
		double cwz = (double) (cz*16 + 8);
		float directionModifier = 0.0F;
		float slopeModifier = 0.0F;
		Random localRand = new Random(random);
		for(int curStep = 0; curStep < veinLength; ++curStep) {
			double scale = 1+Math.sin(curStep*Math.PI/veinLength)*size;
			// Move vein center point one unit into a direction given by slope and direction:
			float xzunit = (float)Math.cos(slope);
			float hunit = (float)Math.sin(slope);
			worldX += Math.cos(direction) * xzunit;
			worldY += hunit;
			worldZ += Math.sin(direction)*xzunit;
			slope += slopeModifier * 0.1F;
			direction += directionModifier * 0.1F;
			slopeModifier *= 0.9F;
			directionModifier *= 0.75F;
			slopeModifier += (localRand.nextFloat() - localRand.nextFloat())*localRand.nextFloat()*2;
			directionModifier += (localRand.nextFloat() - localRand.nextFloat())*localRand.nextFloat()*4;

			// Add a small chance to ignore one point of the vein to make the walls look more rough.
			if(localRand.nextInt(4) != 0) {
				double deltaX = worldX - cwx;
				double deltaZ = worldZ - cwz;
				double stepsLeft = (double)(veinLength - curStep);
				double maxLength = (double)(size + 18);
				// Abort if the vein is getting to long:
				if(deltaX*deltaX + deltaZ*deltaZ - stepsLeft*stepsLeft > maxLength*maxLength) {
					return;
				}

				// Only care about it if it is inside the current chunk:
				if(worldX >= cwx - 16 - scale*2 && worldZ >= cwz - 16 - scale*2 && worldX <= cwx + 16 + scale*2 && worldZ <= cwz + 16 + scale*2) {
					// Determine min and max of the current vein segment in all directions.
					int xmin = (int)(worldX - scale) - cx*16 - 1;
					int xmax = (int)(worldX + scale) - cx*16 + 1;
					int ymin = (int)(worldY - scale) - 1;
					int ymax = (int)(worldY + scale) + 1;
					int zmin = (int)(worldZ - scale) - cz*16 - 1;
					int zmax = (int)(worldZ + scale) - cz*16 + 1;
					if (xmin < 0)
						xmin = 0;
					if (xmax > 16)
						xmax = 16;
					if (ymin < 1)
						ymin = 1; // Don't make veins expand to the bedrock layer.
					if (ymax > 248)
						ymax = 248;
					if (zmin < 0)
						zmin = 0;
					if (zmax > 16)
						zmax = 16;

					// Go through all blocks within range of the vein center and change them if they
					// are within range of the center.
					for(int curX = xmin; curX < xmax; ++curX) {
						double distToCenterX = ((double) (curX + cx*16) + 0.5 - worldX) / scale;
						
						for(int curZ = zmin; curZ < zmax; ++curZ) {
							double distToCenterZ = ((double) (curZ + cz*16) + 0.5 - worldZ) / scale;
							int curHeightIndex = ymax;
							if(distToCenterX * distToCenterX + distToCenterZ * distToCenterZ < 1.0) {
								for(int curY = ymax - 1; curY >= ymin; --curY) {
									double distToCenterY = ((double) curY + 0.5 - worldY) / scale;
									// The first ore that gets into a position will be placed:
									if(chunk[curX][curZ][curHeightIndex] == stone && distToCenterX*distToCenterX + distToCenterY*distToCenterY + distToCenterZ*distToCenterZ < 1.0) {
										chunk[curX][curZ][curHeightIndex] = ore;
									}
									--curHeightIndex;
								}
							}
						}
					}
				}
			}
		}
	}
}
