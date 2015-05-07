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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Value;
import soot.jimple.StaticFieldRef;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.options.Options;
import edu.psu.cse.siis.coal.Analysis;
import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.FatalAnalysisException;
import edu.psu.cse.siis.coal.PropagationSceneTransformer;
import edu.psu.cse.siis.coal.PropagationSceneTransformerFilePrinter;
import edu.psu.cse.siis.coal.SymbolFilter;
import edu.psu.cse.siis.coal.arguments.ArgumentValueManager;
import edu.psu.cse.siis.coal.field.transformers.FieldTransformerManager;
import edu.psu.cse.siis.ic3.db.SQLConnection;
import edu.psu.cse.siis.ic3.manifest.ManifestPullParser;

public class Ic3Analysis extends Analysis<Ic3CommandLineArguments> {
  private static final String INTENT = "android.content.Intent";
  private static final String INTENT_FILTER = "android.content.IntentFilter";
  private static final String BUNDLE = "android.os.Bundle";
  private static final String COMPONENT_NAME = "android.content.ComponentName";
  private static final String ACTIVITY = "android.app.Activity";

  private static final String[] frameworkClassesArray = { INTENT, INTENT_FILTER, BUNDLE,
      COMPONENT_NAME, ACTIVITY };
  protected static final List<String> frameworkClasses = Arrays.asList(frameworkClassesArray);

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Ic3Data.Application.Builder ic3Builder;
  private Map<String, Ic3Data.Application.Component.Builder> componentNameToBuilderMap;

  protected String outputDir;
  protected Writer writer;
  protected ProcessManifest manifest;
  protected Map<String, Integer> componentToIdMap;
  protected SetupApplication setupApplication;

  @Override
  protected void registerFieldTransformerFactories(Ic3CommandLineArguments commandLineArguments) {
    Timers.v().totalTimer.start();
    FieldTransformerManager.v().registerDefaultFieldTransformerFactories();
  }

  @Override
  protected void registerArgumentValueAnalyses(Ic3CommandLineArguments commandLineArguments) {
    ArgumentValueManager.v().registerDefaultArgumentValueAnalyses();
    ArgumentValueManager.v().registerArgumentValueAnalysis("classType",
        new ClassTypeValueAnalysis());
    ArgumentValueManager.v().registerArgumentValueAnalysis("authority",
        new AuthorityValueAnalysis());
    ArgumentValueManager.v().registerArgumentValueAnalysis("path", new PathValueAnalysis());
  }

  @Override
  protected void registerMethodReturnValueAnalyses(Ic3CommandLineArguments commandLineArguments) {
  }

