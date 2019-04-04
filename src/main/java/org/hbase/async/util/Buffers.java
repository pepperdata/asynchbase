package org.hbase.async.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import org.hbase.async.rpc.InvalidResponseException; // TODO: remove?

public class Buffers {

  private static final Logger LOG = LoggerFactory.getLogger(Buffers.class);

  private Buffers() {} // prevent instances

  /**
   * Ensures that at least a {@code nbytes} are readable from the given buffer.
   * If there aren't enough bytes in the buffer this will raise an exception
   * and cause the {@link ReplayingDecoder} to undo whatever we did thus far
   * so we can wait until we read more from the socket.
   * @param buf Buffer to check.
   * @param nbytes Number of bytes desired.
   */
  public static void ensureReadable(final ChannelBuffer buf, final int nbytes) {
    checkArrayLength(buf, nbytes);
    buf.markReaderIndex();
    buf.skipBytes(nbytes);
    buf.resetReaderIndex();
  }

  /**
   * Writes a {@link Boolean boolean} as an HBase RPC parameter.
   * @param buf The buffer to serialize the string to.
   * @param b The boolean value to serialize.
   */
  public static void writeHBaseBool(final ChannelBuffer buf, final boolean b) {
    buf.writeByte(1);  // Code for Boolean.class in HbaseObjectWritable
    buf.writeByte(b ? 0x01 : 0x00);
  }

  /**
   * Writes an {@link Integer int} as an HBase RPC parameter.
   * @param buf The buffer to serialize the string to.
   * @param v The value to serialize.
   */
  public static void writeHBaseInt(final ChannelBuffer buf, final int v) {
    buf.writeByte(5);  // Code for Integer.class in HbaseObjectWritable
    buf.writeInt(v);
  }

  /**
   * Writes a {@link Long long} as an HBase RPC parameter.
   * @param buf The buffer to serialize the string to.
   * @param v The value to serialize.
   */
  public static void writeHBaseLong(final ChannelBuffer buf, final long v) {
    buf.writeByte(6);  // Code for Long.class in HbaseObjectWritable
    buf.writeLong(v);
  }

  /**
   * Writes a {@link String} as an HBase RPC parameter.
   * @param buf The buffer to serialize the string to.
   * @param s The string to serialize.
   */
  public static void writeHBaseString(final ChannelBuffer buf, final String s) {
    buf.writeByte(10);  // Code for String.class in HbaseObjectWritable
    final byte[] b = s.getBytes(CharsetUtil.UTF_8);
    writeVLong(buf, b.length);
    buf.writeBytes(b);
  }

  /**
   * Writes a byte array as an HBase RPC parameter.
   * @param buf The buffer to serialize the string to.
   * @param b The byte array to serialize.
   */
  public static void writeHBaseByteArray(final ChannelBuffer buf, final byte[] b) {
    buf.writeByte(11);     // Code for byte[].class in HbaseObjectWritable
    writeByteArray(buf, b);
  }

  /**
   * Writes a byte array.
   * @param buf The buffer to serialize the string to.
   * @param b The byte array to serialize.
   */
  public static void writeByteArray(final ChannelBuffer buf, final byte[] b) {
    writeVLong(buf, b.length);
    buf.writeBytes(b);
  }

  /**
   * Serializes a `null' reference.
   * @param buf The buffer to write to.
   */
  public static void writeHBaseNull(final ChannelBuffer buf) {
    buf.writeByte(14);  // Code type for `Writable'.
    buf.writeByte(17);  // Code type for `NullInstance'.
    buf.writeByte(14);  // Code type for `Writable'.
  }

  /**
   * Upper bound on the size of a byte array we de-serialize.
   * This is to prevent HBase from OOM'ing us, should there be a bug or
   * undetected corruption of an RPC on the network, which would turn a
   * an innocuous RPC into something allocating a ton of memory.
   * The Hadoop RPC protocol doesn't do any checksumming as they probably
   * assumed that TCP checksums would be sufficient (they're not).
   */
  public static final long MAX_BYTE_ARRAY_MASK =
    0xFFFFFFFFF0000000L;  // => max = 256MB == 268435455

