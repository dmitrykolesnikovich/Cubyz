package io.cubyz.world.generator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import org.joml.Vector3i;

import io.cubyz.api.IRegistryElement;
import io.cubyz.api.Registry;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.Block.BlockClass;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.blocks.Ore;
import io.cubyz.world.Chunk;
import io.cubyz.world.LocalSurface;
import io.cubyz.world.Surface;
import io.cubyz.world.World;
import io.cubyz.world.cubyzgenerators.*;
import io.cubyz.world.cubyzgenerators.biomes.Biome;

//TODO: Add more diversity
public class LifelandGenerator extends SurfaceGenerator {
	
	public static void init() {
		GENERATORS.registerAll(new TerrainGenerator(), new RiverGenerator(), new OreGenerator(), new CaveGenerator(), new VegetationGenerator(), new GrassGenerator());
	}
	
	public static void initOres(Ore[] ores) {
		OreGenerator.ores = ores;
	}
	
	public static final Registry<Generator> GENERATORS = new Registry<>();
	ArrayList<Generator> sortedGenerators = new ArrayList<>();
	
	public void sortGenerators() {
		sortedGenerators.clear();
		for (IRegistryElement elem : GENERATORS.registered()) {
			Generator g = (Generator) elem;
			sortedGenerators.add(g);
		}
		sortedGenerators.sort(new Comparator<Generator>() {
			@Override
			public int compare(Generator a, Generator b) {
				if (a.getPriority() > b.getPriority()) {
					return 1;
				} else if (a.getPriority() < b.getPriority()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}
	
	@Override
	public void generate(Chunk ch, Surface surface) {
		int ox = ch.getX();
		int oz = ch.getZ();
		int wx = ox << 4;
		int wz = oz << 4;
		long seed = surface.getStellarTorus().getLocalSeed();
		// Generate some maps:
		float[][] heightMap = ((LocalSurface)surface).getHeightMapData(wx-8, wz-8, 32, 32);
		float[][] heatMap = ((LocalSurface)surface).getHeatMapData(wx-8, wz-8, 32, 32);
		Biome[][] biomeMap = ((LocalSurface)surface).getBiomeMapData(wx-8, wz-8, 32, 32);
		boolean[][] vegetationIgnoreMap = new boolean[32][32]; // Stores places where vegetation should not grow, like caves and rivers.
		int[][] realHeight = new int[32][32];
		for(int px = 0; px < 32; px++) {
			for(int pz = 0; pz < 32; pz++) {
				int h = (int)(heightMap[px][pz]*World.WORLD_HEIGHT);
				if(h > World.WORLD_HEIGHT)
					h = World.WORLD_HEIGHT;
				realHeight[px][pz] = h;
				
				heatMap[px][pz] = ((2 - heightMap[px][pz] + TerrainGenerator.SEA_LEVEL/(float)World.WORLD_HEIGHT)*heatMap[px][pz]*120) - 100;
			}
		}
		
		Random r = new Random(seed);
		Block[][][] chunk = new Block[16][16][World.WORLD_HEIGHT];
		
		for (Generator g : sortedGenerators) {
			if (g instanceof FancyGenerator) {
				((FancyGenerator) g).generate(r.nextLong(), ox, oz, chunk, vegetationIgnoreMap, heatMap, realHeight, biomeMap);
			} else if (g instanceof BigGenerator) {
				((BigGenerator) g).generate(r.nextLong(), ox*16, oz*16, chunk, vegetationIgnoreMap, (LocalSurface)surface);
			} else {
				g.generate(r.nextLong(), ox, oz, chunk, vegetationIgnoreMap);
			}
		}

		// Place the blocks in the chunk:
		for(int px = 0; px < 16; px++) {
			for(int pz = 0; pz < 16; pz++) {
				for(int py = 0; py < World.WORLD_HEIGHT; py++) {
					Block b = chunk[px][pz][py];
					if(b != null) {
						BlockInstance bi = new BlockInstance(b);
						bi.setPosition(new Vector3i(wx + px, py, wz + pz));
						ch.rawAddBlock(px, py, pz, bi);
						if(bi.getBlock() != null) {
							if (bi.getBlock().hasBlockEntity())
								ch.blockEntities().put(bi, bi.getBlock().createBlockEntity(bi.getPosition()));
							if (bi.getBlock().getBlockClass() == BlockClass.FLUID)
								ch.updatingLiquids().add(bi);
						}
					}
				}
			}
		}

		ch.applyBlockChanges();
	}

	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "lifeland");
	}
}
