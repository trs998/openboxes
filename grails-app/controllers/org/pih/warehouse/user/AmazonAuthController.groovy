/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.user

import com.nimbusds.jose.JWSObject
import grails.converters.JSON
import org.pih.warehouse.core.MailService
import org.pih.warehouse.core.User

class AmazonAuthController {

    MailService mailService
    def userService
    def authService
    def grailsApplication
    def recaptchaService
    def ravenClient
    def amazonAuthService
    def googleAuthService
    def identifierService

    static allowedMethods = [login: "GET", doLogin: "POST", logout: "GET"]

    def config = {
        render ([data:amazonAuthService.config] as JSON)
    }

    def callback = {
        log.info "callback: " + params
        if (params.code) {
            def token = amazonAuthService.getToken(params.code)
            JWSObject jwsObject = JWSObject.parse(token.id_token)
            User user = azureAuthService.findOrCreateUser(jwsObject)
            log.info "USER " + user?.id
            session.user = user
            session.oauthProvider = "azure"
            redirect(controller: "auth", action: "login")
            return
        }
        render "callback successful"
    }

    def login = {
        redirect(url: amazonAuthService.authenticationUrl)
    }

    def logout = {
        log.info "logout " + params
        redirect(url: amazonAuthService.logoutUrl)
    }
}