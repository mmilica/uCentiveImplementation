package be.kuleuven.cs.pbs;


import be.kuleuven.cs.ucsystem.LoyaltyPointClient;

import com.ibm.zurich.idmx.dm.CommitmentOpening;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;


public class PBSUtil {

    public static final Random rng = new SecureRandom();

    public static BigInteger randomBigInteger( BigInteger maximumExclusive ) {
        int log2 = maximumExclusive.bitLength();
        BigInteger result;

        do {
            result = new BigInteger( log2, rng );
        } while ( greaterThanOrEqualTo( result, maximumExclusive ) );

        return result;
    }

    public static BigInteger hashF( BigInteger p, BigInteger q, String info ) {
        byte[] hash;

        {
            BigInteger pMinusOne = p.subtract( BigInteger.ONE );
            BigInteger qSquared = q.multiply( q );

            boolean check1 = pMinusOne.mod( q ).equals( BigInteger.ZERO );
            boolean check2 = !pMinusOne.mod( qSquared ).equals( BigInteger.ZERO );

            if ( !( check1 && check2 ) ) {
                throw new IllegalArgumentException( "p and q are not chosen correctly" );
            }
        }

        BigInteger pMinusOne = p.subtract( BigInteger.ONE );
        BigInteger power = pMinusOne.divide( q );
        BinaryData infoAsBinaryData = new BinaryData( ( info ).getBytes() );


        try {
            MessageDigest sha1 = MessageDigest.getInstance( "SHA-1" );
            sha1.reset();
            sha1.update( infoAsBinaryData.getData() );
            hash = sha1.digest();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException( e );
        }

        BigInteger hashAsBigInteger = new BigInteger( hash );
        power = pMinusOne.divide( q );
        return hashAsBigInteger.modPow( power, p );

    }

    public static BigInteger hashH( BigInteger alpha, BigInteger beta, BigInteger z, String msg, BigInteger q ) {
        {
            BinaryData alphaAsBinary = new BinaryData( alpha );
            BinaryData betaAsBinary = new BinaryData( beta );
            BinaryData zAsBinary = new BinaryData( z );
            BinaryData messageAsBinary = new BinaryData( ( msg ).getBytes() );
            BinaryData data = BinaryData.concatenate( alphaAsBinary, betaAsBinary, zAsBinary, messageAsBinary );

            try {
                MessageDigest sha1 = MessageDigest.getInstance( "SHA-1" );
                sha1.reset();
                sha1.update( data.getData() );
                byte[] result = sha1.digest();

                return new BigInteger( result ).mod( q );
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException( e );
            }
        }
    }

    public static boolean lessThan( BigInteger a, BigInteger b ) {
        return a.compareTo( b ) < 0;
    }

    public static boolean greaterThan( BigInteger a, BigInteger b ) {
        return a.compareTo( b ) > 0;
    }

    public static boolean lessThanOrEqualTo( BigInteger a, BigInteger b ) {
        return a.compareTo( b ) <= 0;
    }

    public static boolean greaterThanOrEqualTo( BigInteger a, BigInteger b ) {
        return a.compareTo( b ) >= 0;
    }
}
