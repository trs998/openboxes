/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.core

import grails.core.GrailsApplication
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.grails.core.io.ResourceLocator
import org.pih.warehouse.LocalizationUtil
import org.springframework.web.context.request.RequestContextHolder

class LocalizationService {

    // inject the grails application so we can access the default locale
    GrailsApplication grailsApplication

    ResourceLocator grailsResourceLocator

    String formatMetadata(Object object) {
        def format = grailsApplication.mainContext.getBean('org.pih.warehouse.FormatTagLib')
        return format.metadata(obj: object)
    }

    String formatDate(Date date) {
        def format = grailsApplication.mainContext.getBean('org.pih.warehouse.FormatTagLib')
        return format.date(obj: date)
    }

    /**
     * Localizes the passed string value based on the current locale
     */
    String getLocalizedString(String value) {

        // null check
        if (!value) {
            return value
        }

        return LocalizationUtil.getLocalizedString(value, getCurrentLocale())
    }
    /**
     * Get a locale based on the given language code. Returns default if no language code is specified.
     *
     * @param languageCode
     * @return
     */
    Locale getLocale(String languageCode) {
        return languageCode ? new Locale(languageCode) : currentLocale
    }

    /**
     * Gets the current locale or return default locale.
     */
    Locale getCurrentLocale() {
        // fetch the locale of the current user; if there isn't one, use the default locale
        return (RequestContextHolder?.requestAttributes?.session?.user?.locale ?:
                new Locale(grailsApplication.config.openboxes.locale.defaultLocale ?: "en"))
    }

    /**
     * Get all messages properties.
     *
     * @return
     */
    Properties getMessagesProperties(Locale locale) {
        Properties messagesProperties = new Properties()
        def messagesPropertiesFilename = (locale && locale.language != "en" && locale.language != 'null') ? "messages_${locale.language}.properties" : "messages.properties"

        // Get properties from classpath
        if (!Metadata.getCurrent().isWarDeployed()) {
            String messagesPropertiesUrl = "grails-app/i18n/" + messagesPropertiesFilename
            messagesProperties = getMessagesPropertiesFromClasspath(messagesPropertiesUrl)
        }
        // Get properties from exploded WAR file
        else {
            String messagesPropertiesUrl = "/WEB-INF/grails-app/i18n/" + messagesPropertiesFilename
            messagesProperties = getMessagesPropertiesFromResource(messagesPropertiesUrl)
        }
        return messagesProperties.sort()
    }

    Properties getMessagesPropertiesWithPrefix(String prefix, Locale locale) {
        return getMessagesProperties(locale).findAll {
            it.key.startsWith(prefix)
        }
    }


        def resource = grailsResourceLocator.findResourceForURI('classpath:' + messagesPropertiesFilename)
        messagesProperties.load(resource.inputStream)

        return messagesProperties
        File messagesPropertiesFile = new ClassPathResource(messagesPropertiesUrl)?.getFile()
        if (messagesPropertiesFile.exists()) {
            InputStream messagesPropertiesStream = new FileInputStream(messagesPropertiesFile)
            properties.load(messagesPropertiesStream)
            messagesPropertiesStream.close()
        }
        return properties
    }
}
