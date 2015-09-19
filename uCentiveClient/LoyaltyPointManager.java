package be.kuleuven.cs.uCentiveClient;

import be.kuleuven.cs.ucsystem.LoyaltyPoint;
import be.kuleuven.cs.ucsystem.LoyaltyPointClient;
import be.kuleuven.cs.ucsystem.UCSystemClient;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoyaltyPointManager {

    // PBS public parameters
    private static final String constant_p = "98E462268DA976E92AFDF5987BC071CA6AD039663338F368673808EFBA7F1EEBE03870E773F028C17CD08BAF6F5F4875A3BCE63206A6C995149E8700F2731767DB04C0BEFFA0D929962298E959E13E5495699B1ADD7117CE859D108B7CC758264C3A47FAE858AC6341E98E3ECD109A525F6892B3A5592E868832D5E5621A2955";
    private static final String constant_q = "9B9BA5FF1975869AA2FC2724B20C657872EED7ED";
    private static final String constant_g = "068239A1D2C22C7D86D5CD0DAE791CB1FA0E022AF5F9DF5F72280C2BCD0E94D61E5ACD13ECB5E56D319D65537CAE4AD525EACB8128F4922301F9F927D4B3424F820ECE82CA0A813ED3E81352A00B3A9D390ACE90BCCB8FC979AB9AB95BF6E1541E28A2614F5F1DAF456D5AB1A11275616874BE3D0269EFBF714EABC5D6CDBBF2";
    private static final String constant_y = "B8E6BD6F233424D129334D6ED2B55C7F8D0FD346D9770518AD6903D6D2E1221195505A8D547F6AE4DE4B0BC767160023E5C9789B22AEDA9AD0B405C61F118B40A001A72C86FF93D22649907085B03E9DBFB75E823F458327341F6C4AC04EADC83E2EBF37356F4655F97C38676BA0247F9DE45CD934ABBD2D1C3EB8908B254F7";
    private static BigInteger p = new BigInteger( constant_p, 16 );
    private static BigInteger q = new BigInteger( constant_q, 16 );
    private static BigInteger g = new BigInteger( constant_g, 16 );
    private static BigInteger y = new BigInteger( constant_y, 16 );
    UCSystemClient ucsClient;

    private List<LoyaltyPointClient> loyaltyPointClientList;

    private String key;
    private int amountToRedeem = 0;
    private final double conversionRate = 0.01;
    private static LoyaltyPointManager ourInstance = new LoyaltyPointManager();


    private LoyaltyPointManager() {
        this.ucsClient = new UCSystemClient(p, q, g, y);
        this.loyaltyPointClientList = new ArrayList<LoyaltyPointClient>();
    }

    /**
     * Returns an instance of the singleton only if the DAO object has been
     * assigned with init(), null otherwise
     *
     * @return
     */
    public static LoyaltyPointManager getInstance() {

        return ourInstance;
    }

    /**
     * Returns the amount to redeem in loyalty points
     * @return
     */
    public int getAmountToRedeem() {
        return amountToRedeem;
    }

    /**
     * Returns the amount to redeem in Euros
     * @return
     */
    public Double getAmountToRedeemInEuros(){
        return convertLoyaltyPointsToEuros(amountToRedeem);
    }

    public void setAmountToRedeem(int amountToRedeem) {
        this.amountToRedeem = amountToRedeem;
    }

    public UCSystemClient getUCSClient() {
        return ucsClient;
    }

    public List<LoyaltyPointClient> getLoyaltyPointClientList() {
        return loyaltyPointClientList;
    }



    /**
     *  Add loyalty points to memory and database storage of points
     * @param lpNewList  the list of newly issued points
     * @return
     */
    public void addLoyaltyPoints(List<LoyaltyPointClient> lpNewList){

        if(!lpNewList.isEmpty()){
            loyaltyPointClientList.addAll(lpNewList);
            lpNewList.size();
        }
        else
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Empty list of points to add");

    }

    /**
     * Delete loyalty points from memory and database. Use after
     * redeeming points from the server
     *
     * @param lpList list of points to delete
     */
    public void deleteLoyaltyPoints(List<LoyaltyPointClient> lpList){

        if(!lpList.isEmpty()){
            loyaltyPointClientList.removeAll(lpList);
        }
        else
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Empty list of points to delete");
    }


    /**
     *  Deletes all the loyalty points in memory and DB
     */
    public void deleteLoyaltyPointsAll(){

        loyaltyPointClientList.clear();

    }


    /**
     * Convert loyalty points to money units (Euros)
     * @param lpAmount
     * @return
     */
    public Double convertLoyaltyPointsToEuros(int lpAmount){
         return lpAmount * conversionRate;
    }


    /**
     *  Selects the points to be reedemed based on the amount selected by the user. It returns the
     *  exact or higher amount of points based on the points available.
     * @param sum
     * @return
     */
    public List<LoyaltyPointClient> selectLoyaltyPointsToRedeem(int sum){


        List<LoyaltyPointClient> collectedSignatures = new ArrayList<LoyaltyPointClient>();
        List<LoyaltyPointClient> listOfSignaturesToShow = new ArrayList<LoyaltyPointClient>();
        List<LoyaltyPointClient> reserveList = new ArrayList<LoyaltyPointClient>();

        collectedSignatures.addAll(getLoyaltyPointClientList());

        // METHOD DESCRIPTION: Initially, we iteratively choose the highest
        // number of points (max) in the list
        // of given signatures and if it is smaller than the given required
        // number of points, the signature is
        // added to the list of signatures to be shown and the required number
        // of points is reduced by this
        // max value; in case 'max' is greater than the required number of
        // points, that signature is moved to
        // the 'reservedList' to be used in the next step. After all the
        // elements are removed from the list in
        // this way, and in case there are still signatures missing to reach the
        // required number of points,
        // the reserveList is checked for the number of points that will give
        // the minimal sum greater than the
        // required number of points.

        // choosing batches of points that will give the required sum and
        // creating a list of their indices

        Collections.sort(collectedSignatures);

        int currentGoal = sum;
        for (int i = collectedSignatures.size() - 1; i >= 0; --i) {
            LoyaltyPointClient current = collectedSignatures.get(i);

            if (current.getAmount() > currentGoal) {
                reserveList.add(current);
            } else {
                currentGoal = currentGoal - current.getAmount();
                listOfSignaturesToShow.add(current);
            }
        }

        // In case the selected set of points does not yet sum to the required
        // number:
         if (currentGoal > 0) {
            LoyaltyPointClient lastOneFromReserve = reserveList.get(reserveList
                    .size() - 1);
            listOfSignaturesToShow.add(lastOneFromReserve);

            Collections.sort(listOfSignaturesToShow);
            int auxiliary_sum = 0;
            int auxiliary_index = listOfSignaturesToShow.size() - 1;
            List<LoyaltyPointClient> newListOfSignaturesToShow = new ArrayList<LoyaltyPointClient>();
            while (auxiliary_sum < sum) {
                LoyaltyPointClient current = listOfSignaturesToShow
                        .get(auxiliary_index);
                auxiliary_index = auxiliary_index - 1;
                auxiliary_sum = auxiliary_sum + current.getAmount();
                newListOfSignaturesToShow.add(current);
            }
            return newListOfSignaturesToShow;

        }
        else {

            // return new ArrayList<LoyaltyPointsStructure>();

            return listOfSignaturesToShow;

        }
    }


    /**
     * Returns the total amount of points in memory
     *
     * @return
     */
    public int getLpTotalAmount(){

        int total =0;
        for(LoyaltyPoint lp : loyaltyPointClientList)
            total += lp.getAmount();

        return total;
    }

}
