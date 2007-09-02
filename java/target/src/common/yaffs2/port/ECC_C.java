package yaffs2.port;

import yaffs2.utils.*;

import yaffs2.utils.*;

public class ECC_C
{
	/*
	 * YAFFS: Yet Another Flash File System. A NAND-flash specific file system.
	 *
	 * Copyright (C) 2002-2007 Aleph One Ltd.
	 *   for Toby Churchill Ltd and Brightstar Engineering
	 *
	 * Created by Charles Manning <charles@aleph1.co.uk>
	 *
	 * This program is free software; you can redistribute it and/or modify
	 * it under the terms of the GNU General Public License version 2 as
	 * published by the Free Software Foundation.
	 */

	/*
	 * This code implements the ECC algorithm used in SmartMedia.
	 *
	 * The ECC comprises 22 bits of parity information and is stuffed into 3 bytes. 
	 * The two unused bit are set to 1.
	 * The ECC can correct single bit errors in a 256-byte page of data. Thus, two such ECC 
	 * blocks are used on a 512-byte NAND page.
	 *
	 */

	/* Table generated by gen-ecc.c
	 * Using a table means we do not have to calculate p1..p4 and p1'..p4'
	 * for each byte of data. These are instead provided in a table in bits7..2.
	 * Bit 0 of each entry indicates whether the entry has an odd or even parity, and therefore
	 * this bytes influence on the line parity.
	 */

	static final String yaffs_ecc_c_version =
	    "$Id: ECC_C.java,v 1.4 2007/09/02 20:58:38 peter.hilber Exp $";