  /**
   * Verifies that the given length looks like a reasonable array length.
   * This method accepts 0 as a valid length.
   * @param buf The buffer from which the length was read.
   * @param length The length to validate.
   * @throws IllegalArgumentException if the length is negative or
   * suspiciously large.
   */
  public static void checkArrayLength(final ChannelBuffer buf, final long length) {
    // 2 checks in 1.  If any of the high bits are set, we know the value is
    // either too large, or is negative (if the most-significant bit is set).
    if ((length & MAX_BYTE_ARRAY_MASK) != 0) {
      if (length < 0) {
        throw new IllegalArgumentException("Read negative byte array length: "
          + length + " in buf=" + buf + '=' + Bytes.pretty(buf));
      } else {
        throw new IllegalArgumentException("Read byte array length that's too"
          + " large: " + length + " > " + ~MAX_BYTE_ARRAY_MASK + " in buf="
          + buf + '=' + Bytes.pretty(buf));
      }
    }
  }

  /**
   * Verifies that the given array looks like a reasonably big array.
   * This method accepts empty arrays.
   * @param array The array to check.
   * @throws IllegalArgumentException if the length of the array is
   * suspiciously large.
   * @throws NullPointerException if the array is {@code null}.
   */
  public static void checkArrayLength(final byte[] array) {
    if ((array.length & MAX_BYTE_ARRAY_MASK) != 0) {
      if (array.length < 0) {  // Not possible unless there's a JVM bug.
        throw new AssertionError("Negative byte array length: "
                                 + array.length + ' ' + Bytes.pretty(array));
      } else {
        throw new IllegalArgumentException("Byte array length too big: "
          + array.length + " > " + ~MAX_BYTE_ARRAY_MASK);
        // Don't dump the gigantic byte array in the exception message.
      }
    }
  }

  /**
   * Verifies that the given length looks like a reasonable array length.
   * This method does not accept 0 as a valid length.
   * @param buf The buffer from which the length was read.
   * @param length The length to validate.
   * @throws IllegalArgumentException if the length is zero, negative or
   * suspiciously large.
   */
  public static void checkNonEmptyArrayLength(final ChannelBuffer buf,
                                       final long length) {
    if (length == 0) {
      throw new IllegalArgumentException("Read zero-length byte array "
        + " in buf=" + buf + '=' + Bytes.pretty(buf));
    }
    checkArrayLength(buf, length);
  }

  /**
   * Reads a byte array.
   * @param buf The buffer from which to read the array.
   * @return A possibly empty but guaranteed non-{@code null} byte array.
   * @throws IllegalArgumentException if the length we read for the byte array
   * is out of reasonable bounds.
   */
  public static byte[] readByteArray(final ChannelBuffer buf) {
    final long length = readVLong(buf);
    checkArrayLength(buf, length);
    final byte[] b = new byte[(int) length];
    buf.readBytes(b);
    return b;
  }

  /**
   * Reads a string encoded by {@code hadoop.io.WritableUtils#readString}.
   * @throws IllegalArgumentException if the length we read for the string
   * is out of reasonable bounds.
   */
  public static String readHadoopString(final ChannelBuffer buf) {
    final int length = buf.readInt();
    checkArrayLength(buf, length);
    final byte[] s = new byte[length];
    buf.readBytes(s);
    return new String(s, CharsetUtil.UTF_8);
  }

  /**
   * De-serializes a protobuf from the given buffer.
   * <p>
   * The protobuf is assumed to be prefixed by a varint indicating its size.
   * @param buf The buffer to de-serialize the protobuf from.
   * @param parser The protobuf parser to use for this type of protobuf.
   * @return An instance of the de-serialized type.
   * @throws InvalidResponseException if the buffer contained an invalid
   * protobuf that couldn't be de-serialized.
   */
  public static <T> T readProtobuf(final ChannelBuffer buf, final Parser<T> parser) {
    final int length = readProtoBufVarint(buf);
    checkArrayLength(buf, length);
    final byte[] payload;
    final int offset;
    if (buf.hasArray()) {  // Zero copy.
      payload = buf.array();
      offset = buf.arrayOffset() + buf.readerIndex();
      buf.readerIndex(buf.readerIndex() + length);
    } else {  // We have to copy the entire payload out of the buffer :(
      payload = new byte[length];
      buf.readBytes(payload);
      offset = 0;
    }
    try {
      return parser.parseFrom(payload, offset, length);
    } catch (InvalidProtocolBufferException e) {
      final String msg = "Invalid RPC response: length=" + length
        + ", payload=" + Bytes.pretty(payload);
      LOG.error("Invalid RPC from buffer: " + buf);
      throw new InvalidResponseException(msg, e);
    }
  }

