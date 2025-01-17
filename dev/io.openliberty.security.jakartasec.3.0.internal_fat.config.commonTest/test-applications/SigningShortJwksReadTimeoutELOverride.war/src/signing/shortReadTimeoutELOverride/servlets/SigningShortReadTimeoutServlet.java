/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package signing.shortReadTimeoutELOverride.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/SigningShortReadTimeout")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerURI}",
                                         clientId = "${openIdConfig.clientId}", clientSecret = "${openIdConfig.clientSecret}", redirectURI = "${openIdConfig.redirectURI}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         jwksReadTimeout = Constants.DEFAULT_JWKS_READ_TIMEOUT,
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         providerMetadata = @OpenIdProviderMetadata(idTokenSigningAlgorithmsSupported = "${openIdConfig.idTokenSigningAlgorithmsSupported}",
                                                                                    jwksURI = "${openIdConfig.jwksURI}"))

@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class SigningShortReadTimeoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
