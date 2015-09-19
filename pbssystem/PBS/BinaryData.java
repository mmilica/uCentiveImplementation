package be.kuleuven.cs.pbs;

import java.math.BigInteger;

public class BinaryData {
    private final byte[] data;

    public BinaryData( BigInteger n ) {
        this.data = n.toByteArray();
    }

    public BinaryData( byte[] data ) {
        this.data = data;
    }

    BinaryData( int n ) {
        byte[] data = new byte[4];

        data[0] = (byte) ( n >> 24 );
        data[1] = (byte) ( n >> 16 );
        data[2] = (byte) ( n >> 8 );
        data[3] = (byte) n;

        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return data.length;
    }

    public static BinaryData concatenate( BinaryData... blocks ) {
        int totalSize = computeTotalSize( blocks );
        byte[] buffer = new byte[totalSize];

        copyBlocksToBuffer( blocks, buffer );

        return new BinaryData( buffer );
    }

    private static int computeTotalSize( BinaryData... blocks ) {
        int total = 0;

        for ( BinaryData block : blocks ) {
            total += block.getSize();
        }

        return total;
    }

    private static void copyBlocksToBuffer( BinaryData[] blocks, byte[] buffer ) {
        int bufferIndex = 0;

        for ( BinaryData block : blocks ) {
            System.arraycopy( block.getData(), 0, buffer, bufferIndex, block.getSize() );
            bufferIndex += block.getSize();
        }
    }
}