  // -------------------------------------- //
  // Variable-length integer value encoding //
  // -------------------------------------- //

  /*
   * Unofficial documentation of the Hadoop VLong encoding
   * *****************************************************
   *
   * The notation used to refer to binary numbers here is `0b' followed by
   * the bits, as is printed by Python's built-in `bin' function for example.
   *
   * Values between
   *   -112 0b10010000
   * and
   *    127 0b01111111
   * (inclusive) are encoded on a single byte using their normal
   * representation.  The boundary "-112" sounds weird at first (and it is)
   * but it'll become clearer once you understand the format.
   *
   * Values outside of the boundaries above are encoded by first having
   * 1 byte of meta-data followed by a variable number of bytes that make up
   * the value being encoded.
   *
   * The "meta-data byte" always starts with the prefix 0b1000.  Its format
   * is as follows:
   *   1 0 0 0 | S | L L L
   * The bit `S' is the sign bit (1 = positive value, 0 = negative, yes
   * that's weird, I would've done it the other way around).
   * The 3 bits labeled `L' indicate how many bytes make up this variable
   * length value.  They're encoded like so:
   *   1 1 1 = 1 byte follows
   *   1 1 0 = 2 bytes follow
   *   1 0 1 = 3 bytes follow
   *   1 0 0 = 4 bytes follow
   *   0 1 1 = 5 bytes follow
   *   0 1 0 = 6 bytes follow
   *   0 0 1 = 7 bytes follow
   *   0 0 0 = 8 bytes follow
   * Yes, this is weird too, it goes backwards, requires more operations to
   * convert the length into something human readable, and makes sorting the
   * numbers unnecessarily complicated.
   * Notice that the prefix wastes 3 bits.  Also, there's no "VInt", all
   * variable length encoded values are eventually transformed to `long'.
   *
   * The remaining bytes are just the original number, as-is, without the
   * unnecessary leading bytes (that are all zeroes).
   *
   * Examples:
   *   42 is encoded as                   00101010 (as-is, 1 byte)
   *  127 is encoded as                   01111111 (as-is, 1 bytes)
   *  128 is encoded as          10001111 10000000 (2 bytes)
   *  255 is encoded as          10001111 11111111 (2 bytes)
   *  256 is encoded as 10001110 00000001 00000000 (3 bytes)
   *   -1 is encoded as                   11111111 (as-is, 1 byte)
   *  -42 is encoded as                   11010110 (as-is, 1 byte)
   * -112 is encoded as                   10010000 (as-is, 1 byte)
   * -113 is encoded as          10000111 01110000 (2 bytes)
   * -256 is encoded as          10000111 11111111 (2 bytes)
   * -257 is encoded as 10000110 00000001 00000000 (3 bytes)
   *
   * The implementations of writeVLong and readVLong below are on average
   * 14% faster than Hadoop's implementation given a uniformly distributed
   * input (lots of values of all sizes), and can be up to 40% faster on
   * certain input sizes (e.g. big values that fit on 8 bytes).  This is due
   * to two main things: fewer arithmetic and logic operations, and processing
   * multiple bytes together when possible.
   * Reading is about 6% faster than writing (negligible difference).
   * My MacBook Pro with a 2.66 GHz Intel Core 2 Duo easily does 5000 calls to
   * readVLong or writeVLong per millisecond.
   *
   * However, since we use Netty, we don't have to deal with the stupid Java
   * I/O library, so unlike Hadoop we don't use DataOutputStream and
   * ByteArrayOutputStream, instead we use ChannelBuffer.  This gives us a
   * significant extra performance boost over Hadoop.  The 14%-60% difference
   * above becomes a 70% to 80% difference!  Yes, that's >4 times faster!  With
   * the code below my MacBook Pro with a 2.66 GHz Intel Core 2 Duo easily
   * does 11000 writeVLong/ms or 13500 readVLong/ms (notice that reading is
   * 18% faster) when using a properly sized dynamicBuffer.  When using a
   * fixed-size buffer, writing (14200/s) is almost as fast as reading
   * (14500/s).
   *
   * So there's really no reason on Earth to use java.io.  Its API is horrible
   * and so is its performance.
   */

