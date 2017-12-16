import {BundleContext} from "./BundleContext";
import {isFunction} from "./utils";


window.addEventListener('ws:system.info', (event)=>{
    let command = event.detail;
    console.log(`%cSystem: ${command.SymbolicName} Version:${command.Version}`, 'color: red; font-size: 1.5em;');
});

const lb = (filter)=>{
    console.log(`%cSymbolicName\tVersion`, 'background: gray; color: white');
    Object.keys(BundleContext.Bundles).forEach( key => {
        let entry = BundleContext.Bundles[key];
        console.log(`${entry.bundleContext.props.SymbolicName}\t${entry.bundleContext.props.Version}`);
    });
};

const services = (filter)=>{
    Object.keys(BundleContext.ServiceReferences).forEach( key => {
        let serviceReferences = bundleContext.getServiceReferences(key, filter);
        if( !serviceReferences.length )
            return;
        console.log(`%c${key}`, 'background: gray; color: white');
        serviceReferences.forEach(serviceReference =>{
            let serviceType = isFunction(serviceReference.instance) ? 'Factory' : 'Singleton';
            let props = '';
            Object.keys(serviceReference.props).forEach( (key)=> {
                props+= `\n\t\t${key}: ${serviceReference.props[key]}`;
            });
            console.log(`\t${serviceType}
\t${serviceReference.context.props.SymbolicName}-${serviceReference.context.props.Version}
\tusage:${serviceReference.usage}\n\tprops:${props}`
            );
        });
    });
};



window.shell ={
    lb: lb,
    services: services
};