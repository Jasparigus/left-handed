package com.github.jasparigus.lefthanded;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MeshMirrorTest
{
	@Test
	public void mirrorNegatesX()
	{
		float[] vx = {3f, -4f, 0f, 7f};
		MeshMirror.mirrorVerticesX(vx, vx.length);
		float[] expected = {-3f, 4f, -0f, -7f};
		for (int i = 0; i < vx.length; i++)
		{
			// bit-exact: the sign of zero must be preserved (mirror of 0.0f is -0.0f)
			assertEquals(Float.floatToRawIntBits(expected[i]), Float.floatToRawIntBits(vx[i]));
		}
	}

	@Test
	public void doubleMirrorRestoresOriginal()
	{
		float[] original = {3f, -4f, 0f, 7f, Float.MAX_VALUE, Float.MIN_VALUE};
		float[] vx = original.clone();
		MeshMirror.mirrorVerticesX(vx, vx.length);
		MeshMirror.mirrorVerticesX(vx, vx.length);
		assertArrayEquals(original, vx, 0f);
	}

	@Test
	public void mirrorPartiallyNegatesOnlyVertexCount()
	{
		float[] vx = {1f, 2f, 3f, 4f};
		MeshMirror.mirrorVerticesX(vx, 2);
		assertArrayEquals(new float[]{-1f, -2f, 3f, 4f}, vx, 0f);
	}

	@Test
	public void windingSwapsAndRestores()
	{
		int[] i2 = {10, 20, 30};
		int[] i3 = {11, 21, 31};
		MeshMirror.swapFaceWinding(i2, i3, 3);
		assertArrayEquals(new int[]{11, 21, 31}, i2);
		assertArrayEquals(new int[]{10, 20, 30}, i3);
		MeshMirror.swapFaceWinding(i2, i3, 3);
		assertArrayEquals(new int[]{10, 20, 30}, i2);
		assertArrayEquals(new int[]{11, 21, 31}, i3);
	}

	@Test
	public void windingSwapsOnlyFaceCountNotArrayLength()
	{
		// model arrays are often longer than faceCount; only the drawn faces must be touched
		int[] i2 = {1, 2, 999};
		int[] i3 = {4, 5, 888};
		MeshMirror.swapFaceWinding(i2, i3, 2);
		assertArrayEquals(new int[]{4, 5, 999}, i2);
		assertArrayEquals(new int[]{1, 2, 888}, i3);
	}

	@Test
	public void cornerColorSwapSkipsSpecialValuesAndRestores()
	{
		// faceColors3 special values (-1 flat, -2 hidden) must never be moved into faceColors2,
		// and the >= 0 condition stays self-inverse because faceColors2 always holds real colors
		int[] c2 = {0x111111, 0x222222, 0x333333};
		int[] c3 = {0xAAAAAA, -1, -2};
		MeshMirror.swapFaceColors(c2, c3, 3);
		assertArrayEquals(new int[]{0xAAAAAA, 0x222222, 0x333333}, c2);
		assertArrayEquals(new int[]{0x111111, -1, -2}, c3);
		MeshMirror.swapFaceColors(c2, c3, 3);
		assertArrayEquals(new int[]{0x111111, 0x222222, 0x333333}, c2);
		assertArrayEquals(new int[]{0xAAAAAA, -1, -2}, c3);
	}
}
