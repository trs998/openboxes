/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package util

import grails.util.Holders
import org.springframework.boot.info.GitProperties

// See http://jira.codehaus.org/browse/GRAILS-6515
class ConfigHelper {

    /**
     * Return the version of the application, as specified in build.gradle.
     */
    static String getAppVersion() {
        // build.app.version is readable by war files, app.version by local builds
        return Holders.grailsApplication.metadata.getProperty('info.app.version')
//        return Holders.grailsApplication.metadata.getProperty(
//            'build.app.version',
//            String,
//            Holders.grailsApplication.config.getProperty('app.version')
//        )
    }

    static String getBranchName(GitProperties gitProperties) {
        // build.git.branch is set by bamboo; gitProperties.branch from git
        return Holders.grailsApplication.metadata.getProperty(
            'build.git.branch',
            String,
            gitProperties.branch
        )
    }

    static getContextPath() {
        String contextPath = Holders.grailsApplication.config.server.contextPath
        return (contextPath != '/') ? contextPath : ''
    }

    static booleanValue(def value) {
        if (value.class == java.lang.Boolean) {
            // because 'true.toBoolean() == false' !!!
            return value
        } else {
            return value.toBoolean()
        }
    }

    static listValue(def value) {
        if (value instanceof java.lang.String) {
            return value?.split(",")
        } else {
            return value
        }

    }

}
