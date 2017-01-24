/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package tlsattacker.fuzzer.mutator;

import tlsattacker.fuzzer.mutator.certificate.CertificateMutator;
import tlsattacker.fuzzer.config.EvolutionaryFuzzerConfig;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowTrace;
import java.util.Random;
import tlsattacker.fuzzer.testvector.TestVector;
import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

/**
 * A mutator implementation that does not modify the TestVectors
 * 
 * @author Robert Merget - robert.merget@rub.de
 */
public class NoneMutator extends Mutator {

    /**
     * The name of the Mutator when referred by command line
     */
    public static final String optionName = "none";

    /**
     * The config to use
     * 
     * @param evoConfig
     * @param certMutator
     */

    public NoneMutator(EvolutionaryFuzzerConfig evoConfig, CertificateMutator certMutator) {
        super(evoConfig, certMutator);
    }

    /**
     * Returns a random TestVector and does not modify it
     * 
     * @return A random TestVecot
     */
    @Override
    public TestVector getNewMutation() {
        Random r = new Random();
        // chose a random trace from the list
        TestVector tempVector = null;
        WorkflowTrace trace = null;

        if (goodVectorsExist()) {
            try {
                tempVector = chooseRandomTestVectorFromFolder(new File(config.getOutputFolder() + "/good/"));
            } catch (IOException | JAXBException | XMLStreamException ex) {
                LOGGER.error("Could not read good TestVector", ex);
            }
        } else if (archiveVectorsExist()) {
            try {
                tempVector = chooseRandomTestVectorFromFolder(new File(config.getArchiveFolder()));
            } catch (IOException | JAXBException | XMLStreamException ex) {
                LOGGER.error("Could not read archive TestVector", ex);
            }
        }
        if (tempVector == null) {
            tempVector = new TestVector(new WorkflowTrace(), certMutator.getServerCertificateStructure(),
                    certMutator.getClientCertificateStructure(), config.getActionExecutorConfig()
                            .getRandomExecutorType(), null);
        }
        tempVector.getTrace().reset();
        tempVector.getTrace().makeGeneric();
        return tempVector;

    }
}