  /**
   * Writes a variable-length {@link Long} value.
   * @param buf The buffer to write to.
   * @param n The value to write.
   */
  @SuppressWarnings("fallthrough")
  public static void writeVLong(final ChannelBuffer buf, long n) {
    // All those values can be encoded on 1 byte.
    if (n >= -112 && n <= 127) {
      buf.writeByte((byte) n);
      return;
    }

    // Set the high bit to indicate that more bytes are to come.
    // Both 0x90 and 0x88 have the high bit set (and are thus negative).
    byte b = (byte) 0x90; // 0b10010000
    if (n < 0) {
      n = ~n;
      b = (byte) 0x88;    // 0b10001000
    }

    {
      long tmp = n;
      do {
        tmp >>>= 8;
        // The first time we decrement `b' here, it's going to move the
        // rightmost `1' in `b' to the right, due to the way 2's complement
        // representation works.  So if `n' is positive, and we started with
        // b = 0b10010000, now we'll have b = 0b10001111, which correctly
        // indicates that `n' is positive (5th bit set) and has 1 byte so far
        // (last 3 bits are set).  If `n' is negative, and we started with
        // b = 0b10001000, now we'll have b = 0b10000111, which correctly
        // indicates that `n' is negative (5th bit not set) and has 1 byte.
        // Each time we keep decrementing this value, the last remaining 3
        // bits are going to change according to the format described above.
        b--;
      } while (tmp != 0);
    }

    buf.writeByte(b);
    switch (b & 0x07) {  // Look at the low 3 bits (the length).
      case 0x00:
        buf.writeLong(n);
        break;
      case 0x01:
        buf.writeInt((int) (n >>> 24));
        buf.writeMedium((int) n);
        break;
      case 0x02:
        buf.writeMedium((int) (n >>> 24));
        buf.writeMedium((int) n);
        break;
      case 0x03:
        buf.writeByte((byte) (n >>> 32));
      case 0x04:
        buf.writeInt((int) n);
        break;
      case 0x05:
        buf.writeMedium((int) n);
        break;
      case 0x06:
        buf.writeShort((short) n);
        break;
      case 0x07:
        buf.writeByte((byte) n);
    }
  }

  /**
   * Reads a variable-length {@link Long} value.
   * @param buf The buffer to read from.
   * @return The value read.
   */
  @SuppressWarnings("fallthrough")
  public static long readVLong(final ChannelBuffer buf) {
    byte b = buf.readByte();
    // Unless the first half of the first byte starts with 0xb1000, we're
    // dealing with a single-byte value.
    if ((b & 0xF0) != 0x80) {  // 0xF0 = 0b11110000, 0x80 = 0b10000000
      return b;
    }

    // The value is negative if the 5th bit is 0.
    final boolean negate = (b & 0x08) == 0;    // 0x08 = 0b00001000
    long result = 0;
    switch (b & 0x07) {  // Look at the low 3 bits (the length).
      case 0x00:
        result = buf.readLong();
        break;
      case 0x01:
        result = buf.readUnsignedInt();
        result <<= 32;
        result |= buf.readUnsignedMedium();
        break;
      case 0x02:
        result = buf.readUnsignedMedium();
        result <<= 24;
        result |= buf.readUnsignedMedium();
        break;
      case 0x03:
        b = buf.readByte();
        result <<= 8;
        result |= b & 0xFF;
      case 0x04:
        result <<= 32;
        result |= buf.readUnsignedInt();
        break;
      case 0x05:
        result |= buf.readUnsignedMedium();
        break;
      case 0x06:
        result |= buf.readUnsignedShort();
        break;
      case 0x07:
        b = buf.readByte();
        result <<= 8;
        result |= b & 0xFF;
    }
    return negate ? ~result : result;
  }

  /**
   * Reads a 32-bit variable-length integer value as used in Protocol Buffers.
   * @param buf The buffer to read from.
   * @return The integer read.
   */
  public static int readProtoBufVarint(final ChannelBuffer buf) {
    int result = buf.readByte();
    if (result >= 0) {
      return result;
    }
    result &= 0x7F;
    result |= buf.readByte() << 7;
    if (result >= 0) {
      return result;
    }
    result &= 0x3FFF;
    result |= buf.readByte() << 14;
    if (result >= 0) {
      return result;
    }
    result &= 0x1FFFFF;
    result |= buf.readByte() << 21;
    if (result >= 0) {
      return result;
    }
    result &= 0x0FFFFFFF;
    final byte b = buf.readByte();
    result |= b << 28;
    if (b >= 0) {
      return result;
    }
    throw new IllegalArgumentException("Not a 32 bit varint: " + result
                                       + " (5th byte: " + b + ")");
  }
}
