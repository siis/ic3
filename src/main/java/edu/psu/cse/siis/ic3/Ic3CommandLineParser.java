/*
 * Copyright (C) 2015 The Pennsylvania State University and the University of Wisconsin
 * Systems and Internet Infrastructure Security Laboratory
 *
 * Author: Damien Octeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.psu.cse.siis.ic3;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.psu.cse.siis.coal.CommandLineParser;

/**
 * Command line parser for IC3.
 */
public class Ic3CommandLineParser extends CommandLineParser<Ic3CommandLineArguments> {
  private static final String COPYRIGHT =
      "Copyright (C) 2015 The Pennsylvania State University and the University of Wisconsin\n"
          + "Systems and Internet Infrastructure Security Laboratory\n";

  @SuppressWarnings("static-access")
  @Override
  protected void parseAnalysisSpecificArguments(Options options) {
    options.addOption(OptionBuilder
        .withDescription("Path to the manifest file or the .apk of the application.").hasArg()
        .withArgName(".apk or manifest").isRequired().create("apkormanifest"));
    options.addOption(OptionBuilder.withDescription("The application name.").hasArg()
        .withArgName("app name").create("appname"));
    options.addOption(OptionBuilder.withDescription("Store entry points to database.")
        .hasOptionalArg().withArgName("DB properties file").create("db"));
    options.addOption(OptionBuilder.withDescription("Use SSH to connect to the database.")
        .hasOptionalArg().withArgName("SSH properties file").create("ssh"));
    options.addOption(OptionBuilder.withDescription("Local DB port to connect to.").hasArg()
        .withType(Number.class).withArgName("local DB port").create("localport"));
    options.addOption("computecomponents", false,
        "Compute which components each exit point belongs to.");
  }

  @Override
  protected void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    System.out.println(COPYRIGHT);
    formatter.printHelp("ic3 -input <Android directory> -classpath <classpath> "
        + "-apk <path to application .apk> [-appName <app name>] [-computecomponents] "
        + "[-db <path to DB properties file>] [-ssh <path to SSH properties file>] "
        + "[-localport <DB local port>] [-modeledtypesonly] [-output <output directory>]", options);
  }
}
