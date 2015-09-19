package be.kuleuven.cs.uCentiveClient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import be.kuleuven.cs.ucsystem.LoyaltyPointClient;
import be.kuleuven.cs.ucsystem.UCSystemClient;
import be.kuleuven.cs.ucsystem.UCSystemUtil;
import be.kuleuven.cs.priman.Priman;
import be.kuleuven.cs.priman.connection.Connection;
import be.kuleuven.cs.priman.connection.ConnectionParameters;
import be.kuleuven.cs.priman.credential.Credential;
import be.kuleuven.cs.priman.exception.ConnectionException;
import be.kuleuven.cs.priman.exception.ProviderNotFoundException;
import be.kuleuven.cs.priman.manager.ConnectionManager;
import be.kuleuven.cs.priman.manager.PersistenceManager;
import be.kuleuven.cs.primanprovider.credential.idemix.IdemixCredential;
import be.kuleuven.cs.primanprovider.credential.idemix.utils.PrimanParser;
import be.kuleuven.cs.primanprovider.persistence.xml.PrimanXMLParser;
import be.kuleuven.cs.uCentiveClient.R;

import com.ibm.zurich.idmx.dm.MasterSecret;
import com.ibm.zurich.idmx.showproof.ProofSpec;

import org.mobcom.inshopnito.server.command.CommandMessage;
import org.mobcom.inshopnito.server.command.impl.GetLoyaltyPointsCommand;
import org.mobcom.inshopnito.server.command.impl.RedeemLoyaltyPointsCommand;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class uCentiveClient extends Activity {


    private TextView textViewLPTotal;
    private EditText editTextRounds;
    private TextView textViewRounds;
    private UCSystemClient ucsClient;
    private int rounds = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        LoyaltyPointManager lpMgr = LoyaltyPointManager.getInstance();
        ucsClient = lpMgr.getUCSClient();

        textViewLPTotal = (TextView) findViewById(R.id.textViewLPTotal);
        textViewLPTotal.setText(lpMgr.getLpTotalAmount() + " points");

        editTextRounds = (EditText) findViewById(R.id.editTextRounds);
        textViewRounds = (TextView) findViewById(R.id.textViewRounds);
        textViewRounds.setText(rounds + "");
        initialize();

    }

    private void initialize(){


        class myClass extends AsyncTask<Void, Void, Integer>{
            @Override
            protected Integer doInBackground(Void... params) {
                //Priman.getInstance().setContext(this);

                File file = new File(uCentiveClient.this.getFilesDir().toURI().resolve("ClientTrustStore.bks"));
                System.out.println(file.getAbsolutePath().toString());
                if(!file.exists()){
                    try {
                        InputStream is = uCentiveClient.this.getAssets().open("ClientTrustStore.bks");
                        FileOutputStream fos = new FileOutputStream(file);
                        byte buf[]=new byte[1024];
                        int len;
                        while((len=is.read(buf))>0)
                            fos.write(buf,0,len);
                        fos.close();
                        is.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                try {
                    URI filesDir = uCentiveClient.this.getApplicationContext().getFilesDir().toURI();
                    if(!new File(filesDir.resolve("LoyaltyCredENC.xml")).exists()){

                        InputStream is = getAssets().open("LoyaltyCred.xml");
                        InputSource iSource = new InputSource(is);
                        Credential cred = (Credential) new PrimanXMLParser().parse(iSource);
                        InputStream is2 = getAssets().open("Loyaltyms.xml");
                        InputSource iSource2 = new InputSource(is2);
                        MasterSecret ms = (MasterSecret) new PrimanXMLParser().parse(iSource2);

                        cred.setSecret(ms);
                        cred.setName("Loyalty Credential");
                        Priman.getInstance().getPersistenceManager().getCredentials().add(cred);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return 1;
            }

        }

        new myClass().execute();

    }

    public ConnectionParameters getConnectionParameters(){
        InputSource iSource = null;
        try {
            InputStream is = getAssets().open("client-ssl.param");
            iSource = new InputSource(is);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return (ConnectionParameters) new PrimanXMLParser().parse(iSource);

    }

    public void setRounds(View view){

        String roundsStr = editTextRounds.getText().toString();
        if(!roundsStr.isEmpty()){
            rounds = Integer.valueOf(roundsStr);
            textViewRounds.setText(rounds + "");
        }else
            showToast("Wrong number of rounds", Toast.LENGTH_LONG);
    }

    public void  getLoyaltyPoints(View view){

        EditText editTextLP =  (EditText) findViewById(R.id.editTextLP);

        String amountStr = editTextLP.getText().toString();

        if(!amountStr.isEmpty()){
            Integer amount = Integer.valueOf(amountStr);
            new getLoyaltyPointsTask().execute(amount);
        }else
            showToast("Select amount of points to be issued", Toast.LENGTH_LONG);


    }


    public void redeemLoyaltyPoints(View view){

        EditText editTextRedeemLP =  (EditText) findViewById(R.id.editTextRedeemLP);
        LoyaltyPointManager lpMgr = LoyaltyPointManager.getInstance();

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.RadioGroup);
        int checkedRadioButton = radioGroup.getCheckedRadioButtonId();

        int selection = 0;

        switch(checkedRadioButton){
            case R.id.radioButtonSingleProof:
                selection = 1;
                break;
            case R.id.radioButtonMultiProof:
                selection = 2;
                break;
            default:
                selection = 1;
        }

        String amountStr = editTextRedeemLP.getText().toString();

        if(!amountStr.isEmpty()){

            Integer amount = Integer.valueOf(amountStr);

            if(amount > lpMgr.getLpTotalAmount())
                showToast("Not enough loyalty points", Toast.LENGTH_LONG);
            else
                new redeemLoyaltyPointsTask().execute(amount, selection);
        }else
            showToast("Select amount of points to be redeemed", Toast.LENGTH_LONG);

    }

    public void  resetLoyaltyPoints(View view){

        LoyaltyPointManager lpMgr = LoyaltyPointManager.getInstance();
        lpMgr.deleteLoyaltyPointsAll();
        textViewLPTotal.setText(lpMgr.getLpTotalAmount() + " points");

    }

    /**
     *  Displays a toast message in the loyalty activity
     *  for a given duration time.
     *
     * @param msg
     * @param duration
     *
     */
    private void showToast(String msg, int duration){

        Context context = getApplicationContext();

        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();

    }

    private class getLoyaltyPointsTask extends AsyncTask<Integer, Void, List<LoyaltyPointClient> > {

        @Override
        protected List<LoyaltyPointClient> doInBackground(Integer... amount) {

            ArrayList<Integer> amountList = (ArrayList<Integer>)
                    UCSystemUtil.getLpAmountList(amount[0]);

            Priman priman = Priman.getInstance();
            ConnectionManager cmgr = priman.getConnectionManager();
            PersistenceManager perMan = priman.getPersistenceManager();


            List<LoyaltyPointClient> newLoyaltyPointsList = null;
            List<LoyaltyPointClient> newLoyaltyPointsListTotal = new ArrayList<LoyaltyPointClient>();

            ConnectionParameters connParam = getConnectionParameters();
            Connection conn = null;

            // Get Idemix credential from Priman credential.
            Credential primanCred = perMan.getCredentials().get(0);
            com.ibm.zurich.idmx.dm.Credential cred = ((IdemixCredential) primanCred).
                    getIdemixCredential();

            CommandMessage cm = new CommandMessage(GetLoyaltyPointsCommand.COMMANDWORD,
                    "0", amountList);

            for(int i =0; i<rounds; i++){

                try {
                    conn = cmgr.getConnection(connParam);
                    conn.send(cm);
                    newLoyaltyPointsList = ucsClient.getLoyaltyPoint(conn, cred, amountList, false);
                    conn.close();
                    newLoyaltyPointsListTotal.addAll(newLoyaltyPointsList);
                } catch (ProviderNotFoundException e) {
                    e.printStackTrace();
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
            }

            return newLoyaltyPointsListTotal;
        }

        @Override
        protected void onPostExecute(List<LoyaltyPointClient> result){

            // Add new loyalty points to the array in memory and to DB
            LoyaltyPointManager lpMgr =  LoyaltyPointManager.getInstance();
            lpMgr.addLoyaltyPoints(result);
            textViewLPTotal.setText( lpMgr.getLpTotalAmount() + " points");
            ucsClient.setCounterEarning(0);

        }
    }

    private class redeemLoyaltyPointsTask extends AsyncTask<Integer, Void, Integer>{

        @Override
        protected Integer doInBackground(Integer... amount) {

            int amountToRedeem = amount[0];
            int selection = amount[1];
            int redeemedAmount = 0;

            Priman priman = Priman.getInstance();
            ConnectionManager cmgr = priman.getConnectionManager();
            PersistenceManager perMan = priman.getPersistenceManager();
            LoyaltyPointManager lpMgr =  LoyaltyPointManager.getInstance();


            ConnectionParameters connParam = getConnectionParameters();
            Connection conn = null;

            // Get Idemix credential from Priman credential.
            Credential primanCred = perMan.getCredentials().get(0);
            com.ibm.zurich.idmx.dm.Credential cred = ((IdemixCredential) primanCred).
                    getIdemixCredential();

            MasterSecret ms = (MasterSecret) primanCred.getSecret();

            ProofSpec spec = null;

            try {
                //Load proof spec
                InputStream is = getAssets().open("LoyaltyProofSpec.xml");
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();

                String specStr = new String(buffer);
                spec = (ProofSpec) PrimanParser.getInstance().parse(specStr);

                for(int i =0; i< rounds; i++){

                    List<LoyaltyPointClient> lpRedeemList =
                            lpMgr.selectLoyaltyPointsToRedeem(amountToRedeem);

                for(LoyaltyPointClient lp : lpRedeemList) {
                    redeemedAmount += lp.getAmount();
//                    Log.i("REDEEM", "REDEEM: " + lp.getAmount() + " points");
                }

                    List<LoyaltyPointClient> lpChangeList = null;

                    conn = cmgr.getConnection(connParam);

                    CommandMessage cm = new CommandMessage(RedeemLoyaltyPointsCommand.COMMANDWORD,
                            "0", specStr, selection);

                    conn.send(cm);
                    switch(selection){
                        case 1:
                            //Single proof
                            lpChangeList = ucsClient.redeemLoyaltyPointSingleProofNonce(conn, cred,
                                    spec, lpRedeemList, amountToRedeem, ms, false);
                            break;
                        case 2:
                            //Multiple proof (one per uCent)
                            lpChangeList = ucsClient.redeemLoyaltyPointNonce(conn, cred, spec, lpRedeemList,
                                    amountToRedeem, ms, false);
                            break;
                        default:
                            lpChangeList = ucsClient.redeemLoyaltyPointSingleProofNonce(conn, cred,
                                    spec, lpRedeemList, amountToRedeem, ms, false);
                    }

                    //Delete redeemed points in memory and database
                    lpMgr.deleteLoyaltyPoints(lpRedeemList);

                    // Add loyalty point change if any
                    if(lpChangeList !=null){
                        lpMgr.addLoyaltyPoints(lpChangeList);
                    }

                    conn.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (ProviderNotFoundException e) {
                e.printStackTrace();
            }

            return redeemedAmount;
        }

        @Override
        protected void onPostExecute(Integer redeemedAmount){
            LoyaltyPointManager lpMgr =  LoyaltyPointManager.getInstance();
            textViewLPTotal.setText(lpMgr.getLpTotalAmount() + " points");
            ucsClient.setCounterRedeeming(0);
            showToast(redeemedAmount + " points redeemed!", Toast.LENGTH_LONG);
        }
    }

}
