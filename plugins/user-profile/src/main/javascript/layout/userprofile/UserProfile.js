import {ProfileMenuItem} from './ProfileMenuItem';
import {ProfileProgressItem} from './dashboard/ProfileProgressItem';
import {ProfilePieItem} from './dashboard/ProfilePieItem';

defineModule(['react'], (React)=> {

    let serviceRegistry = [];

    return {
        activator: {
            start: (context)=>{
                console.info('User-Profile Components Activated');

                serviceRegistry.push(
                    context.registerService('d.cms.ui.component.NavigationMenuItem', (context, props)=>{
                        return React.createElement(ProfileMenuItem, props , null);
                    }, {id: 'profile-menu-item'})
                );

                serviceRegistry.push(
                    context.registerService('d.cms.ui.component.Dashboard.Card', (context, props)=>{
                        return ProfilePieItem;
                    }, {id: 'profile-stats'})
                );

                serviceRegistry.push(
                    context.registerService('d.cms.ui.component.Dashboard.Card', (context, props)=>{
                        return ProfileProgressItem;
                    }, {id: 'profile-engagement'})
                );


            },
            stop: (context)=>{
                console.info('User-Profile Components Deactivated');
                serviceRegistry.forEach( r => r.unregister() );
            }
        },
        exports:{}
    };

});