	static final byte[] column_parity_table = {
		(byte)0x00, (byte)0x55, (byte)0x59, (byte)0x0c, (byte)0x65, (byte)0x30, (byte)0x3c, (byte)0x69,
		(byte)0x69, (byte)0x3c, (byte)0x30, (byte)0x65, (byte)0x0c, (byte)0x59, (byte)0x55, (byte)0x00,
		(byte)0x95, (byte)0xc0, (byte)0xcc, (byte)0x99, (byte)0xf0, (byte)0xa5, (byte)0xa9, (byte)0xfc,
		(byte)0xfc, (byte)0xa9, (byte)0xa5, (byte)0xf0, (byte)0x99, (byte)0xcc, (byte)0xc0, (byte)0x95,
		(byte)0x99, (byte)0xcc, (byte)0xc0, (byte)0x95, (byte)0xfc, (byte)0xa9, (byte)0xa5, (byte)0xf0,
		(byte)0xf0, (byte)0xa5, (byte)0xa9, (byte)0xfc, (byte)0x95, (byte)0xc0, (byte)0xcc, (byte)0x99,
		(byte)0x0c, (byte)0x59, (byte)0x55, (byte)0x00, (byte)0x69, (byte)0x3c, (byte)0x30, (byte)0x65,
		(byte)0x65, (byte)0x30, (byte)0x3c, (byte)0x69, (byte)0x00, (byte)0x55, (byte)0x59, (byte)0x0c,
		(byte)0xa5, (byte)0xf0, (byte)0xfc, (byte)0xa9, (byte)0xc0, (byte)0x95, (byte)0x99, (byte)0xcc,
		(byte)0xcc, (byte)0x99, (byte)0x95, (byte)0xc0, (byte)0xa9, (byte)0xfc, (byte)0xf0, (byte)0xa5,
		(byte)0x30, (byte)0x65, (byte)0x69, (byte)0x3c, (byte)0x55, (byte)0x00, (byte)0x0c, (byte)0x59,
		(byte)0x59, (byte)0x0c, (byte)0x00, (byte)0x55, (byte)0x3c, (byte)0x69, (byte)0x65, (byte)0x30,
		(byte)0x3c, (byte)0x69, (byte)0x65, (byte)0x30, (byte)0x59, (byte)0x0c, (byte)0x00, (byte)0x55,
		(byte)0x55, (byte)0x00, (byte)0x0c, (byte)0x59, (byte)0x30, (byte)0x65, (byte)0x69, (byte)0x3c,
		(byte)0xa9, (byte)0xfc, (byte)0xf0, (byte)0xa5, (byte)0xcc, (byte)0x99, (byte)0x95, (byte)0xc0,
		(byte)0xc0, (byte)0x95, (byte)0x99, (byte)0xcc, (byte)0xa5, (byte)0xf0, (byte)0xfc, (byte)0xa9,
		(byte)0xa9, (byte)0xfc, (byte)0xf0, (byte)0xa5, (byte)0xcc, (byte)0x99, (byte)0x95, (byte)0xc0,
		(byte)0xc0, (byte)0x95, (byte)0x99, (byte)0xcc, (byte)0xa5, (byte)0xf0, (byte)0xfc, (byte)0xa9,
		(byte)0x3c, (byte)0x69, (byte)0x65, (byte)0x30, (byte)0x59, (byte)0x0c, (byte)0x00, (byte)0x55,
		(byte)0x55, (byte)0x00, (byte)0x0c, (byte)0x59, (byte)0x30, (byte)0x65, (byte)0x69, (byte)0x3c,
		(byte)0x30, (byte)0x65, (byte)0x69, (byte)0x3c, (byte)0x55, (byte)0x00, (byte)0x0c, (byte)0x59,
		(byte)0x59, (byte)0x0c, (byte)0x00, (byte)0x55, (byte)0x3c, (byte)0x69, (byte)0x65, (byte)0x30,
		(byte)0xa5, (byte)0xf0, (byte)0xfc, (byte)0xa9, (byte)0xc0, (byte)0x95, (byte)0x99, (byte)0xcc,
		(byte)0xcc, (byte)0x99, (byte)0x95, (byte)0xc0, (byte)0xa9, (byte)0xfc, (byte)0xf0, (byte)0xa5,
		(byte)0x0c, (byte)0x59, (byte)0x55, (byte)0x00, (byte)0x69, (byte)0x3c, (byte)0x30, (byte)0x65,
		(byte)0x65, (byte)0x30, (byte)0x3c, (byte)0x69, (byte)0x00, (byte)0x55, (byte)0x59, (byte)0x0c,
		(byte)0x99, (byte)0xcc, (byte)0xc0, (byte)0x95, (byte)0xfc, (byte)0xa9, (byte)0xa5, (byte)0xf0,
		(byte)0xf0, (byte)0xa5, (byte)0xa9, (byte)0xfc, (byte)0x95, (byte)0xc0, (byte)0xcc, (byte)0x99,
		(byte)0x95, (byte)0xc0, (byte)0xcc, (byte)0x99, (byte)0xf0, (byte)0xa5, (byte)0xa9, (byte)0xfc,
		(byte)0xfc, (byte)0xa9, (byte)0xa5, (byte)0xf0, (byte)0x99, (byte)0xcc, (byte)0xc0, (byte)0x95,
		(byte)0x00, (byte)0x55, (byte)0x59, (byte)0x0c, (byte)0x65, (byte)0x30, (byte)0x3c, (byte)0x69,
		(byte)0x69, (byte)0x3c, (byte)0x30, (byte)0x65, (byte)0x0c, (byte)0x59, (byte)0x55, (byte)0x00
	};

	/* Count the bits in an unsigned char or a U32 */

	static int yaffs_CountBits(byte x)
	{
		int r = 0;
		while (x != 0) {
			if ((x & 1) != 0)
				r++;
			x = (byte)((x & 0xff) >>> 1);
		}
		return r;
	}

	static int yaffs_CountBits32(int x)
	{
		int r = 0;
		while (x != 0) {
			if ((x & 1) != 0)
				r++;
			x >>>= 1;
		}
		return r;
	}

