package media.dee.dcms.webapp.cms.internal;

import media.dee.dcms.components.AdminModule;
import media.dee.dcms.webapp.cms.components.GUIComponent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import javax.websocket.Session;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component(property= EventConstants.EVENT_TOPIC + "=components/essential/bundles", immediate = true)
public class ComponentConnector implements IComponentConnector, EventHandler {
    private final AtomicReference<LogService> logRef = new AtomicReference<>();
    private final AtomicReference<WebSocketEndpoint> wsEndpoint = new AtomicReference<>();
    private final List<GUIComponent> guiComponents = new LinkedList<>();
    private final List<HttpService> httpServiceList = new LinkedList<>();

    private static JSONObject getInstallCommand(GUIComponent component){
        AdminModule adminModule = component.getClass().getAnnotation(AdminModule.class);
        Bundle bundle = FrameworkUtil.getBundle(component.getClass());
        JSONObject jsonObject = new JSONObject();
        JSONObject bundleObject = new JSONObject();
        try {
            jsonObject.put("action", "bundle.install");
            jsonObject.put("bundle", bundleObject);
            bundleObject.put("bundlePath", String.format("/cms/%s/%s%s.js", bundle.getSymbolicName(), bundle.getVersion().toString(), adminModule.value() ));
            bundleObject.put("SymbolicName", bundle.getSymbolicName() );
            bundleObject.put("Version", bundle.getVersion().toString() );
        } catch (JSONException e) {
            //error
        }
        return jsonObject;
    }

    private static JSONObject getUnInstallCommand(GUIComponent component){
        AdminModule adminModule = component.getClass().getAnnotation(AdminModule.class);
        Bundle bundle = FrameworkUtil.getBundle(component.getClass());
        JSONObject jsonObject = new JSONObject();
        JSONObject bundleObject = new JSONObject();
        try {
            jsonObject.put("action", "bundle.uninstall");
            jsonObject.put("bundle", bundleObject);
            bundleObject.put("bundlePath", String.format("/cms/%s/%s%s.js", bundle.getSymbolicName(), bundle.getVersion().toString(), adminModule.value() ));
            bundleObject.put("SymbolicName", bundle.getSymbolicName() );
            bundleObject.put("Version", bundle.getVersion().toString() );
        } catch (JSONException e) {
            //error
        }
        return jsonObject;
    }

    private void registerModuleResources(HttpService httpService, GUIComponent guiComponent){
        AdminModule adminModule = guiComponent.getClass().getAnnotation(AdminModule.class);
        String path = adminModule.value();
        File fPath = new File(path);
        File dir = fPath.getParentFile();
        Bundle bundle = FrameworkUtil.getBundle(guiComponent.getClass());
        ServiceReference<HttpService> ref = bundle.getBundleContext().getServiceReference(HttpService.class);
        HttpService bundleHttpService = bundle.getBundleContext().getService(ref);
        try {
            bundleHttpService.registerResources(String.format("/cms/%s/%s%s", bundle.getSymbolicName(), bundle.getVersion().toString(), dir ), dir.toString(), null );
        } catch (Exception exception) {
            logRef.get().log(LogService.LOG_ERROR, String.format("Error while registering httpService Resource of GUIComponent: %s", guiComponent.getClass().getName()), exception);
        }
    }

    private void unRegisterModuleResources(HttpService httpService, GUIComponent guiComponent){
        AdminModule adminModule = guiComponent.getClass().getAnnotation(AdminModule.class);
        String path = adminModule.value();
        File fPath = new File(path);
        File dir = fPath.getParentFile();
        Bundle bundle = FrameworkUtil.getBundle(guiComponent.getClass());
        ServiceReference<HttpService> ref = bundle.getBundleContext().getServiceReference(HttpService.class);
        HttpService bundleHttpService = bundle.getBundleContext().getService(ref);
        try {
            bundleHttpService.unregister( String.format("/cms/%s/%s%s", bundle.getSymbolicName(), bundle.getVersion().toString(), dir ) );
        } catch (Exception exception) {
            logRef.get().log(LogService.LOG_ERROR, String.format("Error while un-registering httpService Resource of GUIComponent: %s", guiComponent.getClass().getName()), exception);
        }
        bundle.getBundleContext().ungetService(ref);
    }


    private Map<String, BiConsumer<JSONObject, Consumer<JSONObject>>> commands = new HashMap<>();

