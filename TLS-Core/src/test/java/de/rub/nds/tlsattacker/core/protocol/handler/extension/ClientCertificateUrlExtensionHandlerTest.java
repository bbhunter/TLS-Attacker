/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.handler.extension;

import de.rub.nds.tlsattacker.core.protocol.message.extension.ClientCertificateUrlExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.parser.extension.ClientCertificateUrlExtensionParser;
import de.rub.nds.tlsattacker.core.protocol.preparator.extension.ClientCertificateUrlExtensionPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.extension.ClientCertificateUrlExtensionSerializer;
import de.rub.nds.tlsattacker.core.state.TlsContext;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Matthias Terlinde <matthias.terlinde@rub.de>
 */
public class ClientCertificateUrlExtensionHandlerTest {
    private ClientCertificateUrlExtensionHandler handler;
    private TlsContext context;

    @Before
    public void setUp() {
        context = new TlsContext();
        handler = new ClientCertificateUrlExtensionHandler(context);
    }

    @Test
    public void testAdjustTLSContext() {
        ClientCertificateUrlExtensionMessage message = new ClientCertificateUrlExtensionMessage();

        handler.adjustTLSContext(message);

        assertTrue(context.isClientCertificateUrlExtensionIsPresent());
    }

    @Test
    public void testGetParser() {
        assertTrue(handler.getParser(new byte[0], 0) instanceof ClientCertificateUrlExtensionParser);
    }

    @Test
    public void testGetPreparator() {
        assertTrue(handler.getPreparator(new ClientCertificateUrlExtensionMessage()) instanceof ClientCertificateUrlExtensionPreparator);
    }

    @Test
    public void testGetSerializer() {
        assertTrue(handler.getSerializer(new ClientCertificateUrlExtensionMessage()) instanceof ClientCertificateUrlExtensionSerializer);
    }
}