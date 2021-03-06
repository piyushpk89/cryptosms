/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.utils;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;

/*
 * Class with lots of useful low-level methods
 */
public class LowLevel {
	
	/**
	 * Expects four bytes and returns an unsigned integer stored in a long that's represented by the bytes.
	 *
	 * @param data the data
	 * @return the unsigned int
	 */
	public static long getUnsignedInt(byte[] data) {
		return getUnsignedInt(data, 0);
	}

	/**
	 * Expects four bytes (in an array at specified offset) and returns an unsigned integer stored in a long that's represented by the bytes.
	 *
	 * @param data the data
	 * @param offset the offset
	 * @return the unsigned int
	 */
	public static long getUnsignedInt(byte[] data, int offset) {
		if (offset > data.length - 4)
			throw new IndexOutOfBoundsException();

		long result = data[offset] & 0xFF;
		result <<= 8;
		result |= (data[offset + 1] & 0xFF);
		result <<= 8;
		result |= (data[offset + 2] & 0xFF);
		result <<= 8;
		result |= data[offset + 3] & 0xFF;
		return result;
	}
	
	/**
	 * Expects eight bytes and returns a long that's represented by the bytes.
	 *
	 * @param data the data
	 * @return the long
	 */
	public static long getLong(byte[] data) {
		long result = 0L;
		for (int i = 0; i < 8; ++i) {
			result <<= 8;
			result |= (data[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Expects two bytes and returns a short.
	 *
	 * @param data the data
	 * @return the unsigned short
	 */
	public static int getUnsignedShort(byte[] data) {
		return getUnsignedShort(data, 0);
	}

	/**
	 * Expects two bytes and returns a short.
	 *
	 * @param data the data
	 * @param offset the offset
	 * @return the unsigned short
	 */
	public static int getUnsignedShort(byte[] data, int offset) {
		if (offset > data.length - 2)
			throw new IndexOutOfBoundsException();

		int result = data[offset] & 0xFF;
		result <<= 8;
		result |= (data[offset + 1] & 0xFF);
		return result;
	}

	/**
	 * Expects an unsigned integer stored in the 4 low bytes of a long and returns an array of 4 bytes that represent them.
	 *
	 * @param integer the integer
	 * @return the bytes unsigned int
	 */
	public static byte[] getBytesUnsignedInt(long integer) {
		byte[] result = new byte[4];
		result[0] = (byte) ((integer >> 24) & 0xFF);
		result[1] = (byte) ((integer >> 16) & 0xFF);
		result[2] = (byte) ((integer >> 8) & 0xFF);
		result[3] = (byte) (integer & 0xFF);
		return result;
	}
	
	/**
	 * Expects a short and returns an array of 2 bytes that represents it.
	 *
	 * @param integer the integer
	 * @return the bytes unsigned short
	 */
	public static byte[] getBytesUnsignedShort(int integer) {
		byte[] result = new byte[2];
		result[0] = (byte) ((integer >> 8) & 0xFF);
		result[1] = (byte) (integer & 0xFF);
		return result;
	}
	
	/**
	 * Takes a number (has to be between 0 and 255) and returns a byte that represents it.
	 *
	 * @param number the number
	 * @return the bytes unsigned byte
	 */
	public static byte getBytesUnsignedByte(int number) {
		return (byte)(number & 0xFF);
	}
	
	/**
	 * Expects a long and returns a byte array of 8 elements with its big endian representation.
	 *
	 * @param number the number
	 * @return the bytes long
	 */
	public static byte[] getBytesLong(long number) {
		byte[] result = new byte[8];
		result[0] = (byte) ((number >> 56) & 0xFF);
		result[1] = (byte) ((number >> 48) & 0xFF);
		result[2] = (byte) ((number >> 40) & 0xFF);
		result[3] = (byte) ((number >> 32) & 0xFF);
		result[4] = (byte) ((number >> 24) & 0xFF);
		result[5] = (byte) ((number >> 16) & 0xFF);
		result[6] = (byte) ((number >> 8) & 0xFF);
		result[7] = (byte) (number & 0xFF);
		return result;
	}
	
	/**
	 * Takes a byte that is supposed to be unsigned and returns an int that represents that number.
	 *
	 * @param number the number
	 * @return the unsigned byte
	 */
	public static int getUnsignedByte(byte number) {
		return number & 0xFF;
	}

	/**
	 * Inserts data into an array of specified length. Puts random data behind to fill the rest.
	 *
	 * @param data the data
	 * @param length the length
	 * @return the byte[]
	 */
	public static byte[] wrapData(byte[] data, int length) {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		if (data.length >= length)
			buffer.put(data, 0, length);
		else {
			buffer.put(data);
			buffer.put(Encryption.getEncryption().generateRandomData(length - data.length));
		}
		return buffer.array();
	}
	
	/**
	 * Cuts out byte array from a bigger array.
	 *
	 * @param data the data
	 * @param offset the offset
	 * @param length the length
	 * @return the byte[]
	 */
	public static byte[] cutData(byte[] data, int offset, int length) {
		if (length > 0) {
			byte[] result = new byte[length];
			System.arraycopy(data, offset, result, 0, length);
			return result;
		} else
			return new byte[0];
	}

	/**
	 * Takes string containing HEX data and returns byte array that represents it.
	 *
	 * @param s the s
	 * @return the byte[]
	 */
	public static byte[] fromHex(String s) {
	    int len = s.length() / 2;
	    byte[] data = new byte[len];
	    for (int i = 0; i < len; ++i) {
	        data[i] = (byte) ((Character.digit(s.charAt(2 * i), 16) << 4)
	                         + Character.digit(s.charAt(2 * i + 1), 16));
	    }
	    return data;
	}
	
	private static char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	/**
	 * Turns byte array into string containing HEX of all the bytes.
	 *
	 * @param data the data
	 * @return the string
	 */
	public static String toHex(byte[] data) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < data.length; ++i) {
			builder.append(hexChars[(data[i] & 0xF0) >> 4]);
			builder.append(hexChars[data[i] & 0x0F]);
		}
		return builder.toString();
	}
	
	/**
	 * Round up division.
	 *
	 * @param number the number
	 * @param divisor the divisor
	 * @return the int
	 */
	public static int roundUpDivision(int number, int divisor) {
		if (number < divisor)
			return 1;
		else
			return (number + divisor - 1) / divisor;
	}
	
	/**
	 * Closest greatest multiple.
	 *
	 * @param number the number
	 * @param multipleOf the multiple of
	 * @return the int
	 */
	public static int closestGreatestMultiple(int number, int multipleOf) {
		return multipleOf * roundUpDivision(number, multipleOf);
	}
}
