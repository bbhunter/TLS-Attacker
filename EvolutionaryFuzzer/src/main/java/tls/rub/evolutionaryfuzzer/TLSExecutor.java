/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package tls.rub.evolutionaryfuzzer;

import de.rub.nds.tlsattacker.tls.config.ConfigHandler;
import de.rub.nds.tlsattacker.tls.config.ConfigHandlerFactory;
import de.rub.nds.tlsattacker.tls.config.GeneralConfig;
import de.rub.nds.tlsattacker.tls.config.WorkflowTraceSerializer;
import de.rub.nds.tlsattacker.tls.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.tls.exceptions.WorkflowExecutionException;
import de.rub.nds.tlsattacker.tls.workflow.GenericWorkflowExecutor;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.transport.TransportHandler;
import de.rub.nds.tlsattacker.util.KeystoreHandler;
import de.rub.nds.tlsattacker.util.UnoptimizedDeepCopy;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.Level;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.crypto.tls.TlsUtils;
import org.bouncycastle.jce.provider.X509CertificateObject;

/**
 * This is an Implementation of an Executor. This Executor is specially designed
 * for the TLS Protocol. The whole Program is not completely generic in this
 * Fashion designed, but with a little work the Fuzzer can be adapted for other
 * Programs, as long as a new Executor is designed.
 *
 * It is also possible to Design a new Executor which executes the
 * Workflowtraces with another Library than TLS-Attacker.
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class TLSExecutor extends Executor {
    private static final Logger LOG = Logger.getLogger(TLSExecutor.class.getName());

    private final WorkflowTrace trace;
    private final TLSServer server;
    private final WorkflowTrace backupTrace;

    /**
     * Constructor for the TLSExecutor
     *
     * @param trace Trace that the Executor should execute
     * @param server Server on which the Executor should execute the Trace
     */
    public TLSExecutor(WorkflowTrace trace, TLSServer server) {
        this.trace = trace;
        this.server = server;
        backupTrace = (WorkflowTrace) UnoptimizedDeepCopy.copy(trace);
    }

    /**
     * Executes the Trace
     */
    @Override
    public void run() {

        try {

            //TODO The agent should not be generated by the Executor, the Modules should be distinct
            Agent agent = new BasicAFLAgent();

            server.start();

            agent.onApplicationStart();
            ConfigHandler configHandler = ConfigHandlerFactory.createConfigHandler("client");

            GeneralConfig gc = new GeneralConfig();
            gc.setLogLevel(Level.OFF);
            configHandler.initialize(gc);

            EvolutionaryFuzzerConfig fc = new EvolutionaryFuzzerConfig();
    
            TransportHandler transportHandler = null;
            long time = System.currentTimeMillis();
            while (transportHandler == null) {
                try {

                    transportHandler = configHandler.initializeTransportHandler(fc);

                } catch (ConfigurationException E) {
                    //TODO Timeout spezifizieren
                    if (time + 10000 < System.currentTimeMillis()) {
                        System.out.println("Could not start Server! Trying to Restart it!");
                        server.restart();
                        time = System.currentTimeMillis();
                    }
                    //TODO what if it really is a configuration exception?
                    //It may happen that the implementation is not ready yet
                }
            }//TODO Change to config
            TlsContext tlsContext = new TlsContext();
            tlsContext.setWorkflowTrace(trace);
            KeyStore ks = KeystoreHandler.loadKeyStore("../resources/rsa1024.jks", "password");
            tlsContext.setKeyStore(ks);
            tlsContext.setPassword("password");
            tlsContext.setAlias("alias");
            java.security.cert.Certificate sunCert = tlsContext.getKeyStore().getCertificate("alias");
            if (sunCert == null) {
                throw new ConfigurationException("The certificate cannot be fetched. Have you provided correct "
                        + "certificate alias and key? (Current alias: " + "alias" + ")");
            }
            byte[] certBytes = sunCert.getEncoded();

            ASN1Primitive asn1Cert = TlsUtils.readDERObject(certBytes);
            org.bouncycastle.asn1.x509.Certificate cert = org.bouncycastle.asn1.x509.Certificate.getInstance(asn1Cert);

            org.bouncycastle.asn1.x509.Certificate[] certs = new org.bouncycastle.asn1.x509.Certificate[1];
            certs[0] = cert;
            org.bouncycastle.crypto.tls.Certificate tlsCerts = new org.bouncycastle.crypto.tls.Certificate(certs);

            X509CertificateObject x509CertObject = new X509CertificateObject(tlsCerts.getCertificateAt(0));

            tlsContext.setX509ServerCertificateObject(x509CertObject);
            //tlsContext.setProtocolVersion(ProtocolVersion.TLS12);
            WorkflowExecutor workflowExecutor = new GenericWorkflowExecutor(transportHandler, tlsContext);

            //tlsContext.setServerCertificate(certificate);
            try {
                workflowExecutor.executeWorkflow();
            } catch (WorkflowExecutionException ex) {
                File f = new File("faulty/" + LogFileIDManager.getInstance().getID());

                try {
                    f.createNewFile();
                    WorkflowTraceSerializer.write(f, trace);
                } catch (JAXBException | IOException Ex) {
                    System.out.println("Could not serialize WorkflowTrace!");
                    Ex.printStackTrace();
                }
                ex.printStackTrace();
            }
            transportHandler.closeConnection();

            //TODO What if server never exited?
            while (!server.exited()) {
            }

            agent.onApplicationStop();
            Result r = agent.collectResults(new File(server.getTracesFolder().getAbsolutePath() + "/" + server.getID()), backupTrace, trace);

            ResultContainer.getInstance().commit(r);
        } catch (Exception E) {
            //TODO
            File f = new File("faulty/" + LogFileIDManager.getInstance().getID());

            try {
                f.createNewFile();
                WorkflowTraceSerializer.write(f, trace);
            } catch (JAXBException | IOException Ex) {
                System.out.println("Could not serialize WorkflowTrace!");
                E.printStackTrace();
            }
        } finally {
            int id = server.getID();
            server.release();
            //Cleanup
            File file = new File(server.getTracesFolder().getAbsolutePath() + "/" + id);
            if (file.exists()) {
                file.delete();
            }
        }

    }

}