	/* Calculate the ECC for a 256-byte block of data */
	public static void yaffs_ECCCalculate(byte[] data, int dataIndex, 
			byte[] ecc, int eccIndex)
	{
		int i;

		int col_parity = 0;
		int line_parity = 0;
		int line_parity_prime = 0;
		int t;
		int b;

		for (i = 0; i < 256; i++) {
			b = column_parity_table[Utils.byteAsUnsignedByte(data[dataIndex+i])];
			col_parity ^= b;

			if ((b & 0x01) != 0)	// odd number of bits in the byte
			{
				line_parity ^= i;
				line_parity_prime ^= ~i;
			}

		}

		ecc[eccIndex+2] = (byte)((~col_parity) | 0x03);

		t = 0;
		if ((line_parity & 0x80) != 0)
			t |= 0x80;
		if ((line_parity_prime & 0x80) != 0)
			t |= 0x40;
		if ((line_parity & 0x40) != 0)
			t |= 0x20;
		if ((line_parity_prime & 0x40) != 0)
			t |= 0x10;
		if ((line_parity & 0x20) != 0)
			t |= 0x08;
		if ((line_parity_prime & 0x20) != 0)
			t |= 0x04;
		if ((line_parity & 0x10) != 0)
			t |= 0x02;
		if ((line_parity_prime & 0x10) != 0)
			t |= 0x01;
		ecc[eccIndex+1] = (byte)~t;

		t = 0;
		if ((line_parity & 0x08) != 0)
			t |= 0x80;
		if ((line_parity_prime & 0x08) != 0)
			t |= 0x40;
		if ((line_parity & 0x04) != 0)
			t |= 0x20;
		if ((line_parity_prime & 0x04) != 0)
			t |= 0x10;
		if ((line_parity & 0x02) != 0)
			t |= 0x08;
		if ((line_parity_prime & 0x02) != 0)
			t |= 0x04;
		if ((line_parity & 0x01) != 0)
			t |= 0x02;
		if ((line_parity_prime & 0x01) != 0)
			t |= 0x01;
		ecc[eccIndex+0] = (byte)~t;

	/*#ifdef CONFIG_YAFFS_ECC_WRONG_ORDER
		// Swap the bytes into the wrong order
		t = ecc[0];
		ecc[0] = ecc[1];
		ecc[1] = t;
	#endif*/
	}


	/* Correct the ECC on a 256 byte block of data */

	public static int yaffs_ECCCorrect(byte[] data, int dataIndex, 
			byte[] read_ecc, int read_eccIndex,
			byte[] test_ecc, int test_eccIndex)
	{
		int d0, d1, d2;	/* deltas */

		d0 = (read_ecc[read_eccIndex+0] ^ test_ecc[test_eccIndex+0]);
		d1 = (read_ecc[read_eccIndex+1] ^ test_ecc[test_eccIndex+1]);
		d2 = (read_ecc[read_eccIndex+2] ^ test_ecc[test_eccIndex+2]);

		if ((d0 | d1 | d2) == 0)
			return 0; /* no error */

		if (((d0 ^ (d0 >>> 1)) & 0x55) == 0x55 &&
		    ((d1 ^ (d1 >>> 1)) & 0x55) == 0x55 &&
		    ((d2 ^ (d2 >>> 1)) & 0x54) == 0x54) {
			/* Single bit (recoverable) error in data */

			int _byte;
			int bit;

	/*#ifdef CONFIG_YAFFS_ECC_WRONG_ORDER
			// swap the bytes to correct for the wrong order
			unsigned char t;

			t = d0;
			d0 = d1;
			d1 = t;
	#endif*/

			bit = _byte = 0;

			if ((d1 & 0x80) != 0)
				_byte |= 0x80;
			if ((d1 & 0x20) != 0)
				_byte |= 0x40;
			if ((d1 & 0x08) != 0)
				_byte |= 0x20;
			if ((d1 & 0x02) != 0)
				_byte |= 0x10;
			if ((d0 & 0x80) != 0)
				_byte |= 0x08;
			if ((d0 & 0x20) != 0)
				_byte |= 0x04;
			if ((d0 & 0x08) != 0)
				_byte |= 0x02;
			if ((d0 & 0x02) != 0)
				_byte |= 0x01;

			if ((d2 & 0x80) != 0)
				bit |= 0x04;
			if ((d2 & 0x20) != 0)
				bit |= 0x02;
			if ((d2 & 0x08) != 0)
				bit |= 0x01;

			/*data[_byte] ^= (1 << bit);*/ data[dataIndex + _byte] ^= (1 << bit);

			return 1; /* Corrected the error */
		}

		if ((yaffs_CountBits((byte)d0) + 
		     yaffs_CountBits((byte)d1) + 
		     yaffs_CountBits((byte)d2)) ==  1) {
			/* Reccoverable error in ecc */

			read_ecc[read_eccIndex+0] = test_ecc[test_eccIndex+0];
			read_ecc[read_eccIndex+1] = test_ecc[test_eccIndex+1];
			read_ecc[read_eccIndex+2] = test_ecc[test_eccIndex+2];

			return 1; /* Corrected the error */
		}
		
		/* Unrecoverable error */

		return -1;

	}


