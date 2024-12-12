package org.phoebus.applications.uxanalytics.monitor.backend.database.authentication;

import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

public class Neo4JAuthenticationProvider implements ServiceAuthenticationProvider {


    @Override
    public void authenticate(String username, String password) {

    }

    @Override
    public void logout(String token) {

    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return AuthenticationScope.NEO4J;
    }
}