  @Override
  protected void initializeAnalysis(Ic3CommandLineArguments commandLineArguments)
      throws FatalAnalysisException {
    long startTime = System.currentTimeMillis() / 1000;
    outputDir = commandLineArguments.getOutput();

    prepareManifestFile(commandLineArguments);

    if (commandLineArguments.getProtobufDestination() != null) {
      ic3Builder = Ic3Data.Application.newBuilder();
      ic3Builder.setAnalysisStart(startTime);
      if (commandLineArguments.getSample() != null) {
        ic3Builder.setSample(commandLineArguments.getSample());
      }
      componentNameToBuilderMap = ((ManifestPullParser) manifest).populateProtobuf(ic3Builder);
    } else if (commandLineArguments.getDb() != null) {
      SQLConnection.init(commandLineArguments.getDb(), commandLineArguments.getSsh(),
          commandLineArguments.getDbLocalPort());
      componentToIdMap = ((ManifestPullParser) manifest).writeToDb(false);
    }

    Timers.v().mainGeneration.start();
    setupApplication =
        new SetupApplication(commandLineArguments.getManifest(), commandLineArguments.getInput(),
            commandLineArguments.getClasspath());

    Map<String, Set<String>> callBackMethods;

    try {
      callBackMethods =
          setupApplication.calculateSourcesSinksEntrypoints(new HashSet<AndroidMethod>(),
              new HashSet<AndroidMethod>(), manifest);
    } catch (IOException e) {
      logger.error("Could not calculate entry points", e);
      throw new FatalAnalysisException();
    }
    Timers.v().mainGeneration.end();

    Timers.v().misc.start();

    if (manifest == null) {
      manifest = setupApplication.getManifest();
    }

    // Application package name is now known.
    String appName = manifest.getPackageName();
    ArgumentValueManager.v().registerArgumentValueAnalysis("context",
        new ContextValueAnalysis(appName));
    AndroidMethodReturnValueAnalyses.registerAndroidMethodReturnValueAnalyses(appName);

    if (outputDir != null && appName != null) {
      String outputFile = String.format("%s/%s.csv", outputDir, appName);

      try {
        writer = new BufferedWriter(new FileWriter(outputFile, false));
      } catch (IOException e1) {
        logger.error("Could not open file " + outputFile, e1);
      }
    }

    // reset Soot:
    soot.G.reset();

    Map<SootMethod, Set<String>> entryPointMap =
        commandLineArguments.computeComponents() ? new HashMap<SootMethod, Set<String>>() : null;
    addSceneTransformer(entryPointMap);

    if (commandLineArguments.computeComponents()) {
      addEntryPointMappingSceneTransformer(manifest, callBackMethods, entryPointMap);
    }

    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_whole_program(true);
    Options.v().set_soot_classpath(
        commandLineArguments.getInput() + File.pathSeparator + commandLineArguments.getClasspath());
    Options.v().set_ignore_resolution_errors(true);
    Options.v().set_process_dir(frameworkClasses);

    Options.v().setPhaseOption("cg.spark", "on");

    // do not merge variables (causes problems with PointsToSets)
    Options.v().setPhaseOption("jb.ulp", "off");

    Options.v().setPhaseOption("jb.uce", "remove-unreachable-traps:true");

    Options.v().setPhaseOption("cg", "trim-clinit:false");
    Options.v().set_prepend_classpath(true);

    if (AnalysisParameters.v().useShimple()) {
      Options.v().set_via_shimple(true);
      Options.v().set_whole_shimple(true);
    }

    Options.v().set_src_prec(Options.src_prec_java);
    Timers.v().misc.end();

    Timers.v().classLoading.start();
    for (String frameworkClass : frameworkClasses) {
      SootClass c = Scene.v().loadClassAndSupport(frameworkClass);
      Scene.v().forceResolve(frameworkClass, SootClass.BODIES);
      c.setApplicationClass();
    }

    Scene.v().loadNecessaryClasses();
    Timers.v().classLoading.end();

    Timers.v().entryPointMapping.start();
    Scene.v().setEntryPoints(
        Collections.singletonList(setupApplication.getEntryPointCreator().createDummyMain(
            new ArrayList<String>())));
    Timers.v().entryPointMapping.end();
  }

  protected void prepareManifestFile(Ic3CommandLineArguments commandLineArguments) {
    if (commandLineArguments.getDb() != null
        || commandLineArguments.getProtobufDestination() != null) {
      manifest = new ManifestPullParser();
      manifest.loadManifestFile(commandLineArguments.getManifest());
    }
  }

  @Override
  protected void setApplicationClasses(Ic3CommandLineArguments commandLineArguments)
      throws FatalAnalysisException {
    AnalysisParameters.v().addAnalysisClasses(
        computeAnalysisClasses(commandLineArguments.getInput()));
    AnalysisParameters.v().addAnalysisClasses(frameworkClasses);
  }

  @Override
  protected void handleFatalAnalysisException(Ic3CommandLineArguments commandLineArguments,
      FatalAnalysisException exception) {
    String appName = manifest != null ? manifest.getPackageName() : "";
    logger.error("Could not process application " + appName, exception);

    if (outputDir != null && !appName.equals("")) {
      try {
        if (writer == null) {
          String outputFile = String.format("%s/%s.csv", outputDir, appName);

          writer = new BufferedWriter(new FileWriter(outputFile, false));
        }

        writer.write(commandLineArguments.getInput() + " -1\n");
        writer.close();
      } catch (IOException e1) {
        logger.error("Could not write to file after failure to process application", e1);
      }
    }
  }

