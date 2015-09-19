package be.kuleuven.cs.ucsystem;

import be.kuleuven.cs.pbs.PBSParams;
import be.kuleuven.cs.pbs.PBSSignature;
import com.ibm.zurich.idmx.dm.CommitmentOpening;
import com.ibm.zurich.idmx.showproof.ProofSpec;
import com.ibm.zurich.idmx.showproof.predicates.Predicate;

import java.math.BigInteger;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;


public class UCSystemUtil {

    public static enum redeemRespVals{
        OK,
        ALREADY_USED,
        BAD_EPOCH,
        BAD_SIGNATURE,
        BAD_OWNERSHIP,
        BAD_NONCE,
        BAD_OWNERSHIP_SINGLE_PROOF,
        BAD_NONCE_SINGLE_PROOF
    }

    
    private final static int[] LPVals = new int[] { 1 }; //{ 100, 50, 20, 10, 5, 1 };

    public static List<Integer> getLpAmountList(int points) {
        List<Integer> solution = new ArrayList<Integer>();
        int i = 0;

        while ( points > 0 ) {
            if ( LPVals[i] <= points ) {
                solution.add( LPVals[i] );
                points = points - LPVals[i];
            } else {
                ++i;
            }
        }

        return solution;
    }

    /**
     * Parses the credential temporary name in the proof specification file. It
     * assumes a single loyalty credential in the proof.
     *
     * @param spec
     * @return
     */
    public static String getCredTempNameFromProofSpec(ProofSpec spec){

        return (String) spec.getCredTempNames().toArray()[0];
    }

    /**
     * Parses the commitment name in the proof specification file. It assumes a
     * single commitment predicate in the proof.
     *
     * @param spec
     * @return
     */
    public static String getCommNameFromProofSpec(ProofSpec spec){

        String commName = null;
        Vector<Predicate> predicates = spec.getPredicates();
        for(Predicate predicate : predicates){

            if (predicate.getPredicateType().name().matches("COMMITMENT")){
                commName = predicate.toStringPretty()
                        .split("\\(")[1].split(",")[0];
            }

        }
        return commName;
    }

    /**
     * Compares the sizes of multiple collections of objects
     * @param colls
     * @return  True if all the collections have the same size, False otherwise
     */
    public static boolean cmpCollectionSizes(Collection<?>...colls){

        int initialSize = colls[0].size();

        for(Collection<?> collection : colls){

            if(collection.size() != initialSize)
                return false;
        }

        return true;
    }

    /**
     * Returns  the total amount of points in a list of LoyaltyPoint objects
     *
     * @param lpList
     * @return
     */
    public static int getLpListTotalAmount(List<? extends LoyaltyPoint> lpList){

        int total = 0;
        for(LoyaltyPoint lp : lpList){
            total += lp.getAmount();
        }
        return total;
    }
    /**
     *  Returns a string representation of a commitment opening. The values of
     *  the commitment are converted to hexadecimal strings and concatenated using
     *  "," as a separator
     *
     * @param commOpening  An Idemix commitment opening
     * @return  A string with the format: capR,msg,capS,r,n,L_n
     */
    public static String getCommOpeningString(CommitmentOpening commOpening){

        String SEP = "," ; //separator

        return    commOpening.getCapR().toString(16)            + SEP
                + commOpening.getMessageValue().toString(16)    + SEP
                + commOpening.getCapS().toString(16)            + SEP
                + commOpening.getRandom().toString(16)          + SEP
                + commOpening.getN().toString(16)               + SEP
                + commOpening.getN().bitLength();
    }

    /**
     * Loads a commitment opening from a string with the format:
     * capR;msg;capS;r;n;L_n
     *
     * @param commOpeningStr
     * @return  a commitment opening object for a valid input string, null otherwise
     */
    public static CommitmentOpening loadCommOpeningFromString(String commOpeningStr){

        String SEP = "," ;
        String[] strings = commOpeningStr.split(SEP);

        if (strings.length == 6 ){
            return new CommitmentOpening(
                    new BigInteger(strings[0], 16),  //capR
                    new BigInteger(strings[1], 16),  //msg
                    new BigInteger(strings[2], 16),  //capS
                    new BigInteger(strings[3], 16),  // r
                    new BigInteger(strings[4], 16),  // n
                    Integer.valueOf(strings[5])      // bitlength of n
            );
        }else
            return null;
    }
    /**
     *  Returns a string representation of a PBS signature. The values rho,
     *  omega, delta and sigma and the system parameters (p,q,g,y) are converted
     *  to hexadecimal strings and concatenated using ";" as separator
     *
     * @param signature  A PBS signature
     * @return A string with the format: rho,omega,delta,sigma,p,q,g,y,info,msg
     */
    public static String getPBSSignatureString(PBSSignature signature){

        String SEP = "," ; //separator

        return    signature.getRho().toString(16)   + SEP
                + signature.getOmega().toString(16) + SEP
                + signature.getSigma().toString(16) + SEP
                + signature.getDelta().toString(16) + SEP
                + signature.getP().toString(16)     + SEP
                + signature.getQ().toString(16)     + SEP
                + signature.getG().toString(16)     + SEP
                + signature.getY().toString(16)     + SEP
                + signature.getInfo()               + SEP
                + signature.getMsg();
    }

    /**
     * Returns a PBS signature object from its string representation
     *
     * @param signatureStr String representation of a PBS signature using the following format:
     *                     rho,omega,delta,sigma,p,q,g,y,info,msg
     * @return a PBS signature object
     */
    public static PBSSignature loadPBSSignatureFromString(String signatureStr){

        String[] strings = signatureStr.split(",");

        if (strings.length == 10){
            PBSParams params = new PBSParams(
                    new BigInteger (strings[4], 16),  //p
                    new BigInteger (strings[5], 16),  //q
                    new BigInteger (strings[6], 16),  //g
                    new BigInteger (strings[7], 16)); //y

            return  new PBSSignature( params,
                    new BigInteger(strings[0], 16),  //rho
                    new BigInteger(strings[1], 16),  //omega
                    new BigInteger(strings[2], 16),  //sigma
                    new BigInteger(strings[3], 16),  //delta
                    strings[8], strings[9]);         //info, msg
        }else
            return null;
    }
}