    public ComponentConnector(){
        commands.put("list", (message, sendMessage)->{
            int rest = 100;

            List<JSONObject> bundles = guiComponents.stream()
                    .filter( guiComponent -> guiComponent.getClass().getAnnotation(AdminModule.class).autoInstall() )
                    .map( (component)->{
                        AdminModule adminModule = component.getClass().getAnnotation(AdminModule.class);
                        Bundle bundle = FrameworkUtil.getBundle(component.getClass());
                        JSONObject bundleObject = new JSONObject();
                        try {
                            bundleObject.put("bundlePath", String.format("/cms/%s/%s%s.js", bundle.getSymbolicName(), bundle.getVersion().toString(), adminModule.value()));
                            bundleObject.put("SymbolicName", bundle.getSymbolicName());
                            bundleObject.put("Version", bundle.getVersion().toString());
                        } catch (JSONException e) {
                            //error
                        }
                        return bundleObject;
                    })
                    .collect(Collectors.toList());

            try {
                JSONObject result = new JSONObject();
                result.put("bundles", bundles);
                sendMessage.accept(result);

            } catch (JSONException ex){
                logRef.get().log(LogService.LOG_ERROR, "JSON Write Error", ex);
            }

        });
    }

    @Activate
    public void activate(ComponentContext ctx){
        LogService log = logRef.get();
        log.log(LogService.LOG_INFO, "CMS WebSocket Activated");
    }

    @Reference
    public void setLogService( LogService log ) {
        logRef.set(log);
    }


    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, unbind = "unbindWebSocketEndpoint", policy = ReferencePolicy.DYNAMIC)
    public void bindWebSocketEndpoint(media.dee.dcms.websocket.WebSocketEndpoint wsEndpoint){
        if( wsEndpoint instanceof WebSocketEndpoint)
            this.wsEndpoint.set((WebSocketEndpoint)wsEndpoint);
    }

    public void unbindWebSocketEndpoint(media.dee.dcms.websocket.WebSocketEndpoint wsEndpoint){
        if( wsEndpoint instanceof WebSocketEndpoint)
            this.wsEndpoint.compareAndSet((WebSocketEndpoint)wsEndpoint, null);
    }


    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, unbind = "unbindHttpService", policy = ReferencePolicy.DYNAMIC)
    public void bindHttpService( HttpService httpService ) {
        synchronized (httpServiceList) {
            httpServiceList.add(httpService);
        }

        guiComponents.parallelStream()
                .forEach( guiComponent -> registerModuleResources(httpService, guiComponent ));
    }

    public void unbindHttpService(HttpService httpService){
        synchronized (httpServiceList) {
            httpServiceList.remove(httpService);
        }
        guiComponents.parallelStream()
                .forEach( guiComponent -> unRegisterModuleResources(httpService, guiComponent ));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, unbind = "unbindEssentialComponent", policy = ReferencePolicy.DYNAMIC)
    public void bindEssentialComponent(GUIComponent component) {
        synchronized (guiComponents) {
            guiComponents.add(component);
            httpServiceList.parallelStream()
                    .forEach( httpService -> registerModuleResources(httpService, component));
            AdminModule adminModule = component.getClass().getAnnotation(AdminModule.class);
            if( adminModule.autoInstall() )
                wsEndpoint.get().sendAll(getInstallCommand(component));
        }
    }

    public void unbindEssentialComponent( GUIComponent component ) {
        synchronized (guiComponents) {
            guiComponents.remove(component);
            httpServiceList.parallelStream()
                    .forEach( httpService -> unRegisterModuleResources(httpService, component));
            wsEndpoint.get().sendAll(getUnInstallCommand(component));
        }
    }

    @Override
    public void newSession(Session session) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleEvent(Event event) {
        Consumer<JSONObject> sendMessage = (Consumer<JSONObject>) event.getProperty("sendMessage");
        JSONObject message = (JSONObject) event.getProperty("message");

        try {
            JSONArray cmdList = message.getJSONArray("parameters");
            for( int i = 0 ; i < cmdList.length(); ++i) {
                JSONObject cmdObject = cmdList.getJSONObject(i);
                String command = cmdObject.getString("command");
                if (commands.containsKey(command))
                    commands.get(command).accept(message, sendMessage);
            }
        } catch (JSONException e) {
            logRef.get().log(LogService.LOG_ERROR, "JSON READ Error", e);
        }
    }
}