	/*
	 * ECCxxxOther does ECC calcs on arbitrary n bytes of data
	 */
	public static void yaffs_ECCCalculateOther(SerializableObject data/*, int nBytes*/ ,
				     yaffs_ECCOther eccOther)
	{
		int nBytes = data.getSerializedLength();	// PORT
		int i;

		int col_parity = 0;
		int line_parity = 0;
		int line_parity_prime = 0;
		int b;

		for (i = 0; i < nBytes; i++) {
			b = column_parity_table[Utils.byteAsUnsignedByte(data.serialized[data.offset+i])];
			col_parity ^= b;

			if ((b & 0x01) != 0)	 {
				/* odd number of bits in the byte */
				line_parity ^= i;
				line_parity_prime ^= ~i;
			}

		}

		eccOther.setcolParity((byte)((col_parity >>> 2) & 0x3f));
		eccOther.setlineParity(line_parity);
		eccOther.setlineParityPrime(line_parity_prime);
	}

	public static int yaffs_ECCCorrectOther(SerializableObject data/*, int nBytes*/ ,
				  yaffs_ECCOther read_ecc,
				  yaffs_ECCOther test_ecc)
	{
		int nBytes = data.getSerializedLength();
		int cDelta;	/* column parity delta */
		int lDelta;	/* line parity delta */
		int lDeltaPrime;	/* line parity delta */
		int bit;

		cDelta = read_ecc.colParity() ^ test_ecc.colParity();
		lDelta = read_ecc.lineParity() ^ test_ecc.lineParity();
		lDeltaPrime = read_ecc.lineParityPrime() ^ test_ecc.lineParityPrime();

		if ((cDelta | lDelta | lDeltaPrime) == 0)
			return 0; /* no error */

		if (lDelta == ~lDeltaPrime && 
		    (((cDelta ^ (cDelta >>> 1)) & 0x15) == 0x15))
		{
			/* Single bit (recoverable) error in data */

			bit = 0;

			if ((cDelta & 0x20) != 0)
				bit |= 0x04;
			if ((cDelta & 0x08) != 0)
				bit |= 0x02;
			if ((cDelta & 0x02) != 0)
				bit |= 0x01;

			if(lDelta >= nBytes)
				return -1;
				
			data.serialized[data.offset+(int)lDelta] ^= (1 << bit);

			return 1; /* corrected */
		}

		if ((yaffs_CountBits32(lDelta) + yaffs_CountBits32(lDeltaPrime) +
		     yaffs_CountBits((byte)cDelta)) == 1) {
			/* Reccoverable error in ecc */

			read_ecc = test_ecc;
			return 1; /* corrected */
		}

		/* Unrecoverable error */

		return -1;

	}


}
