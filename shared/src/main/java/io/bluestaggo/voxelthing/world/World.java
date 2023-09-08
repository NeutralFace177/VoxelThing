package io.bluestaggo.voxelthing.world;

import io.bluestaggo.voxelthing.math.AABB;
import io.bluestaggo.voxelthing.math.MathUtil;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.generation.GenCache;
import io.bluestaggo.voxelthing.world.generation.GenerationInfo;
import io.bluestaggo.voxelthing.world.storage.ChunkStorage;
import org.joml.Vector3d;
import org.joml.Vector3i;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World implements IBlockAccess {
	protected final ChunkStorage chunkStorage;
	public final GenCache genCache;

	public final Random random = new Random();
	public final long seed = random.nextLong();

	public int worldType = 1;

	public double partialTick;

	public double playerX;
	public double playerZ;


	public World(int type) {
		chunkStorage = new ChunkStorage(this);
		genCache = new GenCache(this);
		worldType = type;
	}

	protected void debugChunk() {
		int range = 1 << ChunkStorage.RADIUS_POW2 >> 1;
		for (int x = -range; x <= range; x++) {
			for (int z = -range; z <= range; z++) {
				Chunk chunk = chunkStorage.newChunkAt(x, 0, z);

				for (int xx = 0; xx < 32; xx++) {
					for (int zz = 0; zz < 32; zz++) {
						for (int yy = 0; yy < 32; yy++) {
							Block block = Block.STONE;
							if ((x + z) % 2 == 0) {
								block = Block.BRICKS;
							} else {
								if (yy >= 4) {
									block = Block.DIRT;
								}
								if (yy == 31) {
									block = Block.GRASS;
								}
								if (random.nextInt(2) == 0) {
									block = null;
								}
								
							}
							chunk.setBlock(xx, yy, zz, block);
						}
					}
				}
			}
		}
	}

	public Chunk getChunkAt(int x, int y, int z) {
		return chunkStorage.getChunkAt(x, y, z);
	}

	public boolean chunkExists(int x, int y, int z) {
		return getChunkAt(x, y, z) != null;
	}

	public Chunk getChunkAtBlock(int x, int y, int z) {
		return getChunkAt(
				Math.floorDiv(x, Chunk.LENGTH),
				Math.floorDiv(y, Chunk.LENGTH),
				Math.floorDiv(z, Chunk.LENGTH)
		);
	}

	public boolean chunkExistsAtBlock(int x, int y, int z) {
		return getChunkAtBlock(x, y, z) != null;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		Chunk chunk = getChunkAtBlock(x, y, z);
		if (chunk == null) {
			return null;
		}
		return chunk.getBlock(
				Math.floorMod(x, Chunk.LENGTH),
				Math.floorMod(y, Chunk.LENGTH),
				Math.floorMod(z, Chunk.LENGTH)
		);
	}

	public void setBlock(int x, int y, int z, Block block) {
		Chunk chunk = getChunkAtBlock(x, y, z);
		if (chunk == null) {
			return;
		}
		chunk.setBlock(
				Math.floorMod(x, Chunk.LENGTH),
				Math.floorMod(y, Chunk.LENGTH),
				Math.floorMod(z, Chunk.LENGTH),
				block
		);
		onBlockUpdate(x, y, z);
	}

	public void loadChunkAt(int cx, int cy, int cz) {
		if (chunkStorage.getChunkAt(cx, cy, cz) != null) {
			return;
		}

		Chunk chunk = chunkStorage.newChunkAt(cx, cy, cz);
		GenerationInfo genInfo = genCache.getGenerationAt(cx, cz);


		genInfo.generate(worldType);
		int vx = (int)((Math.round(cx*32/genInfo.gridDist) * 50));
		int vz = (int)((Math.round(cz*32/genInfo.gridDist) * 50));
		genInfo.voronoiSeedsGen(vx, vz);
		for (int x = 0; x < Chunk.LENGTH; x++) {
			for (int z = 0; z < Chunk.LENGTH; z++) {
				float height = genInfo.getHeight(x, z);
				float exp = worldType == 1 ? 0.08f : 0.15f;
				height = height > 20 ? height + (float)Math.pow(Math.exp(height-20),exp)-1 : height;
				
				for (int y = 0; y < Chunk.LENGTH; y++) {
					int yy = cy * Chunk.LENGTH + y;
					int xx = cx * Chunk.LENGTH + x;
					int zz = cz * Chunk.LENGTH + z;
					boolean cave = yy < height && genInfo.getCave(x, yy, z);
					Block block = null;
					//increase water level for chaotic world
					int waterLevel = worldType == 1 ? 0 : 2;
					if (!cave) {
						if (yy < height - 4) {
							block = Block.STONE;
						} else if (yy < height - 1 && yy < 23) {
							block = Block.DIRT;
							//blends grass -> snow
							if (yy > 18) {
								if (Math.random() > 0.5) {
									block = Block.DIRT;
								} else {
									block = Block.SNOW;
								}
							}
						} else if (yy < height && yy > waterLevel && yy < 23) {
							block = Block.GRASS;
							//blends grass -> snow
							if (yy > 18) {
								if (Math.random() > 0.5) {
									block = Block.GRASS;
								} else {
									block = Block.SNOW;
								}
							}
						} else if (yy < height && yy < waterLevel) {
							block = Block.SAND;
						} else if (yy < height && yy > 22) {
							block = Block.SNOW;
						} else if (yy < waterLevel && worldType == 1) {
							block = Block.WATER;
						}
						
					/* 	if (yy < 40) {
							float b = genInfo.biome(x, z)[1];
							float a = genInfo.biome(x, z)[0];
							if (a > 0.4) {
								block = Block.WOOL[1];
							} else {
								block = Block.WOOL[3];
							}
							if (b > 0.4) {
								block = Block.WOOL[2];
							} else {
								block = Block.WOOL[Block.WOOL.length-1];
							}
						}
	
					for (int i = 0; i < genInfo.unModVSeeds.size(); i++) {
						ArrayList<Float> a = genInfo.unModVSeeds.get(i);
						if (xx == a.get(0) && zz == a.get(1)) {
							block = Block.WOOL[1];
						}	
					}
						
					
*/
				}
					if (block != null) {
						chunk.setBlock(x, y, z, block);
					}
				}
			}
		}

		onChunkAdded(cx, cy, cz);
	}

	public void loadSurroundingChunks(int cx, int cy, int cz, int radius) {
		List<Vector3i> points = MathUtil.getSpherePoints(radius);

		int loaded = 0;

		for (Vector3i point : points) {
			int x = point.x + cx;
			int y = point.y + cy;
			int z = point.z + cz;

			if (!chunkExists(x, y, z)) {
				loadChunkAt(x, y, z);

				if (++loaded >= 250) {
					return;
				}
			}
		}
	}

	public Chunk getOrLoadChunkAt(int x, int y, int z) {
		Chunk chunk = getChunkAt(x, y, z);
		if (chunk == null) {
			loadChunkAt(x, y, z);
			chunk = getChunkAt(x, y, z);
		}
		return chunk;
	}

	public List<AABB> getSurroundingCollision(AABB box) {
		List<AABB> boxes = new ArrayList<>();

		int minX = (int) Math.floor(box.minX);
		int minY = (int) Math.floor(box.minY);
		int minZ = (int) Math.floor(box.minZ);
		int maxX = (int) Math.floor(box.maxX + 1.0);
		int maxY = (int) Math.floor(box.maxY + 1.0);
		int maxZ = (int) Math.floor(box.maxZ + 1.0);

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					Block block = getBlock(x, y, z);
					if (block != null && block != Block.WATER) {
						boxes.add(block.getCollisionBox(x, y, z));
					}
				}
			}
		}

		return boxes;
	}

	public boolean doRaycast(BlockRaycast raycast) {
		Vector3d pos = new Vector3d(raycast.position);
		Vector3d dir = raycast.direction;
		float dist = 0.0f;

		while (dist < raycast.length) {
			int x = (int) Math.floor(pos.x());
			int y = (int) Math.floor(pos.y());
			int z = (int) Math.floor(pos.z());

			Block block = getBlock(x, y, z);
			if (block != null) {
				AABB collision = block.getCollisionBox(x, y, z);
				if (collision.contains(pos.x, pos.y, pos.z)) {
					raycast.setResult(x, y, z, collision.getClosestFace(pos, dir));
					return true;
				}
			}

			pos.add(dir);
			dist += BlockRaycast.STEP_DISTANCE;
		}

		return false;
	}

	public double scaleToTick(double a, double b) {
		return MathUtil.lerp(a, b, partialTick);
	}

	public void onBlockUpdate(int x, int y, int z) {
		if (getBlock(x, y, z) == Block.WATER && getBlock(x, y-1, z) == null) {
			setBlock(x, y-1, z, Block.WATER);
			// terrible updates blocks around it
			for (int ux = -1; ux < 2; ux++) {
				for (int uy = -1; uy < 2; uy++) {
					for (int uz = -1; uz < 2; uz++) {
						onBlockUpdate(x + ux, y + uy, z + uz);
					}
				}
			} 
		}

	}

	

	public void onChunkAdded(int x, int y, int z) {

		
	}

	public void close() {
	}
}
