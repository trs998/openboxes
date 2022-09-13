package org.pih.warehouse.jobs

import grails.util.Holders
import org.apache.commons.lang.WordUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import util.LiquibaseUtil

class JobUtils {

    private static final transient Logger log = LoggerFactory.getLogger(JobUtils)

    private static String getKey(Class clazz) {
        return WordUtils.uncapitalize(clazz.name)
    }

    /** Return true if the given job class can be run at the current time. */
    static boolean shouldExecute(Class clazz) {
        boolean enabled = Boolean.valueOf(Holders.config.getProperty("openboxes.jobs.${getKey(clazz)}.enabled"))
        if (!enabled) {
            return false
        }
        if (LiquibaseUtil.isRunningMigrations()) {
            log.info "Postponing job execution for ${getKey(clazz)} until liquibase migrations are complete"
            return false
        }
        return true
    }

    private static String getCronName(Class clazz) {
        return "${getKey(clazz)}CronTrigger"
    }

    private static String getCronExpression(Class clazz) {
        return Holders.config.getProperty("openboxes.jobs.${getKey(clazz)}.cronExpression")
    }

    /** Create a triggers block for the given job class. */
    static getTriggers(Class clazz) {
        return {
            cron name: getCronName(clazz), cronExpression: getCronExpression(clazz)
        }
    }
}
