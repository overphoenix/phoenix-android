package threads.magnet.net.portmapping;

import static threads.magnet.net.portmapping.PortMapProtocol.TCP;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.util.Set;

import threads.magnet.Settings;
import threads.magnet.service.LifecycleBinding;
import threads.magnet.service.RuntimeLifecycleBinder;

public class PortMappingInitializer {

    public static void portMappingInitializer(@NonNull Set<PortMapper> portMappers,
                                              @NonNull RuntimeLifecycleBinder lifecycleBinder,
                                              final int acceptorPort) {

        final InetAddress acceptorAddress = Settings.acceptorAddress;

        lifecycleBinder.onStartup(LifecycleBinding.bind(() ->
                portMappers.forEach(m -> mapPort(acceptorPort, acceptorAddress, m)))
                .build());
    }

    private static void mapPort(int acceptorPort, InetAddress acceptorAddress, PortMapper m) {
        m.mapPort(acceptorPort, acceptorAddress.toString(), TCP, "bt acceptor");
    }
}