  @Override
  protected void processResults(Ic3CommandLineArguments commandLineArguments)
      throws FatalAnalysisException {
    if (commandLineArguments.getProtobufDestination() != null) {
      ProtobufResultProcessor resultProcessor = new ProtobufResultProcessor();
      try {
        resultProcessor.processResult(manifest.getPackageName(), ic3Builder,
            commandLineArguments.getProtobufDestination(), commandLineArguments.binary(),
            componentNameToBuilderMap, AnalysisParameters.v().getAnalysisClasses().size(), writer);
      } catch (IOException e) {
        logger.error("Could not process analysis results", e);
        throw new FatalAnalysisException();
      }
    } else {
      ResultProcessor resultProcessor = new ResultProcessor();
      try {
        resultProcessor.processResult(commandLineArguments.getDb() != null,
            manifest.getPackageName(), componentToIdMap, AnalysisParameters.v()
                .getAnalysisClasses().size(), writer);
      } catch (IOException | SQLException e) {
        logger.error("Could not process analysis results", e);
        throw new FatalAnalysisException();
      }
    }
  }

  @Override
  protected void finalizeAnalysis(Ic3CommandLineArguments commandLineArguments) {
  }

  protected void addSceneTransformer(Map<SootMethod, Set<String>> entryPointMap) {
    Ic3ResultBuilder resultBuilder = new Ic3ResultBuilder();
    resultBuilder.setEntryPointMap(entryPointMap);
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    String debugDirPath = System.getProperty("user.home") + File.separator + "debug";
    File debugDir = new File(debugDirPath);
    if (!debugDir.exists()) {
      debugDir.mkdir();
    }

    String fileName = dateFormat.format(new Date()) + ".txt";
    String debugFilename = debugDirPath + File.separator + fileName;

    String pack = AnalysisParameters.v().useShimple() ? "wstp" : "wjtp";
    Transform transform =
        new Transform(pack + ".ifds", new PropagationSceneTransformer(resultBuilder,
            new PropagationSceneTransformerFilePrinter(debugFilename, new SymbolFilter() {

              @Override
              public boolean filterOut(Value symbol) {
                return symbol instanceof StaticFieldRef
                    && ((StaticFieldRef) symbol).getField().getDeclaringClass().getName()
                        .startsWith("android.provider");
              }
            })));
    if (PackManager.v().getPack(pack).get(pack + ".ifds") == null) {
      PackManager.v().getPack(pack).add(transform);
    } else {
      Iterator<?> it = PackManager.v().getPack(pack).iterator();
      while (it.hasNext()) {
        Object current = it.next();
        if (current instanceof Transform
            && ((Transform) current).getPhaseName().equals(pack + ".ifds")) {
          it.remove();
          break;
        }

      }
      PackManager.v().getPack(pack).add(transform);
    }
  }

  protected void addEntryPointMappingSceneTransformer(ProcessManifest manifest,
      Map<String, Set<String>> entryPointMapping, Map<SootMethod, Set<String>> entryPointMap) {
    String pack = AnalysisParameters.v().useShimple() ? "wstp" : "wjtp";

    Transform transform =
        new Transform(pack + ".epm", new EntryPointMappingSceneTransformer(manifest,
            entryPointMapping, entryPointMap));
    if (PackManager.v().getPack(pack).get(pack + ".epm") == null) {
      PackManager.v().getPack(pack).add(transform);
    } else {
      Iterator<?> it = PackManager.v().getPack(pack).iterator();
      while (it.hasNext()) {
        Object current = it.next();
        if (current instanceof Transform
            && ((Transform) current).getPhaseName().equals(pack + ".epm")) {
          it.remove();
          break;
        }

      }
      PackManager.v().getPack(pack).add(transform);
    }
  }
}
