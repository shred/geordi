/*
 * geordi
 *
 * Copyright (C) 2018 Richard "Shred" Körber
 *   https://github.com/shred/geordi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.shredzone.geordi;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Geordi's main class.
 */
public class Geordi {

    /**
     * Runs Geordi.
     *
     * @param args
     *            Command line parameters
     */
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("d", "database", true, "database URL");
        options.addOption("u", "user", true, "database user");
        options.addOption("p", "password", true, "database password");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            GeordiModule module = new GeordiModule();
            module.setDatabaseHost(getDatabaseHost(cmd));
            module.setDatabaseUser(getDatabaseUser(cmd));
            module.setDatabasePassword(getDatabasePassword(cmd));

            Injector injector = Guice.createInjector(module);
            GeordiRunner runner = injector.getInstance(GeordiRunner.class);
            runner.start();
        } catch (ParseException ex) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("geordi", options, true);
            System.exit(1);
        }
    }

    private static String getDatabaseHost(CommandLine cmd) {
        String database = "jdbc:postgresql://localhost/geordi";

        if (cmd.hasOption("database")) {
            database = cmd.getOptionValue("database");
        } else {
            String env = System.getenv("GEORDI_DATABASE");
            if (env != null) {
                database = env.trim();
            }
        }

        if (!database.contains("://")) {
            database = "jdbc:postgresql://" + database;
        }

        if (!database.startsWith("jdbc:")) {
            database = "jdbc:" + database;
        }

        return database;
    }

    private static String getDatabaseUser(CommandLine cmd) {
        if (cmd.hasOption("user")) {
            return cmd.getOptionValue("user");
        } else {
            String env = System.getenv("GEORDI_USER");
            if (env != null) {
                return env.trim();
            }
        }

        return null;
    }

    private static String getDatabasePassword(CommandLine cmd) {
        if (cmd.hasOption("password")) {
            return cmd.getOptionValue("password");
        } else {
            String env = System.getenv("GEORDI_PASSWORD");
            if (env != null) {
                return env;
            }
        }

        return null;
    }

}
