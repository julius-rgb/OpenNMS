/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.soa.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.opennms.core.soa.Filter;
import org.opennms.core.soa.Registration;
import org.opennms.core.soa.RegistrationListener;
import org.opennms.core.soa.ServiceRegistry;
import org.opennms.core.soa.filter.FilterParser;

/**
 * DefaultServiceRegistry
 *
 * @author brozow
 * @version $Id: $
 */
public class DefaultServiceRegistry implements ServiceRegistry {
    
    /**
     * AnyFilter
     *
     * @author brozow
     */
    public class AnyFilter implements Filter {

        public boolean match(Map<String, String> properties) {
            return true;
        }

    }

    /** Constant <code>INSTANCE</code> */
    public static final DefaultServiceRegistry INSTANCE = new DefaultServiceRegistry();
    
    private class ServiceRegistration implements Registration {

        private boolean m_unregistered = false;
        private Object m_provider;
        private Map<String, String> m_properties;
        private Class<?>[] m_serviceInterfaces;
        
        public ServiceRegistration(Object provider, Map<String, String> properties, Class<?>[] serviceInterfaces) {
            m_provider = provider;
            m_properties = properties;
            m_serviceInterfaces = serviceInterfaces;
        }
        

        public Map<String, String> getProperties() {
            return m_properties == null ? null : Collections.unmodifiableMap(m_properties);
        }

        public Class<?>[] getProvidedInterfaces() {
            return m_serviceInterfaces;
        }

        public <T> T getProvider(Class<T> serviceInterface) {

            if (serviceInterface == null) throw new NullPointerException("serviceInterface may not be null");

            for( Class<?> cl : m_serviceInterfaces ) {
                if ( serviceInterface.equals( cl ) ) {
                    return serviceInterface.cast( m_provider );
                }
            }
            
            throw new IllegalArgumentException("Provider not registered with interface " + serviceInterface);
        }

        public ServiceRegistry getRegistry() {
            return DefaultServiceRegistry.this;
        }

        public boolean isUnregistered() {
            return m_unregistered;
        }
        
        public void unregister() {
            m_unregistered = true;
            DefaultServiceRegistry.this.unregister(this);
            m_provider = null;
        }
        
    }
    
    private MultivaluedMap<Class<?>, ServiceRegistration> m_registrationMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    private MultivaluedMap<Class<?>, RegistrationListener<?>> m_listenerMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    
    /** {@inheritDoc} */
    public <T> T findProvider(Class<T> serviceInterface) {
        return findProvider(serviceInterface, null);
    }
    
    /** {@inheritDoc} */
    public <T> T findProvider(Class<T> serviceInterface, String filter) {
        Collection<T> providers = findProviders(serviceInterface, filter);
        for(T provider : providers) {
            return provider;
        }
        return null;
    }
    
    /** {@inheritDoc} */
    public <T> Collection<T> findProviders(Class<T> serviceInterface) {
        return findProviders(serviceInterface, null);
    }

    /** {@inheritDoc} */
    public <T> Collection<T> findProviders(Class<T> serviceInterface, String filter) {
        
        Filter f = filter == null ? new AnyFilter() : new FilterParser().parse(filter);

        Set<ServiceRegistration> registrations = getRegistrations(serviceInterface);
        Set<T> providers = new LinkedHashSet<T>(registrations.size());
        for(ServiceRegistration registration : registrations) {
            if (f.match(registration.getProperties())) {
                providers.add(registration.getProvider(serviceInterface));
            }
        }
        return providers;
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    public Registration register(Object serviceProvider, Class<?>... services) {
        return register(serviceProvider, (Map<String, String>)null, services);
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param properties a {@link java.util.Map} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    public Registration register(Object serviceProvider, Map<String, String> properties, Class<?>... services) {
        
        ServiceRegistration registration = new ServiceRegistration(serviceProvider, properties, services);
        
        for(Class<?> serviceInterface : services) {
            m_registrationMap.add(serviceInterface, registration);
        }
        
        for(Class<?> serviceInterface : services) {
            fireProviderRegistered(serviceInterface, registration);
        }

        
        return registration;

    }
    
    private <T> Set<ServiceRegistration> getRegistrations(Class<T> serviceInterface) {
        Set<ServiceRegistration> copy = m_registrationMap.getCopy(serviceInterface);
        return (copy == null ? Collections.<ServiceRegistration>emptySet() : copy);
    }

    private void unregister(ServiceRegistration registration) {
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            m_registrationMap.remove(serviceInterface, registration);
        }
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            fireProviderUnregistered(serviceInterface, registration);
        }

    }

    /** {@inheritDoc} */
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener) {
        m_listenerMap.add(service, listener);
    }

    /** {@inheritDoc} */
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener, boolean notifyForExistingProviders) {

        if (notifyForExistingProviders) {
            
            Set<ServiceRegistration> registrations = null;
            
            synchronized (m_registrationMap) {
                m_listenerMap.add(service, listener);
                registrations = getRegistrations(service);
            }
            
            for(ServiceRegistration registration : registrations) {
                listener.providerRegistered(registration, registration.getProvider(service));
            }
            
        } else {
            
            m_listenerMap.add(service, listener);
            
        }
    }

    /** {@inheritDoc} */
    public <T> void removeListener(Class<T> service, RegistrationListener<T> listener) {
        m_listenerMap.remove(service, listener);
    }
    
    private <T> void fireProviderRegistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerRegistered(registration, registration.getProvider(serviceInterface));
        }
    }
    
    private <T> void fireProviderUnregistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerUnregistered(registration, registration.getProvider(serviceInterface));
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private <T> Set<RegistrationListener<T>> getListeners(Class<T> serviceInterface) {
        Set<RegistrationListener<?>> listeners = m_listenerMap.getCopy(serviceInterface);
        return (Set<RegistrationListener<T>>) (listeners == null ? Collections.emptySet() : listeners);
    }

}
