package example.mas;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.UaException;

import java.security.cert.X509Certificate;
import java.util.List;

import static java.util.Collections.singleton;
import static org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText.english;

/**
 * OPC-UA SERVER for Multi-Robot System
 */
public class OPCUAServer {
    
    private static OpcUaServer server;
    private static SimpleNamespace namespace;
    
    public static void start() throws Exception {
        OpcUaServerConfigBuilder builder = new OpcUaServerConfigBuilder();
        
        builder.setIdentityValidator(new CompositeValidator(
            AnonymousIdentityValidator.INSTANCE
        ));
        
        EndpointConfiguration.Builder endpointBuilder = new EndpointConfiguration.Builder();
        endpointBuilder.addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS);
        endpointBuilder.setSecurityPolicy(SecurityPolicy.None);
        endpointBuilder.setBindPort(Config.SERVER_PORT);
        builder.setEndpoints(singleton(endpointBuilder.build()));
        
        builder.setApplicationName(english(Config.SERVER_NAME));
        builder.setApplicationUri("urn:" + HostnameUtil.getHostname() + ":" + Config.SERVER_PORT + "/" + Config.SERVER_NAME);
        builder.setBuildInfo(new BuildInfo("", "", "", "", "", new DateTime()));
        builder.setCertificateManager(new DefaultCertificateManager());
        
        builder.setCertificateValidator(new ServerCertificateValidator() {
            @Override
            public void validateCertificateChain(List<X509Certificate> list, String s) throws UaException {}
            
            @Override
            public void validateCertificateChain(List<X509Certificate> list) throws UaException {}
        });
        
        server = new OpcUaServer(builder.build());
        namespace = new SimpleNamespace(server);
        server.getAddressSpaceManager().register(namespace);
        
        server.startup().get();
        
        System.out.println("✅ OPC-UA Server started on port " + Config.SERVER_PORT);
    }
    
    public static void stop() throws Exception {
        if (server != null) {
            server.shutdown().get();
        }
    }
    
    public static SimpleNamespace getNamespace() {
        return namespace;
    }
}
