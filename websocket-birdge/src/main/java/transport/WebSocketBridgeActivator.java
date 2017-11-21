package transport;

import endpoints.WebSocketServiceServiceTracker;
import media.dee.dcms.websocket.WebSocketDispatcher;
import media.dee.dcms.websocket.WebSocketService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import transport.internal.RegisteryWebSocketDispatcher;

public class WebSocketBridgeActivator implements BundleActivator {
    private ServiceRegistration<WebSocketDispatcher> webSocketDispatcherServiceRegistration;
    private ServiceRegistration<WebSocketService> webSocketServiceServiceRegistration;
    private WebSocketServiceServiceTracker webSocketServiceServiceTracker;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        RegisteryWebSocketDispatcher dispatcher = new RegisteryWebSocketDispatcher();
        webSocketServiceServiceRegistration = bundleContext.registerService(WebSocketService.class, dispatcher, null);
        webSocketDispatcherServiceRegistration = bundleContext.registerService(WebSocketDispatcher.class, dispatcher, null);
        webSocketServiceServiceTracker = new WebSocketServiceServiceTracker(bundleContext);
        webSocketServiceServiceTracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        bundleContext.ungetService(webSocketDispatcherServiceRegistration.getReference());
        bundleContext.ungetService(webSocketServiceServiceRegistration.getReference());
        webSocketServiceServiceTracker.close();
    }
}
