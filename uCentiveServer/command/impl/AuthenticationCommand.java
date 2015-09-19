package org.mobcom.inshopnito.server.command.impl;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Random;

import org.mobcom.inshopnito.server.InShopnitoServer;
import org.mobcom.inshopnito.server.SessionManager;
import org.mobcom.inshopnito.server.command.AbstractCommand;

import be.kuleuven.cs.priman.Priman;
import be.kuleuven.cs.priman.Technology;
import be.kuleuven.cs.priman.connection.Connection;
import be.kuleuven.cs.priman.credential.claim.representation.policy.Policy;
import be.kuleuven.cs.priman.credential.proof.Nonce;
import be.kuleuven.cs.priman.credential.proof.Proof;
import be.kuleuven.cs.priman.credential.proof.ProofTranscript;
import be.kuleuven.cs.priman.exception.ConnectionException;
import be.kuleuven.cs.priman.exception.ProviderNotFoundException;
import be.kuleuven.cs.priman.manager.CredentialManager;
import be.kuleuven.cs.priman.manager.PersistenceManager;

public class AuthenticationCommand extends AbstractCommand {
	public final static String COMMANDWORD = "AUTHENTICATE";
	private static Random rand = new SecureRandom();
	@Override
	public void execute() {
		URI policyURI = URI.create("http://epoll.pw:8084/public/AuthenticationPolicy/policy.xml");
		Priman priman = Priman.getInstance();
		CredentialManager cm = priman.getCredentialManager();
		PersistenceManager pm = priman.getPersistenceManager();
		
		Technology credTech = pm.load(InShopnitoServer.HOME.resolve("config/credTech.xml"));
		Policy pol = pm.load(policyURI);
		try {
			Nonce nonce = cm.generateNonce(credTech);
			Connection conn = super.getConnection();
			conn.send(nonce);

			Object serializedProof = conn.receive();
			Proof proof = cm.deSerialize(serializedProof);
			
			ProofTranscript pt = proof.validate(nonce);
			
			boolean policyOK = proof.satisfiesPolicy(pol);
			
			if(proof.isValid() && policyOK){
				
				String id = AuthenticationCommand.rand.nextLong()+"";
				ArrayList<String> revealedInfo = new ArrayList<String>();
				for(Entry<String, String> entry:pt.getRevealedValues().entrySet()){
					revealedInfo.add(entry.getKey()+"="+entry.getValue());
				}
				revealedInfo.addAll(pt.getProvenValues().keySet());
				
				SessionManager.getInstance().addSession(id, revealedInfo, pt.getSignedContent());
				System.out.println(pt.getSignedContent());
				conn.send(id+"");
			}
			else{
				conn.send("NOK");

			}
			
		} catch (ProviderNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            try {
               getConnection().close();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
	}
	@Override
	public String getCommandWord() {
		return AuthenticationCommand.COMMANDWORD;
	}

}
