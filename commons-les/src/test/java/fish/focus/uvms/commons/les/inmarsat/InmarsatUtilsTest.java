package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;

import static java.lang.Math.pow;
import static org.junit.Assert.*;

public class InmarsatUtilsTest {

    @Test
    public void int2ByteArray() {
        assertArrayEquals(new byte[]{1, 0, 0, 0}, InmarsatUtils.int2ByteArray(1, 4));
        assertArrayEquals(new byte[]{1, 0, 0}, InmarsatUtils.int2ByteArray(1, 3));
        assertArrayEquals(new byte[]{1, 0}, InmarsatUtils.int2ByteArray(1, 2));
        assertArrayEquals(new byte[]{1}, InmarsatUtils.int2ByteArray(1, 1));
        assertArrayEquals(new byte[]{1, 1, 1, 1}, InmarsatUtils.int2ByteArray(0x01010101, 4));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, InmarsatUtils.int2ByteArray(0x04030201, 4));
        assertArrayEquals(new byte[]{1, 2, 3}, InmarsatUtils.int2ByteArray(0x04030201, 3));
        assertArrayEquals(new byte[]{1, 2}, InmarsatUtils.int2ByteArray(0x04030201, 2));
        assertArrayEquals(new byte[]{1, 2, 3}, InmarsatUtils.int2ByteArray(0x030201, 3));

        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, 0, 0}, InmarsatUtils.int2ByteArray(0XFFFF, 4));
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF}, InmarsatUtils.int2ByteArray(0XFFFF, 2));
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, InmarsatUtils.int2ByteArray(0XFFFFFF, 3));

        assertArrayEquals(new byte[]{(byte) 0xAF, (byte) 0xFF, 0, 0}, InmarsatUtils.int2ByteArray(0XFFAF, 4));
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
                InmarsatUtils.int2ByteArray(0xFFFFFFFF, 4));
    }

    @Test
    public void binaryStringToByteArray() throws Exception {
        assertArrayEquals(new byte[]{(byte) 0b10010101}, InmarsatUtils.binaryStringToByteArray("10010101"));
        assertArrayEquals(new byte[]{(byte) 0b10010101, (byte) 0b10010101},
                InmarsatUtils.binaryStringToByteArray("10010101" + "10010101"));
        assertArrayEquals(new byte[]{(byte) 0b11110000, (byte) 0b10010101, (byte) 0b10010101},
                InmarsatUtils.binaryStringToByteArray("11110000" + "10010101" + "10010101"));
    }

    @Test
    public void binaryStringToByteArrayNegative() {
        assertThrows(InmarsatException.class, () -> InmarsatUtils.binaryStringToByteArray("0101"));
    }

    @Test
    public void intToBinary() {
        assertEquals("00010101", InmarsatUtils.intToBinary(0x15, 8));
        assertEquals("01010101", InmarsatUtils.intToBinary(0x55, 8));
        assertEquals("00000000", InmarsatUtils.intToBinary(0x0, 8));

        assertEquals("000000000", InmarsatUtils.intToBinary(0x0, 9));
        assertEquals("001010101", InmarsatUtils.intToBinary(0x55, 9));
        assertEquals("111111111", InmarsatUtils.intToBinary(0x01FF, 9));

    }

    @Test
    public void byteToZeroPaddedString() {
        assertEquals("00010101", InmarsatUtils.byteToZeroPaddedString((byte) 0x15));
        assertEquals("01010101", InmarsatUtils.byteToZeroPaddedString((byte) 0x55));
        assertEquals("11111111", InmarsatUtils.byteToZeroPaddedString((byte) 0xFF));
        assertEquals("00000000", InmarsatUtils.byteToZeroPaddedString((byte) 0x0));

    }

    @Test
    public void toShortMSBLast() {
        assertEquals(1, InmarsatUtils.toUnsignedShort(new byte[]{(byte) 0x1, 0x0}));
        assertEquals(255, InmarsatUtils.toUnsignedShort(new byte[]{(byte) 0xFF, 0x00}));
        assertEquals(256, InmarsatUtils.toUnsignedShort(new byte[]{(byte) 0x00, 0x01}));
        assertEquals(257, InmarsatUtils.toUnsignedShort(new byte[]{(byte) 0x1, 0x1}));
        assertEquals(49922, InmarsatUtils.toUnsignedShort(new byte[]{0x2, -61}));
        assertEquals(65535, InmarsatUtils.toUnsignedShort(new byte[]{(byte) 0xFF, (byte) 0xFF}));
    }

    @Test
    public void toIntMSBLast() {
        assertEquals(1, InmarsatUtils.toUnsignedInt(new byte[]{0x1, 0x0, 0x0, 0x0}));
        assertEquals((int) pow(2, 8) - 1, InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0xFF, 0x0, 0x0, 0x0}));

        assertEquals((int) pow(2, 8), InmarsatUtils.toUnsignedInt(new byte[]{0x0, 0x1, 0x0, 0x0}));
        assertEquals((int) pow(2, 16) - 1,
                InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0xFF, (byte) 0xFF, 0x0, 0x0}));

        assertEquals((int) pow(2, 16), InmarsatUtils.toUnsignedInt(new byte[]{0x0, 0x0, 0x1, 0x0}));
        assertEquals((int) pow(2, 24) - 1,
                InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0}));

        assertEquals((int) pow(2, 24), InmarsatUtils.toUnsignedInt(new byte[]{0x0, 0x0, 0x0, 0x1}));
        assertEquals(Integer.MAX_VALUE,
                InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F}));
        assertEquals((long) pow(2, 32) - 1,
                InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
        assertEquals(527241, InmarsatUtils.toUnsignedInt(new byte[]{(byte) 0x89, 0x0B, 0x08, 0x0}));
    }

    @Test
    public void byteToUnsignedInt() {
        assertEquals(0, InmarsatUtils.byteToUnsignedInt((byte) 256));
        assertEquals(255, InmarsatUtils.byteToUnsignedInt((byte) 255));
        assertEquals(127, InmarsatUtils.byteToUnsignedInt((byte) 127));
        assertEquals(191, InmarsatUtils.byteToUnsignedInt((byte) -65));
        assertEquals(233, InmarsatUtils.byteToUnsignedInt((byte) -23));
        assertEquals(1, InmarsatUtils.byteToUnsignedInt((byte) 1));
        assertEquals(0, InmarsatUtils.byteToUnsignedInt((byte) 0));
        assertEquals(255, InmarsatUtils.byteToUnsignedInt((byte) -1));
        assertEquals(129, InmarsatUtils.byteToUnsignedInt((byte) -127));
        assertEquals(128, InmarsatUtils.byteToUnsignedInt((byte) -128));
        assertEquals(127, InmarsatUtils.byteToUnsignedInt((byte) -129));
        assertEquals(0, InmarsatUtils.byteToUnsignedInt((byte) -256));
        assertEquals(255, InmarsatUtils.byteToUnsignedInt((byte) -257));
    }


    @Test
    public void bytesArrayToHex() {
        byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, (byte) 0x98, 9, (byte) 0xFF, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                127, (byte) 128};
        String actual = InmarsatUtils.bytesArrayToHexString(bytes);
        String expected = "000102030405069809FF0B0C0D0E0F101112137F80";

        assertEquals(expected, actual);

    }

    @Test
    public void hexStringToByteArray() {
        String hexString = "0154890be702";
        byte[] actual = InmarsatUtils.hexStringToByteArray(hexString);
        byte[] expected = new byte[]{0x1, 0x54, (byte) 0x89, 0x0B, (byte) 0xE7, 0x2};

        assertArrayEquals(expected, actual);
    }
}
