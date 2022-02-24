package fish.focus.uvms.plugins.inmarsat;


import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import fish.focus.uvms.plugins.inmarsat.telnetserversimulator.SocketServer;
import java.io.File;

@ArquillianSuiteDeployment
public class _BuildTestDeployment {


    @Deployment(name = "telnet", order = 1)
    public static Archive<?> telnet() {

        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "telnet.war");
        testWar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
        testWar.addAsLibraries(files);
        testWar.addPackages(true, "fish.focus.uvms.plugins.inmarsat.telnetserversimulator");
        testWar.addPackages(true, "fish.focus.uvms.plugins.inmarsat.telnetclient");
        testWar.addClass(SocketServer.class);
        return testWar;
    }

    @Deployment(name = "exchangesim", order = 2)
    public static Archive<?> createExchangeRegisterSimulation() {

        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "exchange.war");
        testWar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeAndTestDependencies().resolve()
                .withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addClass(ExchangePluginRegistrationSimulatorListener.class);
        testWar.addClass(ExchangePluginRegistrationSimulatorProducer.class);
        testWar.addClass(PluginMessageProducerForSimulator.class);


        return testWar;
    }


    @Deployment(name = "normal", order = 3)
    public static Archive<?> createDeployment() {

        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war");
        testWar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeAndTestDependencies().resolve()
                .withTransitivity().asFile();
        testWar.addAsLibraries(files);


        testWar.addPackages(true, "fish.focus.uvms.plugins.inmarsat");

        //testWar.deleteClass(InmarsatPluginImpl.class);
        //testWar.addClass(InmarsatPluginMock.class);

        testWar.addAsResource("plugin.properties", "plugin.properties");
        testWar.addAsResource("capabilities.properties", "capabilities.properties");
        testWar.addAsResource("settings.properties", "settings.properties");


        //testWar.addAsResource("persistence-integration.xml", "META-INF/persistence.xml");

        return testWar;
    }
}