package de.mmerlin.mmxp.whatabubble.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("WhatABubble");

    private ModLogger() { }

    public static void info(String msg, Object... args) { LOGGER.info(msg, args); }

    public static void warn(String msg, Object... args) { LOGGER.warn(msg, args); }

    public static void error(String msg, Throwable t) { LOGGER.error(msg, t); }

    public static void debug(String msg, Object... args) { LOGGER.debug(msg, args); }
}

